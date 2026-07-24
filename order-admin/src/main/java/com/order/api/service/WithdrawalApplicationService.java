package com.order.api.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.order.api.controller.dto.AccountApiDtos.DepositResponse;
import com.order.api.controller.dto.AccountApiDtos.SensitiveWithdrawalAccountResponse;
import com.order.api.controller.dto.AccountApiDtos.TransactionResponse;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalHistoryResponse;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalRequest;
import com.order.api.controller.dto.AccountApiDtos.WithdrawalResponse;
import com.order.common.core.page.TableDataInfo;
import com.order.common.utils.DateUtils;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.domain.GoodsTransactionFlow;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.domain.OrderUser;
import com.order.member.domain.OrderWithdrawal;
import com.order.member.domain.WithdrawalDailyUsage;
import com.order.member.mapper.GoodsRechargeRecordMapper;
import com.order.member.mapper.GoodsTransactionFlowMapper;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.mapper.OrderWithdrawalMapper;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.ITransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.order.api.service.AccountErrorCodes.AMOUNT_ABOVE_MAXIMUM;
import static com.order.api.service.AccountErrorCodes.AMOUNT_BELOW_MINIMUM;
import static com.order.api.service.AccountErrorCodes.BALANCE;
import static com.order.api.service.AccountErrorCodes.CREDIT_SCORE;
import static com.order.api.service.AccountErrorCodes.DAILY_WITHDRAWAL_AMOUNT;
import static com.order.api.service.AccountErrorCodes.DAILY_WITHDRAWAL_COUNT;
import static com.order.api.service.AccountErrorCodes.IDEMPOTENCY_CONFLICT;
import static com.order.api.service.AccountErrorCodes.INVALID_REQUEST;
import static com.order.api.service.AccountErrorCodes.LEVEL_MINIMUM_BALANCE;
import static com.order.api.service.AccountErrorCodes.MEMBER_LEVEL;
import static com.order.api.service.AccountErrorCodes.OUTSIDE_WITHDRAWAL_WINDOW;
import static com.order.api.service.AccountErrorCodes.PENDING_WITHDRAWAL;
import static com.order.api.service.AccountErrorCodes.PLATFORM_DAILY_LIMIT;
import static com.order.api.service.AccountErrorCodes.REVIEW_CONFLICT;
import static com.order.api.service.AccountErrorCodes.TASK_REQUIREMENT;
import static com.order.api.service.AccountErrorCodes.TRADE_PASSWORD;
import static com.order.api.service.AccountErrorCodes.USER_NOT_FOUND;
import static com.order.api.service.AccountErrorCodes.WITHDRAWAL_ACCOUNT;
import static com.order.api.service.AccountErrorCodes.WITHDRAWAL_DISABLED;
import static com.order.api.service.AccountErrorCodes.WITHDRAWAL_NOT_FOUND;

@Service
public class WithdrawalApplicationService {
    private static final Logger log = LoggerFactory.getLogger(WithdrawalApplicationService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderUserMapper userMapper;
    private final OrderWithdrawalMapper withdrawalMapper;
    private final GoodsWithdrawalAccountMapper accountMapper;
    private final GoodsRechargeRecordMapper rechargeMapper;
    private final GoodsTransactionFlowMapper flowMapper;
    private final TradePasswordVerificationService passwordVerificationService;
    private final IOrderSequenceManagerService sequenceService;
    private final ITransactionService transactionService;
    private final TradeConfigSnapshotService snapshotService;
    private final AccountDataCipher cipher;

    public WithdrawalApplicationService(
            OrderUserMapper userMapper,
            OrderWithdrawalMapper withdrawalMapper,
            GoodsWithdrawalAccountMapper accountMapper,
            GoodsRechargeRecordMapper rechargeMapper,
            GoodsTransactionFlowMapper flowMapper,
            TradePasswordVerificationService passwordVerificationService,
            IOrderSequenceManagerService sequenceService,
            ITransactionService transactionService,
            TradeConfigSnapshotService snapshotService,
            AccountDataCipher cipher) {
        this.userMapper = userMapper;
        this.withdrawalMapper = withdrawalMapper;
        this.accountMapper = accountMapper;
        this.rechargeMapper = rechargeMapper;
        this.flowMapper = flowMapper;
        this.passwordVerificationService = passwordVerificationService;
        this.sequenceService = sequenceService;
        this.transactionService = transactionService;
        this.snapshotService = snapshotService;
        this.cipher = cipher;
    }

    @Transactional(rollbackFor = Exception.class)
    public WithdrawalResponse submit(Long userId, WithdrawalRequest request) {
        if (request == null || request.requestId() == null) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "requestId is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw AccountApiException.badRequest(AMOUNT_BELOW_MINIMUM, "Withdrawal amount must be greater than zero");
        }
        if (request.withdrawalAccountId() == null) {
            throw AccountApiException.badRequest(WITHDRAWAL_ACCOUNT, "Withdrawal account is required");
        }
        String requestId = request.requestId().toString();
        BigDecimal amount;
        try {
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw AccountApiException.badRequest(AMOUNT_BELOW_MINIMUM, "Withdrawal amount supports at most two decimal places");
        }
        OrderWithdrawal prior = withdrawalMapper.selectByUserAndRequestId(userId, requestId);
        if (prior != null) {
            return idempotentResult(prior, amount, request.withdrawalAccountId());
        }

        OrderUser passwordUser = userMapper.selectWithdrawalUserById(userId);
        if (passwordUser == null) {
            throw AccountApiException.notFound(USER_NOT_FOUND, "User not found");
        }
        verifyPassword(passwordUser, request.tradePassword());

        OrderUser user = userMapper.selectWithdrawalUserByIdForUpdate(userId);
        if (user == null) {
            throw AccountApiException.notFound(USER_NOT_FOUND, "User not found");
        }
        prior = withdrawalMapper.selectByUserAndRequestId(userId, requestId);
        if (prior != null) {
            return idempotentResult(prior, amount, request.withdrawalAccountId());
        }
        GoodsMemberLevel level = user.getMemberLevel();
        if (level == null || level.getId() == null) {
            throw AccountApiException.forbidden(MEMBER_LEVEL, "Member level is not configured");
        }
        TradeConfigSnapshotService.TradeConfigSnapshot snapshot = loadSnapshot();
        Map<String, Object> policy = snapshot.values();
        ZoneId zone = snapshot.zoneId();
        validateRules(user, level, policy, amount, zone);

        GoodsWithdrawalAccount account = accountMapper.selectActiveByIdAndUserId(request.withdrawalAccountId(), userId);
        if (account == null) {
            throw AccountApiException.badRequest(WITHDRAWAL_ACCOUNT, "Please check your withdrawal account");
        }
        if (withdrawalMapper.existsPendingByUserId(userId) != 0) {
            throw AccountApiException.conflict(PENDING_WITHDRAWAL, "A withdrawal request is already pending");
        }

        ZonedDateTime dayStart = LocalDate.now(zone).atStartOfDay(zone);
        Date start = Date.from(dayStart.toInstant());
        Date end = Date.from(dayStart.plusDays(1).toInstant());
        validateDailyUserLimits(userId, level, amount, start, end);

        LocalDate businessDate = dayStart.toLocalDate();
        BigDecimal platformLimit = decimal(policy, "platformDailyMaxWithdrawal");
        withdrawalMapper.ensureDailyQuota(businessDate, start, end);
        if (withdrawalMapper.reserveDailyQuota(businessDate, amount, platformLimit) != 1) {
            throw AccountApiException.conflict(PLATFORM_DAILY_LIMIT, "Platform daily withdrawal limit has been reached");
        }

        BigDecimal feeRate = positiveOrZero(level.getWithdrawFeeRate()) != null
                ? positiveOrZero(level.getWithdrawFeeRate())
                : positiveOrZero(decimal(policy, "withdrawalFeeRate"));
        BigDecimal fee = calculateFee(amount, feeRate);
        BigDecimal netAmount = amount.subtract(fee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal balanceBefore = user.getBalance();
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setAmount(amount);
        withdrawal.setFee(fee);
        withdrawal.setNetAmount(netAmount);
        withdrawal.setWithdrawalAccountId(account.getId());
        withdrawal.setWithdrawalType(account.getType());
        withdrawal.setTransactionType("tx");
        withdrawal.setStatus("1");
        withdrawal.setIsHidden("1");
        withdrawal.setRequestId(requestId);
        withdrawal.setOrderNumber(sequenceService.generateCode("TRADE_NO"));
        withdrawal.setCreateTime(DateUtils.getNowDate());
        withdrawal.setBusinessDate(businessDate);
        withdrawal.setAccountMask(cipher.displayMask(account));
        withdrawal.setAccountSnapshotEncrypted(cipher.encryptSnapshot(account));
        if (withdrawalMapper.insertOrderWithdrawal(withdrawal) != 1) {
            throw new IllegalStateException("Unable to create withdrawal");
        }
        if (userMapper.debitBalance(userId, amount) != 1) {
            throw AccountApiException.conflict(BALANCE, "Insufficient balance or concurrent balance update");
        }
        transactionService.recordFlow(userId, "tx", amount.negate(), balanceBefore,
                "withdrawal:" + withdrawal.getOrderNumber());
        log.info("event=withdrawal_submitted userId={} withdrawalId={} requestId={}",
                userId, withdrawal.getId(), requestId);
        return toResponse(withdrawal);
    }

    public WithdrawalResponse submitLegacy(Long userId, BigDecimal amount, String tradePassword, Long accountId) {
        return submit(userId, new WithdrawalRequest(UUID.randomUUID(), amount, tradePassword, accountId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long withdrawalId, String targetStatus, String remarks) {
        if (!"0".equals(targetStatus) && !"2".equals(targetStatus)) {
            throw AccountApiException.badRequest(REVIEW_CONFLICT, "Status must be success(0) or rejected(2)");
        }
        OrderWithdrawal withdrawal = withdrawalMapper.selectForUpdate(withdrawalId);
        if (withdrawal == null) {
            throw AccountApiException.notFound(REVIEW_CONFLICT, "Withdrawal not found");
        }
        if (!"1".equals(withdrawal.getStatus())) {
            throw AccountApiException.conflict(REVIEW_CONFLICT, "Only pending withdrawals can be reviewed");
        }
        if ("2".equals(targetStatus)) {
            OrderUser user = userMapper.selectWithdrawalUserByIdForUpdate(withdrawal.getUserId());
            if (user == null || userMapper.creditBalance(withdrawal.getUserId(), withdrawal.getAmount()) != 1) {
                throw new IllegalStateException("Unable to refund rejected withdrawal");
            }
            transactionService.recordFlow(withdrawal.getUserId(), "txbh", withdrawal.getAmount(),
                    user.getBalance(), "withdrawal-refund:" + withdrawal.getOrderNumber());
            LocalDate businessDate = withdrawal.getBusinessDate();
            if (businessDate == null) {
                businessDate = Instant.ofEpochMilli(withdrawal.getCreateTime().getTime())
                        .atZone(activeZone()).toLocalDate();
            }
            ZonedDateTime quotaStart = businessDate.atStartOfDay(activeZone());
            withdrawalMapper.ensureDailyQuota(
                    businessDate,
                    Date.from(quotaStart.toInstant()),
                    Date.from(quotaStart.plusDays(1).toInstant()));
            withdrawalMapper.releaseDailyQuota(businessDate, withdrawal.getAmount());
        }
        if (withdrawalMapper.transitionStatus(withdrawalId, "1", targetStatus, trimToNull(remarks)) != 1) {
            throw AccountApiException.conflict(REVIEW_CONFLICT, "Withdrawal was reviewed by another request");
        }
        log.info("event=withdrawal_reviewed withdrawalId={} targetStatus={}", withdrawalId, targetStatus);
    }

    @Transactional(readOnly = true)
    public SensitiveWithdrawalAccountResponse sensitiveAccount(Long withdrawalId) {
        OrderWithdrawal withdrawal = withdrawalMapper.selectOrderWithdrawalById(withdrawalId);
        if (withdrawal == null) {
            throw AccountApiException.notFound(WITHDRAWAL_NOT_FOUND, "Withdrawal does not exist");
        }

        if (withdrawal.getAccountSnapshotEncrypted() != null
                && !withdrawal.getAccountSnapshotEncrypted().isBlank()) {
            Map<String, Object> snapshot = cipher.decryptSnapshot(
                    withdrawal.getUserId(), withdrawal.getAccountSnapshotEncrypted());
            return new SensitiveWithdrawalAccountResponse(
                    withdrawalId,
                    text(snapshot.get("type")),
                    text(snapshot.get("withdrawalType")),
                    text(snapshot.get("bankName")),
                    text(snapshot.get("depositType")),
                    text(snapshot.get("branchCode")),
                    text(snapshot.get("branchName")),
                    text(snapshot.get("bankAccount")),
                    text(snapshot.get("accountHolder")),
                    text(snapshot.get("accountName")),
                    text(snapshot.get("walletName")),
                    text(snapshot.get("walletAddress")));
        }

        GoodsWithdrawalAccount account = cipher.reveal(withdrawal.getWithdrawalAccountInfo());
        if (account == null) {
            throw AccountApiException.notFound(WITHDRAWAL_NOT_FOUND, "Withdrawal account snapshot is unavailable");
        }
        return new SensitiveWithdrawalAccountResponse(
                withdrawalId,
                account.getType(),
                account.getWithdrawalType(),
                account.getBankName(),
                account.getDepositType(),
                account.getBranchCode(),
                account.getBranchName(),
                account.getBankAccount(),
                account.getAccountHolder(),
                account.getAccountName(),
                account.getWalletName(),
                account.getWalletAddress());
    }

    @Transactional(readOnly = true)
    public TableDataInfo withdrawalHistory(Long userId, String status, int pageNum, int pageSize) {
        String normalizedStatus = normalizeStatus(status);
        PageHelper.startPage(normalizePage(pageNum), normalizeSize(pageSize));
        List<OrderWithdrawal> rows = withdrawalMapper.selectPublicByUser(userId, normalizedStatus);
        List<WithdrawalHistoryResponse> result = rows.stream()
                .map(row -> new WithdrawalHistoryResponse(
                        row.getId(), row.getOrderNumber(), row.getAmount(), row.getFee(),
                        row.getNetAmount() == null ? subtract(row.getAmount(), row.getFee()) : row.getNetAmount(),
                        row.getStatus(), row.getRemarks(), row.getAccountMask(), row.getCreateTime()))
                .toList();
        return page(result, new PageInfo<>(rows).getTotal());
    }

    @Transactional(readOnly = true)
    public TableDataInfo deposits(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(normalizePage(pageNum), normalizeSize(pageSize));
        List<GoodsRechargeRecord> rows = rechargeMapper.selectPublicByUserId(userId);
        List<DepositResponse> result = rows.stream().map(row -> new DepositResponse(
                row.getId(), row.getAmount(), row.getGiftAmount(), row.getReceivedAmount(), row.getStatus(),
                row.getTransactionType(), row.getOrderNumber(), row.getRemark(), row.getCreateTime())).toList();
        return page(result, new PageInfo<>(rows).getTotal());
    }

    @Transactional(readOnly = true)
    public TableDataInfo transactions(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(normalizePage(pageNum), normalizeSize(pageSize));
        List<GoodsTransactionFlow> rows = flowMapper.selectPublicByUserId(userId);
        List<TransactionResponse> result = rows.stream().map(row -> new TransactionResponse(
                row.getId(), row.getSerialCode(), row.getTransactionType(), row.getTransactionAmount(),
                row.getBalanceBefore(), row.getBalanceAfter(), row.getTransactionCode(),
                row.getRemark(), row.getCreatedTime())).toList();
        return page(result, new PageInfo<>(rows).getTotal());
    }

    private void validateRules(OrderUser user, GoodsMemberLevel level, Map<String, Object> policy,
                               BigDecimal amount, ZoneId zone) {
        if (!isEnabled(policy.get("memberWithdrawalStatus"))) {
            throw AccountApiException.forbidden(WITHDRAWAL_DISABLED, "Member withdrawals are disabled");
        }
        if ("1".equals(user.getWithdrawalStatus())) {
            throw AccountApiException.forbidden(WITHDRAWAL_DISABLED, firstNonBlank(user.getWithdrawalBlockRemark(),
                    "Withdrawal is disabled for this account"));
        }
        if (isYes(policy.get("prohibitWithdrawalAfterRecharge"))
                && "0".equals(user.getDepositBlockWithdrawal())) {
            throw AccountApiException.forbidden(WITHDRAWAL_DISABLED, "Withdrawal is blocked after recharge");
        }
        if (!withinTimeRange(policy.get("withdrawalTimeRange"), zone)) {
            throw AccountApiException.forbidden(OUTSIDE_WITHDRAWAL_WINDOW, "Current time is outside the withdrawal window");
        }
        BigDecimal minCredit = decimal(policy, "minCreditScoreForWithdrawal");
        BigDecimal credit = user.getReputationScore() == null
                ? BigDecimal.ZERO : BigDecimal.valueOf(user.getReputationScore());
        if (positive(minCredit) && credit.compareTo(minCredit) < 0) {
            throw AccountApiException.forbidden(CREDIT_SCORE, "Credit score is below the withdrawal requirement");
        }
        BigDecimal min = maxPositive(decimal(policy, "minWithdrawalAmount"), level.getMinWithdraw());
        if (positive(min) && amount.compareTo(min) < 0) {
            throw AccountApiException.badRequest(AMOUNT_BELOW_MINIMUM, "Withdrawal amount is below the minimum");
        }
        BigDecimal max = minPositive(decimal(policy, "maxWithdrawalAmount"),
                level.getMaxWithdraw(), user.getMaxSingleWithdrawal());
        if (positive(max) && amount.compareTo(max) > 0) {
            throw AccountApiException.badRequest(AMOUNT_ABOVE_MAXIMUM, "Withdrawal amount exceeds the maximum");
        }
        long requiredOrders = maxLong(longValue(policy, "requiredTaskGroupsForWithdrawal"),
                level.getMinWithdrawAmount() == null ? 0L : level.getMinWithdrawAmount().longValue());
        long progress = user.getTaskProgress() == null ? 0L : user.getTaskProgress();
        if (requiredOrders > 0 && progress < requiredOrders) {
            throw AccountApiException.forbidden(TASK_REQUIREMENT, "Please complete the required tasks before withdrawing");
        }
        BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw AccountApiException.badRequest(BALANCE, "Insufficient balance");
        }
        if (isYes(policy.get("withdrawalRestrictLevelMinBalance"))
                && positive(level.getMinBalance())
                && balance.subtract(amount).compareTo(level.getMinBalance()) < 0) {
            throw AccountApiException.badRequest(LEVEL_MINIMUM_BALANCE, "Remaining balance would be below the member level minimum");
        }
    }

    private void validateDailyUserLimits(Long userId, GoodsMemberLevel level, BigDecimal amount,
                                         Date start, Date end) {
        WithdrawalDailyUsage usage = withdrawalMapper.selectDailyUsage(userId, start, end);
        int count = usage == null ? 0 : usage.getWithdrawalCount();
        if (level.getWithdrawCountPerDay() != null && level.getWithdrawCountPerDay() > 0
                && count >= level.getWithdrawCountPerDay()) {
            throw AccountApiException.conflict(DAILY_WITHDRAWAL_COUNT, "Daily withdrawal count has been reached");
        }
        BigDecimal used = usage == null || usage.getWithdrawalAmount() == null
                ? BigDecimal.ZERO : usage.getWithdrawalAmount();
        if (positive(level.getWithdrawLimitPerDay())
                && used.add(amount).compareTo(level.getWithdrawLimitPerDay()) > 0) {
            throw AccountApiException.conflict(DAILY_WITHDRAWAL_AMOUNT, "Daily withdrawal amount has been reached");
        }
    }

    private void verifyPassword(OrderUser user, String rawPassword) {
        TradePasswordVerificationService.Result result = passwordVerificationService.verify(user, rawPassword);
        if (result == TradePasswordVerificationService.Result.BLOCKED) {
            throw AccountApiException.forbidden(INVALID_REQUEST, "Trade password failure limit has been reached");
        }
        if (result == TradePasswordVerificationService.Result.MISSING) {
            throw AccountApiException.badRequest(TRADE_PASSWORD, "Trade password is required");
        }
        if (result == TradePasswordVerificationService.Result.WRONG) {
            throw AccountApiException.forbidden(TRADE_PASSWORD, "Trade password is incorrect");
        }
    }

    private OrderWithdrawal idempotentRow(OrderWithdrawal existing, BigDecimal amount, Long accountId) {
        if (existing.getAmount().compareTo(amount) != 0
                || !existing.getWithdrawalAccountId().equals(accountId)) {
            throw AccountApiException.conflict(IDEMPOTENCY_CONFLICT, "requestId was already used with different parameters");
        }
        return existing;
    }

    private WithdrawalResponse idempotentResult(OrderWithdrawal existing, BigDecimal amount, Long accountId) {
        log.info("event=withdrawal_idempotency_hit userId={} requestId={}",
                existing.getUserId(), existing.getRequestId());
        return toResponse(idempotentRow(existing, amount, accountId));
    }

    private WithdrawalResponse toResponse(OrderWithdrawal row) {
        BigDecimal net = row.getNetAmount() == null ? subtract(row.getAmount(), row.getFee()) : row.getNetAmount();
        return new WithdrawalResponse(row.getId(), row.getOrderNumber(), row.getAmount(),
                row.getFee(), net, row.getStatus(), row.getCreateTime());
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private TradeConfigSnapshotService.TradeConfigSnapshot loadSnapshot() {
        try {
            return snapshotService.snapshot();
        } catch (TradeConfigSnapshotService.SnapshotException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    private ZoneId activeZone() {
        return loadSnapshot().zoneId();
    }

    private boolean withinTimeRange(Object value, ZoneId zone) {
        return withinTimeRange(value, LocalTime.now(zone));
    }

    static boolean withinTimeRange(Object value, LocalTime now) {
        if (!(value instanceof List<?> range) || range.size() != 2) {
            throw new IllegalStateException("withdrawalTimeRange must contain start and end");
        }
        LocalTime start = LocalTime.parse(String.valueOf(range.get(0)), TIME_FORMAT);
        LocalTime end = LocalTime.parse(String.valueOf(range.get(1)), TIME_FORMAT);
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }

    static BigDecimal calculateFee(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalStateException("Withdrawal fee rate cannot exceed 100%");
        }
        return amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private TableDataInfo page(List<?> rows, long total) {
        TableDataInfo result = new TableDataInfo();
        result.setCode(200);
        result.setMsg("查询成功");
        result.setRows(rows);
        result.setTotal(total);
        return result;
    }

    private int normalizePage(int value) {
        if (value < 1) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Invalid page number");
        }
        return value;
    }

    private int normalizeSize(int value) {
        if (value < 1 || value > ApiPagination.MAX_PAGE_SIZE) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Invalid page size");
        }
        return value;
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) return null;
        if (!List.of("0", "1", "2").contains(value)) {
            throw AccountApiException.badRequest(INVALID_REQUEST, "Invalid withdrawal status");
        }
        return value;
    }

    private BigDecimal decimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid numeric configuration: " + key, ex);
        }
    }

    private long longValue(Map<String, Object> map, String key) {
        BigDecimal value = decimal(map, key);
        return value == null ? 0L : value.longValue();
    }

    private boolean isEnabled(Object value) {
        if (value == null) return false;
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "0".equals(normalized) || "enabled".equals(normalized)
                || "true".equals(normalized) || "yes".equals(normalized);
    }

    private boolean isYes(Object value) {
        return isEnabled(value);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal positiveOrZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0 ? value : null;
    }

    static BigDecimal maxPositive(BigDecimal... values) {
        BigDecimal result = null;
        for (BigDecimal value : values) {
            if (positive(value) && (result == null || value.compareTo(result) > 0)) result = value;
        }
        return result;
    }

    static BigDecimal minPositive(BigDecimal... values) {
        BigDecimal result = null;
        for (BigDecimal value : values) {
            if (positive(value) && (result == null || value.compareTo(result) < 0)) result = value;
        }
        return result;
    }

    private long maxLong(long first, long second) {
        return Math.max(first, second);
    }

    private BigDecimal subtract(BigDecimal amount, BigDecimal fee) {
        if (amount == null) return null;
        return amount.subtract(fee == null ? BigDecimal.ZERO : fee).setScale(2, RoundingMode.HALF_UP);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
