package com.order.api.service;

import com.order.member.domain.Goods;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderInfo;
import com.order.member.domain.OrderLink;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsMapper;
import com.order.member.mapper.OrderApiRequestMapper;
import com.order.member.mapper.OrderBonusTableMapper;
import com.order.member.mapper.OrderInfoMapper;
import com.order.member.mapper.OrderLinkMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.ITransactionService;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class OrderConcurrencyMySqlTest {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42")
            .withDatabaseName("order_api_test")
            .withUsername("order")
            .withPassword("order");

    @BeforeEach
    void createLegacySchemaAndMigrate() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS order_api_request");
            statement.execute("DROP TABLE IF EXISTS order_info");
            statement.execute("DROP TABLE IF EXISTS order_link");
            statement.execute("DROP TABLE IF EXISTS order_bonus_table");
            statement.execute("DROP TABLE IF EXISTS goods_transaction_flow");
            statement.execute("DROP TABLE IF EXISTS order_user");
            statement.execute("DROP TABLE IF EXISTS goods");
            statement.execute("""
                    CREATE TABLE order_user (
                        id BIGINT PRIMARY KEY,
                        balance DECIMAL(20,2) NOT NULL,
                        frozen_balance DECIMAL(20,2) NOT NULL DEFAULT 0,
                        task_progress BIGINT NOT NULL DEFAULT 0,
                        version BIGINT NOT NULL DEFAULT 0,
                        update_time DATETIME(3),
                        remarks VARCHAR(500)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods (
                        id BIGINT PRIMARY KEY,
                        title VARCHAR(300),
                        image VARCHAR(255),
                        is_enabled CHAR(1) NOT NULL,
                        price DECIMAL(20,2) NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE order_info (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_number VARCHAR(64),
                        user_id BIGINT NOT NULL,
                        status CHAR(1) NOT NULL,
                        amount DECIMAL(20,2) NOT NULL,
                        rebate DECIMAL(20,2) NOT NULL DEFAULT 0,
                        product_id BIGINT,
                        link_id BIGINT,
                        remarks VARCHAR(500),
                        comment_id BIGINT,
                        create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE order_link (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        link_order_id BIGINT,
                        user_id BIGINT NOT NULL,
                        order_count BIGINT NOT NULL,
                        commission_multiple INT,
                        product_id BIGINT,
                        price_type CHAR(1),
                        price DECIMAL(20,2),
                        status CHAR(1) NOT NULL,
                        create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE order_bonus_table (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        amount DECIMAL(20,2) NOT NULL,
                        is_received CHAR(1) NOT NULL,
                        is_distributed CHAR(1) NOT NULL,
                        distribution_type CHAR(1) NOT NULL,
                        order_num BIGINT,
                        expiry_time DATETIME(3),
                        received_time DATETIME(3),
                        create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE goods_transaction_flow (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        transaction_type VARCHAR(30) NOT NULL,
                        transaction_amount DECIMAL(20,2) NOT NULL,
                        balance_before DECIMAL(20,2) NOT NULL,
                        balance_after DECIMAL(20,2) NOT NULL,
                        remark VARCHAR(255)
                    ) ENGINE=InnoDB
                    """);
        }
        executeScript(findMigration());
    }

    @Test
    void migrationIsRepeatableAndSnapshotsOrderHistory() throws Exception {
        execute("INSERT INTO goods VALUES (31, 'Original', '/original.jpg', '0', 30)");
        execute("""
                INSERT INTO order_info(order_number, user_id, status, amount, rebate, product_id)
                VALUES ('O-HISTORY', 7, '0', 30, 0.3, 31)
                """);

        executeScript(findMigration());
        execute("UPDATE goods SET title = 'Changed', image = '/changed.jpg' WHERE id = 31");

        assertEquals("Original", queryString(
                "SELECT product_title FROM order_info WHERE order_number = 'O-HISTORY'"));
        assertEquals("/original.jpg", queryString(
                "SELECT product_image FROM order_info WHERE order_number = 'O-HISTORY'"));
        assertEquals(2L, queryLong("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'order_info'
                  AND index_name IN (
                    'uk_order_info_order_number',
                    'uk_order_info_pending_user'
                  )
                """));
        assertEquals(1L, queryLong("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'order_bonus_table'
                  AND index_name = 'uk_order_bonus_claimable_exact'
                """));
        assertEquals(1L, queryLong("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'order_api_request'
                  AND index_name = 'uk_order_api_request_key'
                """));
    }

    @Test
    void orderListAndGoodsPaginationPlansUseExpectedIndexes() throws Exception {
        execute("""
                INSERT INTO order_info(
                    order_number, user_id, status, amount, rebate, create_time
                ) VALUES ('PLAN-1', 7, '1', 10, 0, NOW(3))
                """);
        execute("""
                INSERT INTO goods(id, title, image, is_enabled, price)
                VALUES (31, 'Plan goods', '/plan.jpg', '0', 30)
                """);

        String orderPlan = queryString("""
                EXPLAIN ANALYZE
                SELECT id
                FROM order_info
                    FORCE INDEX (idx_order_info_user_status_created)
                WHERE user_id = 7 AND status = '1'
                ORDER BY create_time DESC, id DESC
                LIMIT 20
                """);
        assertTrue(orderPlan.contains("idx_order_info_user_status_created"));

        String goodsPlan = queryString("""
                EXPLAIN ANALYZE
                SELECT id
                FROM goods FORCE INDEX (PRIMARY)
                ORDER BY id
                LIMIT 20
                """);
        assertTrue(goodsPlan.contains("PRIMARY"));
    }

    @Test
    void migrationStopsWhenLegacyDataHasDuplicatePendingOrders() throws Exception {
        execute("ALTER TABLE order_info DROP INDEX uk_order_info_pending_user");
        execute("ALTER TABLE order_info DROP COLUMN pending_user_id");
        execute("""
                INSERT INTO order_info(order_number, user_id, status, amount, rebate)
                VALUES ('DUP-1', 7, '1', 10, 0), ('DUP-2', 7, '1', 20, 0)
                """);

        assertThrows(SQLException.class, () -> executeScript(findMigration()));
    }

    @Test
    void migrationStopsWhenLegacyDataHasDuplicateClaimableOrderBonuses()
            throws Exception {
        execute("ALTER TABLE order_bonus_table DROP INDEX uk_order_bonus_claimable_exact");
        execute("""
                INSERT INTO order_bonus_table(
                    user_id, amount, is_received, is_distributed,
                    distribution_type, order_num
                ) VALUES
                    (7, 10, '1', '1', '0', 3),
                    (7, 20, '1', '1', '0', 3)
                """);

        assertThrows(SQLException.class, () -> executeScript(findMigration()));
    }

    @Test
    void idempotencyKeyCannotBeReboundToASecondOrder() throws Exception {
        execute("""
                INSERT INTO order_api_request(
                    user_id, operation_type, request_id, result_type, result_id
                ) VALUES (
                    7, 'CREATE_ORDER', 'request-12345678', 'ORDER', 20
                )
                """);

        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO order_api_request(
                    user_id, operation_type, request_id, result_type, result_id
                ) VALUES (
                    7, 'CREATE_ORDER', 'request-12345678', 'ORDER', 21
                )
                """));
        assertEquals(20L, queryLong("""
                SELECT result_id
                FROM order_api_request
                WHERE user_id = 7
                  AND operation_type = 'CREATE_ORDER'
                  AND request_id = 'request-12345678'
                """));
    }

    @Test
    void createRollsBackOrderReservationProgressAndFlowWhenFlowPersistenceFails()
            throws Exception {
        execute("""
                INSERT INTO order_user(id, balance, frozen_balance, task_progress)
                VALUES (7, 100, 0, 0)
                """);
        execute("INSERT INTO goods VALUES (31, 'Product', '/product.jpg', '0', 30)");

        try (RollbackHarness harness = rollbackHarness()) {
            when(harness.userMapper.lockUserById(7L)).thenAnswer(invocation ->
                    harness.jdbc.queryForObject(
                            "SELECT id FROM order_user WHERE id = 7 FOR UPDATE",
                            Long.class));
            when(harness.userMapper.selectOrderTaskUserById(7L))
                    .thenReturn(taskUser(new BigDecimal("100.00"), BigDecimal.ZERO, 0L));
            when(harness.orderMapper.hasOpenOrders(7L)).thenReturn(null);
            when(harness.policyService.activePolicy()).thenReturn(activePolicy());
            when(harness.goodsMapper.selectNearestPriceGoods(any(BigDecimal.class)))
                    .thenReturn(goods(31L, new BigDecimal("30.00")));
            when(harness.sequenceService.generateCode("TRADE_NO"))
                    .thenReturn("O-ROLLBACK-CREATE");
            when(harness.orderMapper.insertOrderInfo(any(OrderInfo.class)))
                    .thenAnswer(invocation -> {
                        OrderInfo order = invocation.getArgument(0);
                        return harness.jdbc.update("""
                                INSERT INTO order_info(
                                    order_number, user_id, status, amount, rebate,
                                    product_id, product_title, product_image
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                """,
                                order.getOrderNumber(),
                                order.getUserId(),
                                order.getStatus(),
                                order.getAmount(),
                                order.getRebate(),
                                order.getProductId(),
                                order.getProductTitle(),
                                order.getProductImage());
                    });
            when(harness.userMapper.reserveOrderFunds(
                    anyLong(), any(BigDecimal.class), anyLong(), anyBoolean()))
                    .thenAnswer(invocation -> {
                        Long userId = invocation.getArgument(0);
                        BigDecimal amount = invocation.getArgument(1);
                        Long progress = invocation.getArgument(2);
                        Boolean allowNegative = invocation.getArgument(3);
                        String sql = allowNegative
                                ? """
                                  UPDATE order_user
                                  SET balance = balance - ?,
                                      frozen_balance = frozen_balance + ?,
                                      task_progress = task_progress + ?
                                  WHERE id = ?
                                  """
                                : """
                                  UPDATE order_user
                                  SET balance = balance - ?,
                                      frozen_balance = frozen_balance + ?,
                                      task_progress = task_progress + ?
                                  WHERE id = ? AND balance >= ?
                                  """;
                        return allowNegative
                                ? harness.jdbc.update(sql, amount, amount, progress, userId)
                                : harness.jdbc.update(
                                        sql, amount, amount, progress, userId, amount);
                    });
            failFlowAfterInsert(harness);

            assertTrue(AopUtils.isAopProxy(harness.service));
            assertThrows(IllegalStateException.class, () -> harness.service.create(7L));
        }

        assertEquals(0L, queryLong(
                "SELECT COUNT(*) FROM order_info WHERE order_number = 'O-ROLLBACK-CREATE'"));
        assertEquals(new BigDecimal("100.00"),
                queryDecimal("SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("0.00"),
                queryDecimal("SELECT frozen_balance FROM order_user WHERE id = 7"));
        assertEquals(0L, queryLong("SELECT task_progress FROM order_user WHERE id = 7"));
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM goods_transaction_flow"));
    }

    @Test
    void submitRollsBackStatusSettlementAndFlowWhenFlowPersistenceFails()
            throws Exception {
        execute("""
                INSERT INTO order_user(id, balance, frozen_balance, task_progress)
                VALUES (7, 70, 30, 1)
                """);
        execute("""
                INSERT INTO order_info(id, order_number, user_id, status, amount, rebate)
                VALUES (20, 'O-ROLLBACK-SUBMIT', 7, '1', 30, 0.30)
                """);

        try (RollbackHarness harness = rollbackHarness()) {
            when(harness.userMapper.lockUserById(7L)).thenAnswer(invocation ->
                    harness.jdbc.queryForObject(
                            "SELECT id FROM order_user WHERE id = 7 FOR UPDATE",
                            Long.class));
            when(harness.orderMapper.selectOwnedOrderForUpdate(20L, 7L))
                    .thenAnswer(invocation -> {
                        harness.jdbc.queryForObject("""
                                SELECT id FROM order_info
                                WHERE id = 20 AND user_id = 7 FOR UPDATE
                                """, Long.class);
                        return pendingOrder();
                    });
            when(harness.policyService.activePolicy()).thenReturn(activePolicy());
            when(harness.userMapper.selectOrderBalanceById(7L))
                    .thenReturn(balanceUser(
                            new BigDecimal("70.00"), new BigDecimal("30.00")));
            when(harness.orderMapper.transitionStatus(20L, 7L, "1", "0"))
                    .thenAnswer(invocation -> harness.jdbc.update("""
                            UPDATE order_info SET status = '0'
                            WHERE id = 20 AND user_id = 7 AND status = '1'
                            """));
            when(harness.userMapper.settleOrderFunds(
                    7L, new BigDecimal("30.00"), new BigDecimal("0.30")))
                    .thenAnswer(invocation -> harness.jdbc.update("""
                            UPDATE order_user
                            SET balance = balance + ? + ?,
                                frozen_balance = frozen_balance - ?
                            WHERE id = ? AND frozen_balance >= ?
                            """,
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(1),
                            invocation.getArgument(0),
                            invocation.getArgument(1)));
            failFlowAfterInsert(harness);

            assertTrue(AopUtils.isAopProxy(harness.service));
            assertThrows(
                    IllegalStateException.class,
                    () -> harness.service.submit(7L, 20L));
        }

        assertEquals("1", queryString("SELECT status FROM order_info WHERE id = 20"));
        assertEquals(new BigDecimal("70.00"),
                queryDecimal("SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("30.00"),
                queryDecimal("SELECT frozen_balance FROM order_user WHERE id = 7"));
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM goods_transaction_flow"));
    }

    @Test
    void bonusClaimRollsBackClaimBalanceAndFlowWhenFlowPersistenceFails()
            throws Exception {
        execute("INSERT INTO order_user(id, balance, frozen_balance) VALUES (7, 100, 0)");
        execute("""
                INSERT INTO order_bonus_table(
                    id, user_id, amount, is_received, is_distributed, distribution_type
                ) VALUES (88, 7, 28.88, '1', '1', '0')
                """);

        try (RollbackHarness harness = rollbackHarness()) {
            when(harness.bonusMapper.selectOwnedBonusForUpdate(88L, 7L))
                    .thenAnswer(invocation -> {
                        harness.jdbc.queryForObject("""
                                SELECT id FROM order_bonus_table
                                WHERE id = 88 AND user_id = 7 FOR UPDATE
                                """, Long.class);
                        OrderBonusTable bonus = new OrderBonusTable();
                        bonus.setId(88L);
                        bonus.setUserId(7L);
                        bonus.setAmount(new BigDecimal("28.88"));
                        bonus.setIsReceived("1");
                        bonus.setIsDistributed("1");
                        return bonus;
                    });
            when(harness.userMapper.lockUserById(7L)).thenAnswer(invocation ->
                    harness.jdbc.queryForObject(
                            "SELECT id FROM order_user WHERE id = 7 FOR UPDATE",
                            Long.class));
            when(harness.userMapper.selectOrderBalanceById(7L))
                    .thenReturn(balanceUser(new BigDecimal("100.00"), BigDecimal.ZERO));
            when(harness.bonusMapper.claimBonus(88L, 7L))
                    .thenAnswer(invocation -> harness.jdbc.update("""
                            UPDATE order_bonus_table
                            SET is_received = '0', received_time = NOW(3)
                            WHERE id = 88 AND user_id = 7
                              AND is_received = '1' AND is_distributed = '1'
                            """));
            when(harness.userMapper.creditBalance(7L, new BigDecimal("28.88")))
                    .thenAnswer(invocation -> {
                        Long userId = invocation.getArgument(0);
                        BigDecimal amount = invocation.getArgument(1);
                        return harness.jdbc.update("""
                                UPDATE order_user SET balance = balance + ?
                                WHERE id = ?
                                """, amount, userId);
                    });
            failFlowAfterInsert(harness);

            assertTrue(AopUtils.isAopProxy(harness.service));
            assertThrows(
                    IllegalStateException.class,
                    () -> harness.service.claimBonus(7L, 88L));
        }

        assertEquals("1", queryString(
                "SELECT is_received FROM order_bonus_table WHERE id = 88"));
        assertEquals(new BigDecimal("100.00"),
                queryDecimal("SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(0L, queryLong("SELECT COUNT(*) FROM goods_transaction_flow"));
    }

    @Test
    void concurrentCreateProducesOnePendingOrderAndOneReservation() throws Exception {
        execute("INSERT INTO order_user(id, balance, frozen_balance) VALUES (7, 100, 0)");

        List<Boolean> results = concurrently(
                () -> createOrRecover("O-C1", new BigDecimal("30.00")),
                () -> createOrRecover("O-C2", new BigDecimal("30.00")));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1L, queryLong(
                "SELECT COUNT(*) FROM order_info WHERE user_id = 7 AND status = '1'"));
        assertEquals(new BigDecimal("70.00"), queryDecimal(
                "SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("30.00"), queryDecimal(
                "SELECT frozen_balance FROM order_user WHERE id = 7"));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM goods_transaction_flow
                WHERE user_id = 7 AND transaction_type = 'rw'
                """));
    }

    @Test
    void concurrentSubmitSettlesPrincipalAndRebateOnce() throws Exception {
        execute("""
                INSERT INTO order_user(id, balance, frozen_balance, task_progress)
                VALUES (7, 70, 30, 1)
                """);
        execute("""
                INSERT INTO order_info(id, order_number, user_id, status, amount, rebate)
                VALUES (20, 'O-20', 7, '1', 30, 0.30)
                """);

        List<Boolean> results = concurrently(
                () -> submit(20L), () -> submit(20L));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals("0", queryString("SELECT status FROM order_info WHERE id = 20"));
        assertEquals(new BigDecimal("100.30"), queryDecimal(
                "SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("0.00"), queryDecimal(
                "SELECT frozen_balance FROM order_user WHERE id = 7"));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM goods_transaction_flow
                WHERE user_id = 7 AND transaction_type = 'bjfh'
                """));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM goods_transaction_flow
                WHERE user_id = 7 AND transaction_type = 'fy'
                """));
    }

    @Test
    void sequentialOrderCyclesKeepBothHistoricalRowsInsteadOfOverwriting()
            throws Exception {
        execute("""
                INSERT INTO order_user(id, balance, frozen_balance, task_progress)
                VALUES (7, 100, 0, 0)
                """);

        assertTrue(createOrRecover("O-FIRST", new BigDecimal("30.00")));
        long firstOrderId = queryLong(
                "SELECT id FROM order_info WHERE order_number = 'O-FIRST'");
        assertTrue(submit(firstOrderId));

        assertTrue(createOrRecover("O-SECOND", new BigDecimal("20.00")));
        long secondOrderId = queryLong(
                "SELECT id FROM order_info WHERE order_number = 'O-SECOND'");
        assertTrue(submit(secondOrderId));

        assertTrue(firstOrderId != secondOrderId);
        assertEquals(2L, queryLong(
                "SELECT COUNT(*) FROM order_info WHERE user_id = 7"));
        assertEquals(2L, queryLong("""
                SELECT COUNT(DISTINCT order_number)
                FROM order_info
                WHERE user_id = 7 AND status = '0'
                """));
        assertEquals(new BigDecimal("100.00"),
                queryDecimal("SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("0.00"),
                queryDecimal("SELECT frozen_balance FROM order_user WHERE id = 7"));
    }

    @Test
    void createAndSubmitDoNotDeadlockOrLoseFunds() throws Exception {
        execute("""
                INSERT INTO order_user(id, balance, frozen_balance, task_progress)
                VALUES (7, 70, 30, 1)
                """);
        execute("""
                INSERT INTO order_info(id, order_number, user_id, status, amount, rebate)
                VALUES (20, 'O-20', 7, '1', 30, 0.30)
                """);

        concurrently(
                () -> createOrRecover("O-NEXT", new BigDecimal("20.00")),
                () -> submit(20L));

        assertEquals(1L, queryLong(
                "SELECT COUNT(*) FROM order_info WHERE user_id = 7 AND status = '0'"));
        assertTrue(queryLong(
                "SELECT COUNT(*) FROM order_info WHERE user_id = 7 AND status = '1'") <= 1);
        assertEquals(new BigDecimal("100.30"), queryDecimal("""
                SELECT balance + frozen_balance
                FROM order_user
                WHERE id = 7
                """));
    }

    @Test
    void concurrentBonusClaimCreditsOnlyOnce() throws Exception {
        execute("INSERT INTO order_user(id, balance, frozen_balance) VALUES (7, 100, 0)");
        execute("""
                INSERT INTO order_bonus_table(
                    id, user_id, amount, is_received, is_distributed, distribution_type
                ) VALUES (88, 7, 28.88, '1', '1', '0')
                """);

        List<Boolean> results = concurrently(
                () -> claimBonus(88L), () -> claimBonus(88L));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals("0", queryString(
                "SELECT is_received FROM order_bonus_table WHERE id = 88"));
        assertEquals(new BigDecimal("128.88"), queryDecimal(
                "SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(1L, queryLong("""
                SELECT COUNT(*) FROM goods_transaction_flow
                WHERE user_id = 7 AND transaction_type = 'bonus'
                """));
    }

    @Test
    void mapperGuardsBlockFinancialHistoryAndReferencedConfigurationMutation()
            throws Exception {
        execute("""
                INSERT INTO order_user(
                    id, balance, frozen_balance, task_progress, version, remarks
                ) VALUES (7, 100, 30, 1, 0, 'before')
                """);
        execute("INSERT INTO goods VALUES (31, 'Product', '/product.jpg', '0', 30)");
        execute("""
                INSERT INTO order_link(
                    id, link_order_id, user_id, order_count, commission_multiple,
                    product_id, price_type, price, status
                ) VALUES (9, 1, 7, 1, 2, 31, '0', 30, '1')
                """);
        execute("""
                INSERT INTO order_info(
                    id, order_number, user_id, status, amount, rebate,
                    product_id, link_id, remarks, comment_id
                ) VALUES (20, 'O-GUARD', 7, '1', 30, 0.30, 31, 9, 'before', 1)
                """);
        execute("""
                INSERT INTO order_bonus_table(
                    id, user_id, amount, is_received, is_distributed, distribution_type
                ) VALUES
                    (88, 7, 28.88, '0', '1', '0'),
                    (89, 7, 18.88, '1', '1', '1')
                """);

        try (SqlSession session = mapperSessionFactory().openSession(false)) {
            OrderLinkMapper linkMapper = session.getMapper(OrderLinkMapper.class);
            OrderLink linkEdit = new OrderLink();
            linkEdit.setId(9L);
            linkEdit.setPrice(new BigDecimal("99.00"));
            assertEquals(0, linkMapper.updateOrderLink(linkEdit));
            assertEquals(0, linkMapper.deleteOrderLinkById(9L));

            OrderInfoMapper orderMapper = session.getMapper(OrderInfoMapper.class);
            OrderInfo metadata = new OrderInfo();
            metadata.setId(20L);
            metadata.setAmount(new BigDecimal("999.00"));
            metadata.setStatus("2");
            metadata.setRemarks("after");
            metadata.setCommentId(2L);
            assertEquals(1, orderMapper.updateOrderInfo(metadata));
            assertEquals(0, orderMapper.deleteOrderInfoById(20L));

            OrderUserMapper userMapper = session.getMapper(OrderUserMapper.class);
            OrderUser userEdit = new OrderUser();
            userEdit.setId(7L);
            userEdit.setVersion(0L);
            userEdit.setBalance(new BigDecimal("999.00"));
            userEdit.setFrozenBalance(new BigDecimal("999.00"));
            userEdit.setRemarks("after");
            assertEquals(1, userMapper.updateOrderUser(userEdit));

            OrderBonusTableMapper bonusMapper =
                    session.getMapper(OrderBonusTableMapper.class);
            OrderBonusTable claimedEdit = new OrderBonusTable();
            claimedEdit.setId(88L);
            claimedEdit.setAmount(new BigDecimal("99.00"));
            assertEquals(0, bonusMapper.updateOrderBonusTable(claimedEdit));
            assertEquals(0, bonusMapper.deleteOrderBonusTableById(88L));

            OrderBonusTable availableEdit = new OrderBonusTable();
            availableEdit.setId(89L);
            availableEdit.setAmount(new BigDecimal("19.99"));
            assertEquals(1, bonusMapper.updateOrderBonusTable(availableEdit));
            session.commit();
        }

        assertEquals(new BigDecimal("30.00"),
                queryDecimal("SELECT amount FROM order_info WHERE id = 20"));
        assertEquals("1", queryString("SELECT status FROM order_info WHERE id = 20"));
        assertEquals("after", queryString("SELECT remarks FROM order_info WHERE id = 20"));
        assertEquals(new BigDecimal("100.00"),
                queryDecimal("SELECT balance FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("30.00"),
                queryDecimal("SELECT frozen_balance FROM order_user WHERE id = 7"));
        assertEquals("after", queryString("SELECT remarks FROM order_user WHERE id = 7"));
        assertEquals(new BigDecimal("28.88"),
                queryDecimal("SELECT amount FROM order_bonus_table WHERE id = 88"));
        assertEquals(new BigDecimal("19.99"),
                queryDecimal("SELECT amount FROM order_bonus_table WHERE id = 89"));
    }

    private boolean createOrRecover(String orderNumber, BigDecimal amount) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                lockUser(connection);
                if (exists(connection,
                        "SELECT 1 FROM order_info WHERE user_id = 7 AND status = '1' LIMIT 1")) {
                    connection.commit();
                    return false;
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO order_info(order_number, user_id, status, amount, rebate)
                        VALUES (?, 7, '1', ?, 0)
                        """)) {
                    insert.setString(1, orderNumber);
                    insert.setBigDecimal(2, amount);
                    insert.executeUpdate();
                }
                try (PreparedStatement reserve = connection.prepareStatement("""
                        UPDATE order_user
                        SET balance = balance - ?,
                            frozen_balance = frozen_balance + ?,
                            task_progress = task_progress + 1,
                            version = version + 1
                        WHERE id = 7 AND balance >= ?
                        """)) {
                    reserve.setBigDecimal(1, amount);
                    reserve.setBigDecimal(2, amount);
                    reserve.setBigDecimal(3, amount);
                    if (reserve.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                insertFlow(connection, "rw", amount.negate());
                connection.commit();
                return true;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private boolean submit(long orderId) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                lockUser(connection);
                BigDecimal amount;
                BigDecimal rebate;
                String status;
                try (PreparedStatement lockOrder = connection.prepareStatement("""
                        SELECT amount, rebate, status
                        FROM order_info
                        WHERE id = ? AND user_id = 7
                        FOR UPDATE
                        """)) {
                    lockOrder.setLong(1, orderId);
                    try (ResultSet result = lockOrder.executeQuery()) {
                        result.next();
                        amount = result.getBigDecimal("amount");
                        rebate = result.getBigDecimal("rebate");
                        status = result.getString("status");
                    }
                }
                if (!"1".equals(status)) {
                    connection.commit();
                    return false;
                }
                try (PreparedStatement transition = connection.prepareStatement("""
                        UPDATE order_info SET status = '0'
                        WHERE id = ? AND user_id = 7 AND status = '1'
                        """)) {
                    transition.setLong(1, orderId);
                    if (transition.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement settle = connection.prepareStatement("""
                        UPDATE order_user
                        SET balance = balance + ? + ?,
                            frozen_balance = frozen_balance - ?,
                            version = version + 1
                        WHERE id = 7 AND balance >= 0 AND frozen_balance >= ?
                        """)) {
                    settle.setBigDecimal(1, amount);
                    settle.setBigDecimal(2, rebate);
                    settle.setBigDecimal(3, amount);
                    settle.setBigDecimal(4, amount);
                    if (settle.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                insertFlow(connection, "bjfh", amount);
                insertFlow(connection, "fy", rebate);
                connection.commit();
                return true;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private boolean claimBonus(long bonusId) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal amount;
                String received;
                try (PreparedStatement lockBonus = connection.prepareStatement("""
                        SELECT amount, is_received
                        FROM order_bonus_table
                        WHERE id = ? AND user_id = 7
                        FOR UPDATE
                        """)) {
                    lockBonus.setLong(1, bonusId);
                    try (ResultSet result = lockBonus.executeQuery()) {
                        result.next();
                        amount = result.getBigDecimal("amount");
                        received = result.getString("is_received");
                    }
                }
                if (!"1".equals(received)) {
                    connection.commit();
                    return false;
                }
                lockUser(connection);
                try (PreparedStatement claim = connection.prepareStatement("""
                        UPDATE order_bonus_table
                        SET is_received = '0', received_time = NOW(3)
                        WHERE id = ? AND user_id = 7
                          AND is_received = '1' AND is_distributed = '1'
                          AND (expiry_time IS NULL OR expiry_time > NOW())
                        """)) {
                    claim.setLong(1, bonusId);
                    if (claim.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement credit = connection.prepareStatement(
                        "UPDATE order_user SET balance = balance + ? WHERE id = 7")) {
                    credit.setBigDecimal(1, amount);
                    credit.executeUpdate();
                }
                insertFlow(connection, "bonus", amount);
                connection.commit();
                return true;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void lockUser(Connection connection) throws Exception {
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT id FROM order_user WHERE id = 7 FOR UPDATE")) {
            lock.executeQuery().close();
        }
    }

    private void insertFlow(
            Connection connection, String transactionType, BigDecimal amount)
            throws Exception {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO goods_transaction_flow(
                    user_id, transaction_type, transaction_amount,
                    balance_before, balance_after, remark
                ) VALUES (7, ?, ?, 0, ?, 'concurrency-test')
                """)) {
            insert.setString(1, transactionType);
            insert.setBigDecimal(2, amount);
            insert.setBigDecimal(3, amount);
            if (insert.executeUpdate() != 1) {
                throw new IllegalStateException("Unable to persist test flow");
            }
        }
    }

    private boolean exists(Connection connection, String sql) throws Exception {
        try (PreparedStatement select = connection.prepareStatement(sql);
             ResultSet result = select.executeQuery()) {
            return result.next();
        }
    }

    private List<Boolean> concurrently(Callable<Boolean> first, Callable<Boolean> second)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(gated(ready, start, first));
            Future<Boolean> secondResult = executor.submit(gated(ready, start, second));
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

    private void executeScript(Path scriptPath) throws Exception {
        String delimiter = ";";
        StringBuilder sql = new StringBuilder();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String line : Files.readAllLines(scriptPath)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("DELIMITER ")) {
                    delimiter = trimmed.substring("DELIMITER ".length()).trim();
                    continue;
                }
                if (trimmed.startsWith("--") || (trimmed.isEmpty() && sql.length() == 0)) {
                    continue;
                }
                sql.append(line).append('\n');
                if (trimmed.endsWith(delimiter)) {
                    int delimiterIndex = sql.lastIndexOf(delimiter);
                    String statementSql = sql.substring(0, delimiterIndex).trim();
                    if (!statementSql.isEmpty()) {
                        statement.execute(statementSql);
                    }
                    sql.setLength(0);
                }
            }
        }
        if (!sql.toString().isBlank()) {
            throw new IllegalStateException("Unterminated SQL statement in " + scriptPath);
        }
    }

    private Path findMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("sql")
                    .resolve("2026-07-18_order_api_optimization.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Order API migration script was not found");
    }

    private SqlSessionFactory mapperSessionFactory() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.order.member.domain");
        factory.setMapperLocations(
                new ClassPathResource("mapper/OrderInfoMapper.xml"),
                new ClassPathResource("mapper/OrderLinkMapper.xml"),
                new ClassPathResource("mapper/OrderBonusTableMapper.xml"),
                new ClassPathResource("mapper/OrderUserMapper.xml"),
                new ClassPathResource("mapper/OrderApiRequestMapper.xml"));
        return factory.getObject();
    }

    private RollbackHarness rollbackHarness() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        OrderUserMapper userMapper = mock(OrderUserMapper.class);
        OrderInfoMapper orderMapper = mock(OrderInfoMapper.class);
        OrderApiRequestMapper requestMapper = mock(OrderApiRequestMapper.class);
        OrderLinkMapper linkMapper = mock(OrderLinkMapper.class);
        OrderBonusTableMapper bonusMapper = mock(OrderBonusTableMapper.class);
        GoodsMapper goodsMapper = mock(GoodsMapper.class);
        OrderTradePolicyService policyService = mock(OrderTradePolicyService.class);
        IOrderSequenceManagerService sequenceService =
                mock(IOrderSequenceManagerService.class);
        ITransactionService transactionService = mock(ITransactionService.class);

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.register(TransactionTestConfiguration.class);
        context.registerBean(DataSource.class, () -> dataSource);
        context.registerBean(
                OrderApplicationService.class,
                () -> new OrderApplicationService(
                        userMapper,
                        orderMapper,
                        requestMapper,
                        linkMapper,
                        bonusMapper,
                        goodsMapper,
                        policyService,
                        sequenceService,
                        transactionService));
        context.refresh();

        return new RollbackHarness(
                context,
                jdbc,
                userMapper,
                orderMapper,
                requestMapper,
                linkMapper,
                bonusMapper,
                goodsMapper,
                policyService,
                sequenceService,
                transactionService,
                context.getBean(OrderApplicationService.class));
    }

    private void failFlowAfterInsert(RollbackHarness harness) {
        doAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            String type = invocation.getArgument(1);
            BigDecimal amount = invocation.getArgument(2);
            BigDecimal balanceBefore = invocation.getArgument(3);
            String remark = invocation.getArgument(4);
            harness.jdbc.update("""
                    INSERT INTO goods_transaction_flow(
                        user_id, transaction_type, transaction_amount,
                        balance_before, balance_after, remark
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    userId,
                    type,
                    amount,
                    balanceBefore,
                    balanceBefore.add(amount),
                    remark);
            throw new IllegalStateException("simulated flow persistence failure");
        }).when(harness.transactionService).recordFlow(
                anyLong(),
                anyString(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyString());
    }

    private OrderTradePolicyService.TradePolicy activePolicy() {
        return new OrderTradePolicyService.TradePolicy(
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new OrderTradePolicyService.PercentageRange(
                        new BigDecimal("50.00"),
                        new BigDecimal("50.00")));
    }

    private OrderUser taskUser(
            BigDecimal balance, BigDecimal frozenBalance, long taskProgress) {
        OrderUser user = balanceUser(balance, frozenBalance);
        user.setTaskProgress(taskProgress);
        user.setIsBanned("1");
        GoodsMemberLevel memberLevel = new GoodsMemberLevel();
        memberLevel.setId(2L);
        memberLevel.setOrderCountPerDay(40L);
        memberLevel.setMinCommissionRate(new BigDecimal("1.00"));
        user.setMemberLevel(memberLevel);
        return user;
    }

    private OrderUser balanceUser(BigDecimal balance, BigDecimal frozenBalance) {
        OrderUser user = new OrderUser();
        user.setId(7L);
        user.setParentId(0L);
        user.setBalance(balance);
        user.setFrozenBalance(frozenBalance);
        return user;
    }

    private Goods goods(Long id, BigDecimal price) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setTitle("Product");
        goods.setImage("/product.jpg");
        goods.setPrice(price);
        return goods;
    }

    private OrderInfo pendingOrder() {
        OrderInfo order = new OrderInfo();
        order.setId(20L);
        order.setOrderNumber("O-ROLLBACK-SUBMIT");
        order.setUserId(7L);
        order.setStatus("1");
        order.setType("0");
        order.setAmount(new BigDecimal("30.00"));
        order.setRebate(new BigDecimal("0.30"));
        order.setUpperRebate(BigDecimal.ZERO);
        return order;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionTestConfiguration {
        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    private static final class RollbackHarness implements AutoCloseable {
        private final AnnotationConfigApplicationContext context;
        private final JdbcTemplate jdbc;
        private final OrderUserMapper userMapper;
        private final OrderInfoMapper orderMapper;
        private final OrderApiRequestMapper requestMapper;
        private final OrderLinkMapper linkMapper;
        private final OrderBonusTableMapper bonusMapper;
        private final GoodsMapper goodsMapper;
        private final OrderTradePolicyService policyService;
        private final IOrderSequenceManagerService sequenceService;
        private final ITransactionService transactionService;
        private final OrderApplicationService service;

        private RollbackHarness(
                AnnotationConfigApplicationContext context,
                JdbcTemplate jdbc,
                OrderUserMapper userMapper,
                OrderInfoMapper orderMapper,
                OrderApiRequestMapper requestMapper,
                OrderLinkMapper linkMapper,
                OrderBonusTableMapper bonusMapper,
                GoodsMapper goodsMapper,
                OrderTradePolicyService policyService,
                IOrderSequenceManagerService sequenceService,
                ITransactionService transactionService,
                OrderApplicationService service) {
            this.context = context;
            this.jdbc = jdbc;
            this.userMapper = userMapper;
            this.orderMapper = orderMapper;
            this.requestMapper = requestMapper;
            this.linkMapper = linkMapper;
            this.bonusMapper = bonusMapper;
            this.goodsMapper = goodsMapper;
            this.policyService = policyService;
            this.sequenceService = sequenceService;
            this.transactionService = transactionService;
            this.service = service;
        }

        @Override
        public void close() {
            context.close();
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
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
