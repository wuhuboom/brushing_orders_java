package com.order.member.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.order.member.service.impl.SequenceAllocator;
import com.order.member.domain.dto.TransactionDto;
import com.order.web.controller.member.OrderUserController;
import db.migration.mysql.V2026_11_09_00__identity_indexes_and_link_batch_sequence;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/** Explicit opt-in only: this fixture never connects to the application database. */
class BackendHardeningMySqlTest {
    private DruidDataSource source;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() throws Exception {
        String url = System.getProperty("hardening.jdbcUrl");
        assumeTrue(url != null, "Requires isolated codex_backend_hardening_test database");
        URI endpoint = URI.create(url.substring("jdbc:".length()));
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals("/codex_backend_hardening_test", endpoint.getPath());
        assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
        assertNull(endpoint.getRawQuery());
        assertNull(endpoint.getRawFragment());
        source = new DruidDataSource();
        source.setUrl(url);
        source.setUsername(System.getenv("HARDENING_DB_USER"));
        source.setPassword(System.getenv("HARDENING_DB_PASSWORD"));
        source.setMaxActive(12);
        jdbc = new JdbcTemplate(source);
        assertEquals("codex_backend_hardening_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        for (String table : List.of("order_user", "order_info", "order_login_log",
                "order_withdrawal", "order_link", "order_link_batch_sequence",
                "admin_transaction_request", "gift_test_balance")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("""
                CREATE TABLE order_user (id BIGINT PRIMARY KEY, username VARCHAR(64),
                phone_number VARCHAR(64), invite_code VARCHAR(64), parent_id BIGINT,
                UNIQUE KEY idx_phone_number_username(phone_number,username)) ENGINE=InnoDB
                """);
        jdbc.execute("CREATE TABLE order_link (id BIGINT PRIMARY KEY, link_order_id BIGINT) ENGINE=InnoDB");
        for (String table : List.of("order_info", "order_login_log", "order_withdrawal")) {
            jdbc.execute("CREATE TABLE " + table
                    + " (id BIGINT PRIMARY KEY, user_id BIGINT, create_time DATETIME) ENGINE=InnoDB");
        }
    }

    @AfterEach
    void close() { if (source != null) source.close(); }

    @Test
    void duplicateIdentitiesStopBeforeAnyDdl() {
        jdbc.update("INSERT INTO order_user VALUES (1,'duplicate','phone1','invite1',0),(2,'duplicate','phone2','invite2',0)");
        assertThrows(SQLException.class, this::migrate);
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE()
                AND TABLE_NAME='order_user' AND INDEX_NAME='uk_order_user_username'
                """, Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM order_user", Integer.class));
    }

    @Test
    void flywayDiscoversJavaMigrationInTheGuardedLocation() {
        var flyway = Flyway.configure().dataSource(source)
                .locations("classpath:db/migration/mysql").load();
        var migration = java.util.Arrays.stream(flyway.info().all())
                .filter(info -> info.getVersion() != null
                        && info.getVersion().compareTo(org.flywaydb.core.api.MigrationVersion.fromVersion("2026.11.09.00")) == 0)
                .findFirst().orElseThrow();
        assertNotNull(migration.getChecksum());
    }

    @Test
    void concurrentGiftRetriesCommitExactlyOneCredit() throws Exception {
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/mysql/V2026_10_27_00__admin_transaction_requests.sql")).execute(source);
        jdbc.execute("CREATE TABLE gift_test_balance (id BIGINT PRIMARY KEY, balance DECIMAL(18,2)) ENGINE=InnoDB");
        jdbc.update("INSERT INTO gift_test_balance VALUES (1,0)");
        ITransactionService money = mock(ITransactionService.class);
        when(money.processTransaction(any())).thenAnswer(invocation -> {
            TransactionDto intent = invocation.getArgument(0);
            jdbc.update("UPDATE gift_test_balance SET balance=balance+? WHERE id=1", intent.getAmount());
            return true;
        });
        var requests = new AdminTransactionRequestService(source,
                new DataSourceTransactionManager(source), money);
        var controller = new OrderUserController() {
            @Override public Long getUserId() { return 42L; }
        };
        ReflectionTestUtils.setField(controller, "adminTransactionRequests", requests);
        TransactionDto gift = new TransactionDto();
        gift.setRequestId("concurrent-gift-00001");
        gift.setUserId(1L);
        gift.setAmount(new BigDecimal("12.00"));
        var workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) results.add(workers.submit(() -> controller.giftAmount(gift).get("code")));
            for (Future<Object> result : results) assertEquals(200, result.get(15, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(new BigDecimal("12.00"), jdbc.queryForObject("SELECT balance FROM gift_test_balance WHERE id=1", BigDecimal.class));
            verify(money, times(1)).processTransaction(any());
            gift.setAmount(new BigDecimal("13.00"));
            assertThrows(IllegalArgumentException.class, () -> controller.giftAmount(gift));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void migrationEnforcesIndividualIdentitiesAndCanResume() throws Exception {
        migrate();
        migrate();
        jdbc.update("INSERT INTO order_user VALUES (1,'member1','phone1','invite1',0)");
        assertThrows(RuntimeException.class, () -> jdbc.update(
                "INSERT INTO order_user VALUES (2,'member1','phone2','invite2',0)"));
        assertThrows(RuntimeException.class, () -> jdbc.update(
                "INSERT INTO order_user VALUES (3,'member3','phone1','invite3',0)"));
        assertThrows(RuntimeException.class, () -> jdbc.update(
                "INSERT INTO order_user VALUES (4,'member4','phone4','invite1',0)"));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE()
                AND TABLE_NAME='order_login_log' AND INDEX_NAME='idx_login_user_time' AND COLUMN_NAME='user_id'
                """, Integer.class));
    }

    @Test
    void batchIdsAreUniqueAcrossAllocatorsAndSurviveBusinessRollback() throws Exception {
        jdbc.update("INSERT INTO order_link VALUES (1,71)");
        migrate();
        SequenceAllocator first = new SequenceAllocator(source);
        SequenceAllocator second = new SequenceAllocator(source);
        var workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<Long>> results = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                SequenceAllocator allocator = i % 2 == 0 ? first : second;
                results.add(workers.submit(allocator::allocateLinkBatch));
            }
            var ids = new HashSet<Long>();
            for (Future<Long> result : results) assertTrue(ids.add(result.get(15, java.util.concurrent.TimeUnit.SECONDS)));
            assertEquals(40, ids.size());
            assertEquals(72L, ids.stream().min(Long::compareTo).orElseThrow());
            AtomicLong rolledBackId = new AtomicLong();
            var business = new TransactionTemplate(new DataSourceTransactionManager(source));
            assertThrows(IllegalStateException.class, () -> business.execute(status -> {
                rolledBackId.set(first.allocateLinkBatch());
                throw new IllegalStateException("Rollback business transaction");
            }));
            assertTrue(second.allocateLinkBatch() > rolledBackId.get());
        } finally {
            workers.shutdownNow();
        }
    }

    private void migrate() throws Exception {
        try (Connection connection = source.getConnection()) {
            new V2026_11_09_00__identity_indexes_and_link_batch_sequence().migrate(new Context() {
                public Configuration getConfiguration() { return null; }
                public Connection getConnection() { return connection; }
            });
        }
    }
}
