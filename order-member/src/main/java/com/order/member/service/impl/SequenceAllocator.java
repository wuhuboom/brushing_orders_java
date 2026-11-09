package com.order.member.service.impl;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Allocations commit independently; unused numbers after business rollback are intentional. */
@Component
public class SequenceAllocator {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public SequenceAllocator(@Qualifier("sequenceDataSource") DataSource source) {
        jdbc = new JdbcTemplate(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(5);
    }

    public long allocateLinkBatch() {
        return transaction.execute(status -> {
            if (jdbc.update("""
                    UPDATE order_link_batch_sequence SET current_value = current_value + 1
                    WHERE id = 1 AND current_value < 9223372036854775807
                    """) != 1) {
                throw new IllegalStateException("Link batch sequence is missing or exhausted");
            }
            Long value = jdbc.queryForObject(
                    "SELECT current_value FROM order_link_batch_sequence WHERE id = 1", Long.class);
            if (value == null || value <= 0) {
                throw new IllegalStateException("Invalid link batch sequence");
            }
            return value;
        });
    }

    public long allocate(String type, String date, String time) {
        if (type == null || type.isBlank() || date == null || !date.matches("[0-9]{8}")
                || time == null || !time.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("Invalid sequence key");
        }
        return transaction.execute(status -> {
            // Atomic upsert avoids SELECT FOR UPDATE on an absent row (gap-lock races).
            jdbc.update("""
                INSERT INTO order_sequence_manager
                    (seq_type, seq_date, seq_time, current_seq, version, create_time, update_time)
                VALUES (?, ?, ?, 1, 0, NOW(3), NOW(3))
                ON DUPLICATE KEY UPDATE current_seq = current_seq + 1, update_time = NOW(3)
                """, type, date, time);
            Long value = jdbc.queryForObject("""
                SELECT current_seq FROM order_sequence_manager
                WHERE seq_type = ? AND seq_date = ? AND seq_time = ?
                """, Long.class, type, date, time);
            if (value == null || value <= 0 || value > 99999999L) {
                throw new IllegalStateException("Sequence exhausted");
            }
            return value;
        });
    }
}
