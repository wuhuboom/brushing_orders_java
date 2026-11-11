package com.order.member.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.order.framework.config.DruidConfig;
import com.order.framework.datasource.DynamicDataSource;
import com.order.member.domain.dto.TransactionDto;
import com.order.member.mapper.*;
import com.order.member.service.impl.*;
import com.order.web.controller.member.OrderUserController;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/** Real money service, Spring transaction proxy, MyBatis SQL and Druid pools; no production access. */
class AdminTransactionRealMoneyMySqlTest {
    private DruidDataSource business, sequences;
    private DynamicDataSource dynamic;
    private JdbcTemplate jdbc;
    private AnnotationConfigApplicationContext context;
    private AdminTransactionRequestService requests;
    private SiteMessageNotificationService notification;
    private ExecutorService workers;

    @Configuration @EnableTransactionManagement
    static class Transactions {}

    @BeforeEach void setup() throws Exception {
        String url = System.getProperty("admin.transaction.jdbcUrl");
        assumeTrue(url != null, "Requires an explicitly selected isolated local test database");
        URI endpoint = URI.create(url.substring("jdbc:".length()));
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals("/codex_admin_transaction_test", endpoint.getPath());
        assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
        assertTrue(endpoint.getPort() > 0); assertNull(endpoint.getRawQuery()); assertNull(endpoint.getRawFragment());
        business = new DruidDataSource(); business.setUrl(url); business.setUsername("root"); business.setPassword("");
        business.setMaxActive(12); business.setMaxWait(3000); business.setSocketTimeout(7000);
        business.setConnectionInitSqls(List.of("SET SESSION innodb_lock_wait_timeout=3"));
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(url); properties.setUsername("root"); properties.setPassword("");
        sequences = new DruidConfig().sequenceDataSource(properties);
        assertEquals(2, sequences.getMaxActive());
        dynamic = new DynamicDataSource(business, Map.of("MASTER", business));
        jdbc = new JdbcTemplate(dynamic);
        assertEquals("codex_admin_transaction_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        createSchema();
        jdbc.update("INSERT INTO order_user(id, username, vip_id, parent_id, password, balance) VALUES (7,'test-seven',0,0,'test',100),(8,'test-eight',0,0,'test',100)");
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dynamic); factory.setTypeAliasesPackage("com.order.member.domain");
        factory.setMapperLocations(new ClassPathResource("mapper/OrderUserMapper.xml"),
                new ClassPathResource("mapper/GoodsRechargeRecordMapper.xml"),
                new ClassPathResource("mapper/GoodsTransactionFlowMapper.xml"),
                new ClassPathResource("mapper/OrderSequenceManagerMapper.xml"));
        SqlSessionTemplate sessions = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        context = new AnnotationConfigApplicationContext();
        context.register(Transactions.class);
        context.registerBean("dynamicDataSource", DataSource.class, () -> dynamic);
        context.registerBean("sequenceDataSource", DataSource.class, () -> sequences);
        context.registerBean("transactionManager", DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(dynamic));
        context.registerBean(OrderUserMapper.class, () -> sessions.getMapper(OrderUserMapper.class));
        context.registerBean(GoodsRechargeRecordMapper.class, () -> sessions.getMapper(GoodsRechargeRecordMapper.class));
        context.registerBean(GoodsTransactionFlowMapper.class, () -> sessions.getMapper(GoodsTransactionFlowMapper.class));
        context.registerBean(OrderSequenceManagerMapper.class, () -> sessions.getMapper(OrderSequenceManagerMapper.class));
        notification = mock(SiteMessageNotificationService.class);
        context.registerBean(SiteMessageNotificationService.class, () -> notification);
        context.registerBean(IOrderConfigService.class, () -> mock(IOrderConfigService.class));
        context.register(SequenceAllocator.class, OrderSequenceManagerServiceImpl.class,
                GoodsRechargeRecordServiceImpl.class, GoodsTransactionFlowServiceImpl.class,
                TransactionServiceImpl.class, AdminTransactionRequestService.class);
        context.refresh();
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(context.getBean(ITransactionService.class)));
        requests = context.getBean(AdminTransactionRequestService.class);
        workers = Executors.newFixedThreadPool(10);
    }

    private void createSchema() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("sql/init/order_base_20260803.sql"))) root = root.getParent();
        assertNotNull(root, "Must run inside the backend checkout");
        String baseline = Files.readString(root.resolve("sql/init/order_base_20260803.sql"));
        jdbc.execute("DROP TABLE IF EXISTS admin_transaction_request");
        // Member-level DDL retains its real foreign key to translations.
        Matcher translations = Pattern.compile("CREATE TABLE `translations` \\(.*?;", Pattern.DOTALL).matcher(baseline);
        assertTrue(translations.find());
        jdbc.execute(translations.group().replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS"));
        for (String table : List.of("order_user", "goods_member_level", "order_login_log", "order_withdrawal", "goods_recharge_record", "goods_transaction_flow", "order_sequence_manager")) {
            jdbc.execute("DROP TABLE IF EXISTS `" + table + "`");
            Matcher ddl = Pattern.compile("CREATE TABLE `" + table + "` \\(.*?;", Pattern.DOTALL).matcher(baseline);
            assertTrue(ddl.find(), "Missing baseline DDL: " + table);
            jdbc.execute(ddl.group());
        }
        // Build the legacy snapshot forward with the same migrations used by the application.
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/mysql/V2026_10_27_00__admin_transaction_requests.sql"),
                new ClassPathResource("db/migration/mysql/V2026_11_08_00__member_product_match_ratios.sql"))
                .execute(dynamic);
        // Current member queries read the stored relative copy added in release 1.2.6.
        jdbc.execute("ALTER TABLE goods_member_level ADD COLUMN description_relative LONGTEXT NULL");
    }

    @AfterEach void cleanup() {
        if (workers != null) workers.shutdownNow();
        if (context != null) context.close();
        if (business != null) business.close();
        if (sequences != null) sequences.close();
    }
    private TransactionDto intent(String key, long user, int operation, String amount, String gift) {
        TransactionDto dto = new TransactionDto(); dto.setRequestId(key); dto.setUserId(user);
        dto.setOperationType(operation); dto.setAmount(new BigDecimal(amount)); dto.setGiftAmount(new BigDecimal(gift));
        dto.setTransactionType(operation == 0 ? "ck" : "kk"); return dto;
    }
    private BigDecimal balance(long user) { return jdbc.queryForObject("SELECT balance FROM order_user WHERE id=?", BigDecimal.class, user); }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private void success(TransactionDto dto) { assertEquals("SUCCEEDED", requests.submit(1L, dto).state()); }

    @Test void flywayUpgradeRequiresNewReleaseVersionAndRestartPreservesRequestHistory(@org.junit.jupiter.api.io.TempDir Path migrations) throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS admin_transaction_request");
        jdbc.execute("DROP TABLE IF EXISTS admin_transaction_test_history");
        jdbc.execute("DROP TABLE IF EXISTS sys_application_version");
        String migration = "V2026_10_27_00__admin_transaction_requests.sql";
        try (var input = new ClassPathResource("db/migration/mysql/" + migration).getInputStream()) {
            Files.copy(input, migrations.resolve(migration));
        }
        org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                .dataSource(dynamic).locations("filesystem:" + migrations)
                .table("admin_transaction_test_history").baselineVersion("2026.09.01.0").load();
        flyway.baseline();
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/mysql/V2026_08_18_00__application_version.sql")).execute(dynamic);
        jdbc.update("INSERT INTO sys_application_version(id,application_version,schema_version) VALUES (1,'1.2.2','2026.09.01.0')");
        try {
            assertThrows(IllegalStateException.class, () -> new com.order.framework.migration.ControlledFlywayMigrationStrategy("1.2.2").migrate(flyway));
            var upgrade = new com.order.framework.migration.ControlledFlywayMigrationStrategy("1.2.3");
            upgrade.migrate(flyway);
            assertEquals("1.2.3", jdbc.queryForObject("SELECT application_version FROM sys_application_version WHERE id=1",String.class));
            TransactionDto dto = intent("real-migration-1",7,0,"5","0");
            success(dto);
            upgrade.migrate(flyway);
            success(dto);
            assertEquals(new BigDecimal("105.00"),balance(7));
            assertEquals(1,count("admin_transaction_request"));
            assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM admin_transaction_test_history WHERE type='SQL' AND success=1",Long.class));
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS admin_transaction_test_history");
            jdbc.execute("DROP TABLE IF EXISTS sys_application_version");
        }
    }

    @Test void eightConcurrentReplaysWriteOneRechargeAndTwoLinkedFlows() throws Exception {
        TransactionDto dto = intent("real-same-request",7,0,"405","7.50");
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        for (int i=0;i<8;i++) tasks.add(workers.submit(() -> { start.await(); success(dto); return null; }));
        start.countDown(); for (Future<?> task : tasks) task.get(15, TimeUnit.SECONDS);
        success(dto);
        assertEquals(new BigDecimal("512.50"), balance(7));
        assertEquals(1, count("goods_recharge_record")); assertEquals(2, count("goods_transaction_flow"));
        assertEquals(1, count("admin_transaction_request"));
        assertEquals(new BigDecimal("412.50"), jdbc.queryForObject("SELECT received_amount FROM goods_recharge_record",BigDecimal.class));
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM goods_transaction_flow f JOIN goods_recharge_record r ON f.transaction_code=r.order_number",Long.class));
        assertEquals(List.of(new BigDecimal("100.00"),new BigDecimal("505.00")),jdbc.queryForList("SELECT balance_before FROM goods_transaction_flow ORDER BY id",BigDecimal.class));
        assertEquals(List.of(new BigDecimal("505.00"),new BigDecimal("512.50")),jdbc.queryForList("SELECT balance_after FROM goods_transaction_flow ORDER BY id",BigDecimal.class));
    }

    @Test void eightConcurrentGiftControllerRetriesCreditOnceAndRejectChangedAmount() throws Exception {
        OrderUserController controller = new OrderUserController() {
            @Override public Long getUserId() { return 1L; }
        };
        ReflectionTestUtils.setField(controller, "adminTransactionRequests", requests);
        TransactionDto gift = intent("real-gift-request-01", 7, 1, "12.50", "999");
        CountDownLatch ready = new CountDownLatch(8), start = new CountDownLatch(1);
        List<Future<AdminTransactionRequestService.Result>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(workers.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                var response = controller.giftAmount(gift);
                assertEquals(200, response.get("code"));
                return assertInstanceOf(AdminTransactionRequestService.Result.class, response.get("data"));
            }));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        var expected = tasks.get(0).get(15, TimeUnit.SECONDS);
        assertEquals(gift.getRequestId(), expected.requestId());
        assertEquals("SUCCEEDED", expected.state());
        for (var task : tasks) assertEquals(expected, task.get(15, TimeUnit.SECONDS));
        assertEquals(expected, controller.giftAmount(gift).get("data"));
        assertEquals(expected, requests.status(1L, gift.getRequestId()));

        gift.setAmount(new BigDecimal("13.50"));
        assertThrows(IllegalArgumentException.class, () -> controller.giftAmount(gift));
        assertEquals(new BigDecimal("112.50"), balance(7));
        assertEquals(1, count("admin_transaction_request"));
        assertEquals(1, count("goods_recharge_record"));
        assertEquals(1, count("goods_transaction_flow"));
        assertEquals(new BigDecimal("12.50"), jdbc.queryForObject(
                "SELECT received_amount FROM goods_recharge_record", BigDecimal.class));
        assertEquals(1L, jdbc.queryForObject("""
                SELECT COUNT(*) FROM goods_transaction_flow f
                JOIN goods_recharge_record r ON f.transaction_code = r.order_number
                WHERE f.user_id = 7 AND f.transaction_type = 'zs'
                  AND r.transaction_type = 'zs' AND r.gift_amount = 0
                  AND f.transaction_amount = 12.50
                  AND f.balance_before = 100 AND f.balance_after = 112.50
                """, Long.class));
    }

    @Test void lockFailureAfterMoneyWritesRollsBackBeforeRetryingTheSameRequest() {
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            // The actual transaction has written money before the simulated lock failure.
            // Seeing the same balance/ledger on the next attempt proves the first was rolled back.
            assertEquals(new BigDecimal("112.50"), balance(7));
            assertEquals(1, count("goods_recharge_record"));
            assertEquals(1, count("goods_transaction_flow"));
            assertEquals(new BigDecimal("100.00"), jdbc.queryForObject(
                    "SELECT balance_before FROM goods_transaction_flow", BigDecimal.class));
            if (attempts.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("simulated lock failure after real money writes");
            }
            return null;
        }).when(notification).createForTransaction(anyLong(), anyString(), any(), any(), any());
        TransactionDto dto = intent("real-retry-after-writes", 7, 0, "12.50", "0");

        var result = requests.submit(1L, dto);

        assertEquals("SUCCEEDED", result.state());
        assertEquals(2, attempts.get());
        assertEquals(result, requests.status(1L, dto.getRequestId()));
        assertEquals(result, requests.submit(1L, dto));
        assertEquals(2, attempts.get(), "A committed replay must not execute money again");
        assertEquals(new BigDecimal("112.50"), balance(7));
        assertEquals(1, count("admin_transaction_request"));
        assertEquals(1, count("goods_recharge_record"));
        assertEquals(1, count("goods_transaction_flow"));
    }

    @Test void exhaustedLockRetriesLeaveNoFailedResultAndOriginalRequestCanStillSucceed() {
        doThrow(new CannotAcquireLockException("simulated persistent lock conflict"))
                .when(notification).createForTransaction(anyLong(), anyString(), any(), any(), any());
        TransactionDto dto = intent("real-retry-unconfirmed", 7, 0, "12.50", "0");

        assertThrows(CannotAcquireLockException.class, () -> requests.submit(1L, dto));

        verify(notification, times(5)).createForTransaction(anyLong(), anyString(), any(), any(), any());
        assertEquals("UNKNOWN", requests.status(1L, dto.getRequestId()).state());
        assertEquals(new BigDecimal("100.00"), balance(7));
        assertEquals(0, count("admin_transaction_request"));
        assertEquals(0, count("goods_recharge_record"));
        assertEquals(0, count("goods_transaction_flow"));

        reset(notification);
        var result = requests.submit(1L, dto);
        assertEquals("SUCCEEDED", result.state());
        assertEquals(result, requests.submit(1L, dto));
        verify(notification, times(1)).createForTransaction(anyLong(), anyString(), any(), any(), any());
        assertEquals(new BigDecimal("112.50"), balance(7));
        assertEquals(1, count("admin_transaction_request"));
        assertEquals(1, count("goods_recharge_record"));
        assertEquals(1, count("goods_transaction_flow"));
    }

    @Test void lockFailureWhileSavingBusinessRejectionRetriesOnlyTheFailedResult() {
        TransactionDto dto = intent("real-failed-save-retry", 7, 0, "12.50", "0");
        doThrow(new IllegalStateException("simulated business rejection after money writes"))
                .when(notification).createForTransaction(anyLong(), anyString(), any(), any(), any());
        JdbcTemplate requestJdbc = spy(new JdbcTemplate(dynamic));
        AtomicInteger failureSaves = new AtomicInteger();
        doAnswer(invocation -> {
            if (failureSaves.incrementAndGet() == 1) {
                throw new CannotAcquireLockException("simulated lock failure while saving rejection");
            }
            return invocation.callRealMethod();
        }).when(requestJdbc).update(contains("UPDATE admin_transaction_request"),
                eq("FAILED"), anyString(), eq(1L), eq(dto.getRequestId()));
        ReflectionTestUtils.setField(requests, "jdbc", requestJdbc);

        var result = requests.submit(1L, dto);

        assertEquals("FAILED", result.state());
        assertEquals(2, failureSaves.get());
        assertEquals(result, requests.status(1L, dto.getRequestId()));
        assertEquals(result, requests.submit(1L, dto));
        verify(notification, times(1)).createForTransaction(anyLong(), anyString(), any(), any(), any());
        assertEquals(new BigDecimal("100.00"), balance(7));
        assertEquals(1, count("admin_transaction_request"));
        assertEquals(0, count("goods_recharge_record"));
        assertEquals(0, count("goods_transaction_flow"));
    }

    @Test void distinctConcurrentCreditsAndDebitsKeepAnUnbrokenBalanceLedger() throws Exception {
        CountDownLatch start = new CountDownLatch(1); List<Future<?>> tasks = new ArrayList<>();
        for (int i=0;i<20;i++) {
            final int n=i;
            tasks.add(workers.submit(() -> { start.await(); success(intent("real-mixed-req-"+String.format("%03d",n),7,n%2,n%2==0?"1":"2","0")); return null; }));
        }
        start.countDown(); for (Future<?> task : tasks) task.get(20,TimeUnit.SECONDS);
        assertEquals(new BigDecimal("90.00"),balance(7)); assertEquals(20,count("goods_transaction_flow"));
        assertEquals(10,count("goods_recharge_record")); assertEquals(20,count("admin_transaction_request"));
        BigDecimal previous = new BigDecimal("100.00");
        for (Map<String,Object> flow : jdbc.queryForList("SELECT balance_before,transaction_amount,balance_after FROM goods_transaction_flow ORDER BY id")) {
            assertEquals(previous, flow.get("balance_before"));
            previous = previous.add((BigDecimal)flow.get("transaction_amount")); assertEquals(previous, flow.get("balance_after"));
        }
        assertEquals(balance(7), previous);
    }

    @Test void databaseFailureAfterRechargeAndFlowsRollsBackAllMoneyButKeepsFailedResult() {
        jdbc.execute("CREATE TRIGGER reject_test_credit BEFORE UPDATE ON order_user FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='simulated balance write failure'");
        TransactionDto dto=intent("real-rollback-01",7,0,"405","7.50");
        assertEquals("FAILED",requests.submit(1L,dto).state());
        assertEquals("FAILED",requests.status(1L,dto.getRequestId()).state());
        assertEquals(new BigDecimal("100.00"),balance(7));
        assertEquals(0,count("goods_recharge_record")); assertEquals(0,count("goods_transaction_flow"));
        assertTrue(count("order_sequence_manager")>0,"Committed sequence numbers survive business rollback");
        jdbc.execute("DROP TRIGGER reject_test_credit");
        assertEquals("FAILED",requests.submit(1L,dto).state()); assertEquals(new BigDecimal("100.00"),balance(7));
    }

    @Test void insufficientDebitNeverWritesAFlowOrChangesBalance() {
        TransactionDto dto=intent("real-insufficient",7,1,"101","0");
        assertEquals("FAILED",requests.submit(1L,dto).state());
        assertEquals(new BigDecimal("100.00"),balance(7)); assertEquals(0,count("goods_transaction_flow"));
        assertEquals(0,count("goods_recharge_record"));
    }

    @Test void concurrentUserOperationReleasesRowThenAdminUsesTheNewBalance() throws Exception {
        CountDownLatch held=new CountDownLatch(1), release=new CountDownLatch(1);
        Future<?> userOperation=workers.submit(() -> new TransactionTemplate(new DataSourceTransactionManager(dynamic)).execute(s -> {
            jdbc.update("UPDATE order_user SET balance=balance-20 WHERE id=7"); held.countDown();
            try { assertTrue(release.await(5,TimeUnit.SECONDS)); } catch (InterruptedException e) {throw new RuntimeException(e);} return null;
        }));
        assertTrue(held.await(3,TimeUnit.SECONDS));
        TransactionDto dto=intent("real-user-admin-1",7,0,"5","0");
        Future<?> admin=workers.submit(() -> success(dto));
        try {
            // Verify an actual InnoDB wait, rather than accidentally exercising sequential calls.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            boolean waiting = false;
            while (System.nanoTime() < deadline) {
                Long waits = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM performance_schema.data_lock_waits w
                        JOIN performance_schema.data_locks l
                          ON l.ENGINE=w.ENGINE AND l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID
                        WHERE l.OBJECT_SCHEMA=DATABASE() AND l.OBJECT_NAME='order_user'
                        """, Long.class);
                if (waits != null && waits > 0) { waiting = true; break; }
                Thread.sleep(10);
            }
            assertTrue(waiting, "Administrator transaction must actually wait on the user's row lock");
            assertFalse(admin.isDone());
            assertEquals("UNKNOWN",requests.status(1L,dto.getRequestId()).state());
        } finally { release.countDown(); }
        userOperation.get(10,TimeUnit.SECONDS); admin.get(10,TimeUnit.SECONDS);
        assertEquals(new BigDecimal("85.00"),balance(7));
        assertEquals(new BigDecimal("80.00"),jdbc.queryForObject("SELECT balance_before FROM goods_transaction_flow",BigDecimal.class));
    }

    @Test void otherUserCompletesWhileFirstBusinessTransactionStillHoldsItsUserLock() throws Exception {
        CountDownLatch firstHeld=new CountDownLatch(1),release=new CountDownLatch(1);
        doAnswer(invocation -> {
            if (Long.valueOf(7).equals(invocation.getArgument(0))) {firstHeld.countDown(); assertTrue(release.await(5,TimeUnit.SECONDS));} return null;
        }).when(notification).createForTransaction(anyLong(),anyString(),any(),any(),any());
        Future<?> first=workers.submit(() -> success(intent("real-user-seven-1",7,0,"1","0")));
        assertTrue(firstHeld.await(3,TimeUnit.SECONDS));
        try {
            Future<?> other=workers.submit(() -> success(intent("real-user-eight-1",8,0,"2","0")));
            other.get(3,TimeUnit.SECONDS);
            assertFalse(first.isDone()); assertEquals(new BigDecimal("102.00"),balance(8));
        } finally {release.countDown();}
        first.get(10,TimeUnit.SECONDS); assertEquals(new BigDecimal("101.00"),balance(7));
    }
}
