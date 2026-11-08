package com.order.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.member.domain.dto.TransactionDto;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** A terminal result and its money movement commit together, under a database unique key. */
@Service
public class AdminTransactionRequestService {
    private static final Logger log = LoggerFactory.getLogger(AdminTransactionRequestService.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ITransactionService money;
    private final ObjectMapper json = new ObjectMapper();

    public record Result(String requestId, String state, String message) {}
    private record Saved(String hash, Result result) {}
    private static class ExistingRequest extends RuntimeException {}

    public AdminTransactionRequestService(@Qualifier("dynamicDataSource") DataSource source,
            PlatformTransactionManager manager, ITransactionService money) {
        this.jdbc = new JdbcTemplate(source);
        this.money = money;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Result submit(Long operator, TransactionDto dto) {
        validateKey(dto == null ? null : dto.getRequestId());
        if (operator == null || dto.getUserId() == null) {
            throw new IllegalArgumentException("请检查用户和操作员");
        }
        String hash = fingerprint(dto);
        Saved saved = find(operator, dto.getRequestId());
        if (saved != null) return replay(saved, hash);
        try {
            return transaction.execute(status -> {
                claim(operator, dto, hash);
                // Persist a terminal rejection too: the browser has already saved this request.
                // Claim first so a changed invalid payload cannot overwrite an existing result.
                validateMoney(dto);
                if (!money.processTransaction(dto)) throw new IllegalStateException("交易未完成");
                return finish(operator, dto.getRequestId(), "SUCCEEDED", "操作成功");
            });
        } catch (ExistingRequest duplicate) {
            return replayRequired(operator, dto.getRequestId(), hash);
        } catch (RuntimeException failure) {
            // The money transaction has rolled back before a terminal failure is recorded.
            // A concurrent retry may win the unique key; in that case return its committed result.
            log.warn("Administrator transaction rolled back: operator={} request={} user={}",
                    operator, dto.getRequestId(), dto.getUserId(), failure);
            try {
                return transaction.execute(status -> {
                    claim(operator, dto, hash);
                    return finish(operator, dto.getRequestId(), "FAILED",
                            "本次操作未入账，请核对金额及余额后重新提交；若再次失败请联系管理员");
                });
            } catch (ExistingRequest duplicate) {
                return replayRequired(operator, dto.getRequestId(), hash);
            }
        }
    }

    public Result status(Long operator, String requestId) {
        validateKey(requestId);
        Saved saved = find(operator, requestId);
        // Absence is NOT proof of failure: an uncommitted request is deliberately invisible.
        return saved == null ? new Result(requestId, "UNKNOWN", "结果尚未确认，请勿发起新交易") : saved.result();
    }

    private void claim(Long operator, TransactionDto dto, String hash) {
        try {
            jdbc.update("""
                INSERT INTO admin_transaction_request
                    (operator_id, request_id, payload_hash, user_id, state)
                VALUES (?, ?, ?, ?, 'PROCESSING')
                """, operator, dto.getRequestId(), hash, dto.getUserId());
        } catch (DuplicateKeyException duplicate) {
            throw new ExistingRequest();
        }
    }

    private Result finish(Long operator, String id, String state, String message) {
        if (jdbc.update("""
                UPDATE admin_transaction_request SET state=?, message=?, completed_at=NOW(3)
                WHERE operator_id=? AND request_id=?
                """, state, message, operator, id) != 1) {
            throw new IllegalStateException("Unable to save transaction result");
        }
        return new Result(id, state, message);
    }

    private Saved find(Long operator, String id) {
        List<Saved> rows = jdbc.query("""
            SELECT payload_hash, state, message FROM admin_transaction_request
            WHERE operator_id=? AND request_id=?
            """, (rs, n) -> new Saved(rs.getString(1), new Result(id, rs.getString(2), rs.getString(3))), operator, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Result replayRequired(Long operator, String id, String hash) {
        Saved saved = find(operator, id);
        if (saved == null) throw new IllegalStateException("交易结果尚未确认，请使用原请求查询或重试");
        return replay(saved, hash);
    }

    private Result replay(Saved saved, String hash) {
        if (!saved.hash().equals(hash)) throw new IllegalArgumentException("同一请求编号不能用于不同交易");
        return saved.result();
    }

    private static void validateMoney(TransactionDto dto) {
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0
                || dto.getOperationType() == null
                || (dto.getOperationType() != 0 && dto.getOperationType() != 1)
                || (dto.getGiftAmount() != null && dto.getGiftAmount().signum() < 0)) {
            throw new IllegalArgumentException("请检查操作类型和金额");
        }
    }

    private static void validateKey(String key) {
        if (key == null || !key.matches("[a-zA-Z0-9_-]{16,64}")) {
            throw new IllegalArgumentException("缺少有效交易请求编号，请刷新管理页面后重试");
        }
    }

    private String fingerprint(TransactionDto dto) {
        try {
            byte[] bytes = json.writeValueAsBytes(Arrays.asList(dto.getUserId(), dto.getOperationType(),
                    dto.getTransactionType(), number(dto.getAmount()), number(dto.getGiftAmount()),
                    dto.getGiftType(), number(dto.getGiftRatio()), dto.getRemark()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to identify transaction", e);
        }
    }

    private static String number(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
