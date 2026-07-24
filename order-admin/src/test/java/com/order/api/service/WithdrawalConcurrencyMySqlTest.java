package com.order.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class WithdrawalConcurrencyMySqlTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42")
            .withDatabaseName("order_test")
            .withUsername("order")
            .withPassword("order");

    @BeforeEach
    void createSchema() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS order_withdrawal");
            statement.execute("DROP TABLE IF EXISTS withdrawal_daily_quota");
            statement.execute("DROP TABLE IF EXISTS order_user");
            statement.execute("DROP TABLE IF EXISTS goods_withdrawal_account");
            statement.execute("DROP TABLE IF EXISTS goods_recharge_record");
            statement.execute("DROP TABLE IF EXISTS goods_transaction_flow");
            statement.execute("DROP TABLE IF EXISTS sys_menu");
            statement.execute("""
                    CREATE TABLE order_user (
                        id BIGINT PRIMARY KEY,
                        balance DECIMAL(20,2) NOT NULL,
                        trade_password VARCHAR(255),
                        withdrawal_password_fail_limit INT DEFAULT 5,
                        withdrawal_password_fail_count INT DEFAULT 0
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE order_withdrawal (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        amount DECIMAL(20,2) NOT NULL,
                        status CHAR(1) NOT NULL,
                        order_number VARCHAR(64),
                        fee DECIMAL(20,2),
                        withdrawal_account_id BIGINT,
                        create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        INDEX idx_user_status (user_id, status)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods_withdrawal_account (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        type CHAR(1),
                        withdrawal_type_id BIGINT,
                        is_default CHAR(1),
                        bank_name VARCHAR(100),
                        deposit_type VARCHAR(50),
                        branch_code VARCHAR(50),
                        branch_name VARCHAR(100),
                        bank_account VARCHAR(100),
                        account_holder VARCHAR(100),
                        account_name VARCHAR(100),
                        wallet_name VARCHAR(100),
                        wallet_address VARCHAR(255),
                        create_time DATETIME(3)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods_recharge_record (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT,
                        create_time DATETIME(3)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods_transaction_flow (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT,
                        created_time DATETIME(3)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE sys_menu (
                        menu_id BIGINT PRIMARY KEY,
                        menu_name VARCHAR(100),
                        parent_id BIGINT,
                        order_num INT,
                        path VARCHAR(200),
                        component VARCHAR(255),
                        query VARCHAR(255),
                        route_name VARCHAR(100),
                        is_frame INT,
                        is_cache INT,
                        menu_type CHAR(1),
                        visible CHAR(1),
                        status CHAR(1),
                        perms VARCHAR(100),
                        icon VARCHAR(100),
                        create_by VARCHAR(64),
                        create_time DATETIME,
                        update_by VARCHAR(64),
                        update_time DATETIME,
                        remark VARCHAR(500)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    INSERT INTO sys_menu(
                        menu_id, menu_name, parent_id, order_num, path, component, query,
                        route_name, is_frame, is_cache, menu_type, visible, status, perms,
                        icon, create_by, create_time, update_by, update_time, remark
                    ) VALUES (
                        1, 'Query', 100, 1, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
                        'member:withdrawal:query', '#', 'test', NOW(), '', NULL, ''
                    )
                    """);
            statement.execute("""
                    CREATE TABLE withdrawal_daily_quota (
                        business_date DATE PRIMARY KEY,
                        reserved_amount DECIMAL(20,2) NOT NULL,
                        update_time DATETIME(3) NOT NULL
                    ) ENGINE=InnoDB
                    """);
        }
    }

    @Test
    void sameUserConcurrentSubmissionsCreateOnlyOnePendingWithdrawal() throws Exception {
        execute("INSERT INTO order_user(id, balance) VALUES (7, 1000)");
        List<Boolean> results = concurrently(this::submitWhileHoldingUserLock,
                this::submitWhileHoldingUserLock);

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1L, queryLong(
                "SELECT COUNT(*) FROM order_withdrawal WHERE user_id = 7 AND status = '1'"));
    }

    @Test
    void platformDailyQuotaCannotBeOversold() throws Exception {
        execute("""
                INSERT INTO withdrawal_daily_quota(business_date, reserved_amount, update_time)
                VALUES (CURRENT_DATE, 0, NOW(3))
                """);
        List<Boolean> results = concurrently(this::reserveSeventy, this::reserveSeventy);

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(new BigDecimal("70.00"), queryDecimal(
                "SELECT reserved_amount FROM withdrawal_daily_quota WHERE business_date = CURRENT_DATE"));
    }

    @Test
    void concurrentDoubleRejectionRefundsExactlyOnce() throws Exception {
        execute("INSERT INTO order_user(id, balance) VALUES (7, 0)");
        execute("""
                INSERT INTO order_withdrawal(id, user_id, amount, status)
                VALUES (9, 7, 100, '1')
                """);
        List<Boolean> results = concurrently(this::rejectWhileHoldingWithdrawalLock,
                this::rejectWhileHoldingWithdrawalLock);

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(new BigDecimal("100.00"),
                queryDecimal("SELECT balance FROM order_user WHERE id = 7"));
        assertEquals("2", queryString("SELECT status FROM order_withdrawal WHERE id = 9"));
    }

    @Test
    void concurrentTradePasswordFailuresAreNotLost() throws Exception {
        execute("""
                INSERT INTO order_user(
                    id, balance, trade_password,
                    withdrawal_password_fail_limit, withdrawal_password_fail_count
                ) VALUES (7, 1000, 'encoded', 5, 0)
                """);

        concurrently(this::incrementTradePasswordFailureWhileHoldingLock,
                this::incrementTradePasswordFailureWhileHoldingLock);

        assertEquals(2L, queryLong("""
                SELECT withdrawal_password_fail_count
                FROM order_user WHERE id = 7
                """));
    }

    @Test
    void runtimeOptimizationMigrationCanRunTwice() throws Exception {
        execute("ALTER TABLE order_withdrawal ADD COLUMN business_date DATE NULL");
        execute("""
                ALTER TABLE order_withdrawal
                ADD INDEX idx_withdrawal_user_status_time(
                    user_id, status, create_time
                )
                """);
        Path migration = findMigration("2026-07-18_api_runtime_optimization.sql");

        runMysqlScript(migration);
        runMysqlScript(migration);

        assertEquals(1L, queryLong("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'order_withdrawal'
                  AND index_name = 'idx_withdrawal_business_status_time'
                """));
        execute("""
                INSERT INTO order_withdrawal(
                    user_id, amount, status, business_date, create_time
                ) VALUES (7, 10, '1', CURRENT_DATE, NOW(3))
                """);
        String explain = queryString("""
                EXPLAIN ANALYZE
                SELECT id
                FROM order_withdrawal
                    FORCE INDEX (idx_withdrawal_business_status_time)
                WHERE business_date = CURRENT_DATE
                  AND status IN ('0', '1')
                  AND create_time >= CURRENT_DATE
                  AND create_time < CURRENT_DATE + INTERVAL 1 DAY
                ORDER BY create_time
                """);
        assertTrue(explain.contains("idx_withdrawal_business_status_time"));

        String aggregatePlan = queryString("""
                EXPLAIN ANALYZE
                SELECT COUNT(*), COALESCE(SUM(amount), 0)
                FROM order_withdrawal
                    FORCE INDEX (idx_withdrawal_user_status_time)
                WHERE user_id = 7
                  AND status IN ('0', '1')
                  AND create_time >= CURRENT_DATE
                  AND create_time < CURRENT_DATE + INTERVAL 1 DAY
                """);
        assertTrue(aggregatePlan.contains("idx_withdrawal_user_status_time"));
    }

    @Test
    void phaseOneMigrationAppliesToMySqlEight() throws Exception {
        execute("DROP TABLE withdrawal_daily_quota");
        execute("""
                INSERT INTO goods_withdrawal_account(
                    id, user_id, type, is_default, bank_name, bank_account,
                    account_holder, create_time
                ) VALUES (3, 7, '0', '0', 'Bank A', '6222021234567890', 'Alice', NOW(3))
                """);
        execute("""
                INSERT INTO order_withdrawal(
                    id, user_id, amount, status, order_number, fee,
                    withdrawal_account_id, create_time
                ) VALUES (9, 7, 100, '1', 'W1', 0.50, 3, NOW(3))
                """);

        Path migration = findMigration("2026-07-17_account_withdrawal_hardening.sql");
        String script = Files.readString(migration).replaceAll("(?m)^--.*$", "");
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }

        assertEquals(new BigDecimal("99.50"),
                queryDecimal("SELECT net_amount FROM order_withdrawal WHERE id = 9"));
        assertEquals("Bank A ****7890",
                queryString("SELECT account_mask FROM order_withdrawal WHERE id = 9"));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'goods_withdrawal_account'
                  AND column_name = 'bank_account_enc'
                """));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM sys_menu
                WHERE perms = 'member:withdrawal:sensitive'
                """));
    }

    private boolean submitWhileHoldingUserLock() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id FROM order_user WHERE id = 7 FOR UPDATE")) {
                    lock.executeQuery().close();
                }
                long pending;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT COUNT(*) FROM order_withdrawal WHERE user_id = 7 AND status = '1'");
                     ResultSet result = select.executeQuery()) {
                    result.next();
                    pending = result.getLong(1);
                }
                if (pending != 0) {
                    connection.rollback();
                    return false;
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO order_withdrawal(user_id, amount, status) VALUES (7, 10, '1')")) {
                    insert.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private boolean reserveSeventy() throws Exception {
        try (Connection connection = connection();
             PreparedStatement update = connection.prepareStatement("""
                     UPDATE withdrawal_daily_quota
                     SET reserved_amount = reserved_amount + 70, update_time = NOW(3)
                     WHERE business_date = CURRENT_DATE
                       AND reserved_amount + 70 <= 100
                     """)) {
            return update.executeUpdate() == 1;
        }
    }

    private boolean rejectWhileHoldingWithdrawalLock() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                long userId;
                BigDecimal amount;
                String status;
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT user_id, amount, status FROM order_withdrawal WHERE id = 9 FOR UPDATE");
                     ResultSet result = lock.executeQuery()) {
                    result.next();
                    userId = result.getLong("user_id");
                    amount = result.getBigDecimal("amount");
                    status = result.getString("status");
                }
                if (!"1".equals(status)) {
                    connection.rollback();
                    return false;
                }
                try (PreparedStatement credit = connection.prepareStatement(
                        "UPDATE order_user SET balance = balance + ? WHERE id = ?")) {
                    credit.setBigDecimal(1, amount);
                    credit.setLong(2, userId);
                    credit.executeUpdate();
                }
                try (PreparedStatement transition = connection.prepareStatement(
                        "UPDATE order_withdrawal SET status = '2' WHERE id = 9 AND status = '1'")) {
                    if (transition.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                connection.commit();
                return true;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private boolean incrementTradePasswordFailureWhileHoldingLock() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement("""
                        SELECT trade_password, withdrawal_password_fail_limit,
                               withdrawal_password_fail_count
                        FROM order_user
                        WHERE id = 7
                        FOR UPDATE
                        """)) {
                    lock.executeQuery().close();
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE order_user
                        SET withdrawal_password_fail_count =
                                COALESCE(withdrawal_password_fail_count, 0) + 1
                        WHERE id = 7
                        """)) {
                    update.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private List<Boolean> concurrently(Callable<Boolean> first, Callable<Boolean> second)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> firstTask = gated(ready, start, first);
        Callable<Boolean> secondTask = gated(ready, start, second);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(firstTask);
            Future<Boolean> secondResult = executor.submit(secondTask);
            ready.await();
            start.countDown();
            return List.of(firstResult.get(), secondResult.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> gated(
            CountDownLatch ready, CountDownLatch start, Callable<Boolean> task) {
        return () -> {
            ready.countDown();
            start.await();
            return task.call();
        };
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private Path findMigration(String name) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("sql").resolve(name);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Migration script was not found: " + name);
    }

    private void runMysqlScript(Path migration) throws Exception {
        List<String> lines = Files.readAllLines(migration);
        String delimiter = ";";
        StringBuilder statementBuffer = new StringBuilder();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                if (trimmed.regionMatches(true, 0, "DELIMITER ", 0, 10)) {
                    delimiter = trimmed.substring(10).trim();
                    continue;
                }
                statementBuffer.append(line).append('\n');
                String pending = statementBuffer.toString().trim();
                if (pending.endsWith(delimiter)) {
                    String sql = pending.substring(0, pending.length() - delimiter.length()).trim();
                    if (!sql.isEmpty()) {
                        statement.execute(sql);
                    }
                    statementBuffer.setLength(0);
                }
            }
        }
        if (!statementBuffer.toString().isBlank()) {
            throw new IllegalStateException("Unterminated SQL in migration: " + migration);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long queryLong(String sql) throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private BigDecimal queryDecimal(String sql) throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBigDecimal(1);
        }
    }

    private String queryString(String sql) throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
