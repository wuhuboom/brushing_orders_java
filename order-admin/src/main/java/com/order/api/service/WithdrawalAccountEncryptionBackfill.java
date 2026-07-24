package com.order.api.service;

import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Opt-in, resumable first-stage encryption backfill. It is never exposed as an HTTP endpoint.
 */
@Component
public class WithdrawalAccountEncryptionBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(WithdrawalAccountEncryptionBackfill.class);

    private final boolean enabled;
    private final int batchSize;
    private final String direction;
    private final GoodsWithdrawalAccountMapper mapper;
    private final AccountDataCipher cipher;
    private final TransactionTemplate transactionTemplate;

    public WithdrawalAccountEncryptionBackfill(
            @Value("${account-security.backfill-enabled:false}") boolean enabled,
            @Value("${account-security.backfill-batch-size:200}") int batchSize,
            @Value("${account-security.backfill-direction:ENCRYPT}") String direction,
            GoodsWithdrawalAccountMapper mapper,
            AccountDataCipher cipher,
            TransactionTemplate transactionTemplate) {
        this.enabled = enabled;
        this.batchSize = Math.max(1, Math.min(batchSize, 1000));
        this.direction = direction == null ? "ENCRYPT" : direction.trim().toUpperCase();
        this.mapper = mapper;
        this.cipher = cipher;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (cipher.mode() == AccountDataCipher.Mode.LEGACY_READ) {
            throw new IllegalStateException("Encryption backfill requires DUAL_WRITE or ENCRYPTED_ONLY mode");
        }
        if ("DECRYPT".equals(direction)) {
            restorePlaintext();
            return;
        }
        if (!"ENCRYPT".equals(direction)) {
            throw new IllegalStateException("Account backfill direction must be ENCRYPT or DECRYPT");
        }
        long afterId = 0L;
        long migrated = 0L;
        while (true) {
            List<GoodsWithdrawalAccount> batch = mapper.selectPlaintextBackfillBatch(afterId, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            transactionTemplate.executeWithoutResult(status -> {
                for (GoodsWithdrawalAccount account : batch) {
                    cipher.protect(account);
                    mapper.updateEncryptionColumns(account);
                }
            });
            afterId = batch.get(batch.size() - 1).getId();
            migrated += batch.size();
            log.info("event=withdrawal_account_encryption_backfill migrated={} lastId={}", migrated, afterId);
        }
        log.info("event=withdrawal_account_encryption_backfill_complete migrated={}", migrated);
    }

    private void restorePlaintext() {
        long afterId = 0L;
        long restored = 0L;
        while (true) {
            List<GoodsWithdrawalAccount> batch =
                    mapper.selectCiphertextRollbackBatch(afterId, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            transactionTemplate.executeWithoutResult(status -> {
                for (GoodsWithdrawalAccount account : batch) {
                    cipher.reveal(account);
                    mapper.restorePlaintextColumns(account);
                }
            });
            afterId = batch.get(batch.size() - 1).getId();
            restored += batch.size();
            log.info("event=withdrawal_account_plaintext_restore restored={} lastId={}",
                    restored, afterId);
        }
        log.info("event=withdrawal_account_plaintext_restore_complete restored={}", restored);
    }
}
