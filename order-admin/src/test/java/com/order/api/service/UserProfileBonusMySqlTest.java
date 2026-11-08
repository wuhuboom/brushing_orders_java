package com.order.api.service;

import com.order.api.controller.dto.UserProfileResponse;
import com.order.member.domain.OrderBonusTable;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderBonusTableMapper;
import com.order.member.mapper.OrderUserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UserProfileBonusMySqlTest {
    private static final String DATABASE = "codex_user_profile_bonus_test";
    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory sessions;

    @BeforeAll
    static void connectToIsolatedDatabase() throws Exception {
        String override = System.getProperty("user.profile.bonus.jdbcUrl");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (override != null) {
            // This test recreates tables; only accept a dedicated loopback test database.
            assertTrue(override.startsWith("jdbc:mysql://"));
            URI endpoint = URI.create(override.substring("jdbc:".length()));
            assertEquals("127.0.0.1", endpoint.getHost());
            assertEquals("/" + DATABASE, endpoint.getPath());
            assertTrue(endpoint.getPort() > 0 && endpoint.getPort() <= 65535);
            assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
            assertNull(endpoint.getRawQuery());
            assertNull(endpoint.getRawFragment());
            dataSource.setUrl(override);
            dataSource.setUsername("root");
            dataSource.setPassword("");
        } else {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Docker is unavailable; use -Duser.profile.bonus.jdbcUrl for an isolated local MySQL");
            mysql = new MySQLContainer("mysql:8.0.42")
                    .withDatabaseName(DATABASE)
                    .withUsername("order")
                    .withPassword("order");
            mysql.start();
            dataSource.setUrl(mysql.getJdbcUrl());
            dataSource.setUsername(mysql.getUsername());
            dataSource.setPassword(mysql.getPassword());
        }
        jdbc = new JdbcTemplate(dataSource);
        assertEquals(DATABASE, jdbc.queryForObject("SELECT DATABASE()", String.class));
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.order.member.domain");
        factory.setMapperLocations(
                new ClassPathResource("mapper/OrderUserMapper.xml"),
                new ClassPathResource("mapper/OrderBonusTableMapper.xml"));
        factory.afterPropertiesSet();
        sessions = factory.getObject();
        assertNotNull(sessions);
    }

    @AfterAll
    static void stopOwnedContainer() {
        if (mysql != null) {
            mysql.stop();
        }
    }

    @BeforeEach
    void createMinimalSchema() {
        for (String table : List.of("goods_transaction_flow", "order_bonus_table", "order_info",
                "order_user", "goods_member_level")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("""
                CREATE TABLE order_user (
                    id BIGINT PRIMARY KEY, username VARCHAR(64), phone_number VARCHAR(64),
                    avatar VARCHAR(255), gender CHAR(1), email VARCHAR(255), birthday DATETIME,
                    vip_id BIGINT, parent_id BIGINT, invite_code VARCHAR(64),
                    balance DECIMAL(20,2), frozen_balance DECIMAL(20,2), base_salary DECIMAL(20,2),
                    task_progress BIGINT, reputation_score BIGINT, work_limit DECIMAL(20,2),
                    is_enabled CHAR(1), allow_invite CHAR(1), is_frozen CHAR(1), is_banned CHAR(1),
                    is_withdrawal_notification CHAR(1), product_matching CHAR(1), account_status CHAR(1),
                    product_match_min DECIMAL(5,2), product_match_max DECIMAL(5,2),
                    transaction_status CHAR(1), withdrawal_status CHAR(1),
                    deposit_block_withdrawal CHAR(1), assist_withdrawal_status CHAR(1)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_member_level (
                    id BIGINT PRIMARY KEY, name VARCHAR(64), level BIGINT, icon VARCHAR(255),
                    price DECIMAL(20,2), min_balance DECIMAL(20,2), invite_count BIGINT,
                    order_count_per_day BIGINT, min_commission_rate DECIMAL(20,2),
                    max_commission_rate DECIMAL(20,2), min_continuous_commission_rate DECIMAL(20,2),
                    max_continuous_commission_rate DECIMAL(20,2), task_count_per_day BIGINT,
                    withdraw_count_per_day BIGINT, withdraw_fee_rate DECIMAL(20,2),
                    min_withdraw_amount DECIMAL(20,2), withdraw_limit_per_day DECIMAL(20,2),
                    min_withdraw DECIMAL(20,2), max_withdraw DECIMAL(20,2), description VARCHAR(255),
                    product_match_enabled CHAR(1), product_match_min DECIMAL(20,2),
                    product_match_max DECIMAL(20,2)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_info (
                    id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, status CHAR(1) NOT NULL
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_transaction_flow (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
                    transaction_type VARCHAR(16) NOT NULL, transaction_amount DECIMAL(20,2) NOT NULL,
                    created_time DATETIME NOT NULL,
                    INDEX idx_user_type (user_id, transaction_type)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_bonus_table (
                    id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, order_num BIGINT,
                    amount DECIMAL(20,2) NOT NULL, is_received CHAR(1) NOT NULL DEFAULT '1',
                    is_distributed CHAR(1) NOT NULL DEFAULT '1', expiry_time DATETIME(3),
                    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
                    received_time DATETIME(3), distribution_time DATETIME(3),
                    animation_duration BIGINT, display_duration BIGINT, distribution_type CHAR(1),
                    push_type CHAR(1), to_users VARCHAR(255)
                ) ENGINE=InnoDB
                """);
        jdbc.update("INSERT INTO order_user(id, task_progress) VALUES (7, 0), (8, 0)");
    }

    @Test
    void totalCommissionIsZeroWhenThereAreNoCommissionFlows() {
        assertTotalCommission("0");
    }

    @Test
    void totalCommissionIncludesAllDatesOnlyForTheCurrentUsersOrderCommissionsAndRefreshesAfterSettlement() {
        flow(7, "fy", "10.25", "2020-01-01 00:00:00");
        flow(7, "fy", "95.41", "2026-10-30 12:00:00");
        flow(8, "fy", "500.00", "2026-10-30 12:00:00");
        for (String type : List.of("xjfy", "jj", "cz", "bjfh")) {
            flow(7, type, "100.00", "2026-10-30 12:00:00");
        }

        assertTotalCommission("105.66");

        flow(7, "fy", "2.34", "2026-10-30 12:01:00");
        assertTotalCommission("108.00");
    }

    @Test
    void pendingAmountRemainsAfterClaimAndClearsOnlyAfterDistribution() {
        assertAmounts("0", "0");
        bonus(1, 7, 5, "125.50");
        assertAmounts("0", "0");

        jdbc.update("UPDATE order_user SET task_progress = 4 WHERE id = 7");
        assertAmounts("125.50", "0");

        try (SqlSession session = sessions.openSession(true)) {
            assertEquals(1, session.getMapper(OrderBonusTableMapper.class).receiveBonus(1L));
        }
        assertAmounts("125.50", "0");

        try (SqlSession session = sessions.openSession(true)) {
            assertEquals(1, session.getMapper(OrderBonusTableMapper.class).distributeBonus(1L));
        }
        assertAmounts("0", "125.50");

        bonus(2, 7, 5, "42.25");
        assertAmounts("42.25", "125.50");
    }

    @Test
    void claimedBonusRemainsAfterProgressChangesOpenOrdersAndExpiryUntilDistribution() {
        bonus(1, 7, 1, "125.50");
        try (SqlSession session = sessions.openSession(true)) {
            assertEquals(1, session.getMapper(OrderBonusTableMapper.class).receiveBonus(1L));
        }
        assertAmounts("125.50", "0");

        jdbc.update("UPDATE order_user SET task_progress = 6 WHERE id = 7");
        assertAmounts("125.50", "0");
        jdbc.update("INSERT INTO order_info VALUES (1, 7, '1')");
        assertAmounts("125.50", "0");
        jdbc.update("UPDATE order_bonus_table SET expiry_time = NOW() - INTERVAL 1 SECOND WHERE id = 1");
        assertAmounts("125.50", "0");

        try (SqlSession session = sessions.openSession(true)) {
            assertEquals(1, session.getMapper(OrderBonusTableMapper.class).distributeBonus(1L));
        }
        assertAmounts("0", "125.50");
    }

    @Test
    void claimedUndistributedBonusTakesPriorityOverTheNextUnclaimedBonus() {
        bonus(1, 7, 1, "125.50");
        try (SqlSession session = sessions.openSession(true)) {
            assertEquals(1, session.getMapper(OrderBonusTableMapper.class).receiveBonus(1L));
        }
        jdbc.update("UPDATE order_bonus_table SET expiry_time = NOW() + INTERVAL 1 DAY WHERE id = 1");
        jdbc.update("UPDATE order_user SET task_progress = 1 WHERE id = 7");
        bonus(2, 7, 2, "42.25");
        assertAmounts("125.50", "0");

        try (SqlSession session = sessions.openSession(true)) {
            assertEquals(1, session.getMapper(OrderBonusTableMapper.class).distributeBonus(1L));
        }
        assertAmounts("42.25", "125.50");
    }

    @Test
    void unsubmittedOrderDelaysTriggerUntilItIsCompleted() {
        bonus(1, 7, 2, "88.00");
        jdbc.update("UPDATE order_user SET task_progress = 1 WHERE id = 7");
        jdbc.update("INSERT INTO order_info VALUES (1, 7, '1')");
        assertAmounts("0", "0");

        jdbc.update("UPDATE order_info SET status = '0' WHERE id = 1");
        jdbc.update("INSERT INTO order_info VALUES (2, 8, '1')");
        assertAmounts("88.00", "0");
    }

    @Test
    void ignoresPastFutureExpiredAndOtherUsersBonuses() {
        jdbc.update("UPDATE order_user SET task_progress = 4 WHERE id = 7");
        bonus(1, 7, 4, "10.00");
        bonus(2, 7, 6, "20.00");
        bonus(3, 8, 5, "30.00");
        jdbc.update("UPDATE order_bonus_table SET is_received = '0' WHERE id = 3");
        bonus(4, 7, 5, "40.00");
        jdbc.update("UPDATE order_bonus_table SET expiry_time = NOW() - INTERVAL 1 SECOND WHERE id = 4");
        assertAmounts("0", "0");

        jdbc.update("UPDATE order_bonus_table SET expiry_time = NOW() + INTERVAL 1 DAY WHERE id = 4");
        assertAmounts("40.00", "0");
    }

    @Test
    void returnsTheSameSingleBonusAsOrderCreationWhenSeveralRecordsMatch() {
        bonus(1, 7, 1, "10.00");
        bonus(2, 7, 1, "20.00");
        bonus(3, 7, 1, "30.00");
        jdbc.update("""
                UPDATE order_bonus_table
                SET expiry_time = '2099-01-01', create_time = '2026-01-01'
                """);
        jdbc.update("UPDATE order_bonus_table SET expiry_time = '2099-01-02' WHERE id = 1");
        assertMatchesOrderCreation("20.00");

        jdbc.update("UPDATE order_bonus_table SET create_time = '2025-01-01' WHERE id = 3");
        assertMatchesOrderCreation("30.00");

        jdbc.update("UPDATE order_bonus_table SET expiry_time = NULL WHERE id = 1");
        assertMatchesOrderCreation("10.00");
    }

    @Test
    void nullTaskProgressUsesFirstOrderAndDistributedBonusesDoNotBecomePending() {
        jdbc.update("UPDATE order_user SET task_progress = NULL WHERE id = 7");
        bonus(1, 7, 1, "15.75");
        assertAmounts("15.75", "0");
        jdbc.update("UPDATE order_bonus_table SET is_distributed = '0' WHERE id = 1");
        assertAmounts("0", "15.75");
    }

    @Test
    void exportTotalsMatchProfilesAcrossMembersAndBonusEligibilityTransitions() {
        jdbc.update("UPDATE order_user SET task_progress = CASE id WHEN 7 THEN 4 ELSE 1 END");
        jdbc.update("INSERT INTO order_user(id, task_progress) VALUES (9, 0)");
        flow(7, "fy", "10.25", "2020-01-01 00:00:00");
        flow(7, "fy", "2.50", "2026-10-30 12:00:00");
        flow(7, "xjfy", "999.00", "2026-10-30 12:00:00");
        flow(8, "fy", "81.00", "2026-10-30 12:00:00");
        flow(9, "fy", "999.00", "2026-10-30 12:00:00");

        bonus(101, 7, 5, "70.00");
        bonus(102, 7, 5, "71.00");
        bonus(103, 7, 4, "72.00");
        bonus(104, 7, 6, "73.00");
        bonus(105, 7, 2, "7.25");
        bonus(106, 7, 3, "17.50");
        bonus(107, 7, 1, "18.50");
        bonus(201, 8, 2, "20.00");
        bonus(202, 8, 2, "21.00");
        bonus(203, 8, 2, "22.00");
        bonus(204, 8, 2, "23.00");
        bonus(205, 8, 1, "8.75");
        bonus(301, 9, 1, "999.00");
        jdbc.update("UPDATE order_bonus_table SET is_received = '0' WHERE id IN (105, 106, 107, 205)");
        jdbc.update("UPDATE order_bonus_table SET is_distributed = '0' WHERE id IN (105, 205)");
        jdbc.update("UPDATE order_bonus_table SET expiry_time = NOW() - INTERVAL 1 DAY WHERE id IN (102, 106)");
        jdbc.update("UPDATE order_bonus_table SET expiry_time = '2099-01-01', create_time = '2026-01-01' WHERE id BETWEEN 201 AND 204");
        jdbc.update("UPDATE order_bonus_table SET expiry_time = '2099-01-02' WHERE id = 201");
        jdbc.update("UPDATE order_bonus_table SET create_time = '2025-01-01' WHERE id IN (203, 204)");
        jdbc.update("INSERT INTO order_info VALUES (1, 7, '1')");

        assertExportMatchesProfiles("7.25", "18.50", "22.00");

        // Claimed bonuses survive expiry and open orders; only distribution removes them.
        jdbc.update("UPDATE order_bonus_table SET is_distributed = '0' WHERE id = 107");
        jdbc.update("INSERT INTO order_info VALUES (2, 8, '1')");
        assertExportMatchesProfiles("25.75", "17.50", "0.00");

        jdbc.update("UPDATE order_bonus_table SET is_distributed = '0' WHERE id = 106");
        jdbc.update("UPDATE order_info SET status = '0'");
        assertExportMatchesProfiles("43.25", "70.00", "22.00");
    }

    @Test
    void emptyExportScopeReturnsNoMembersEvenWhenFinancialRecordsExist() {
        flow(7, "fy", "10.00", "2026-10-30 12:00:00");
        bonus(1, 7, 1, "50.00");

        try (SqlSession session = sessions.openSession()) {
            assertTrue(session.getMapper(OrderUserMapper.class)
                    .selectOrderUserExportStatistics(List.of()).isEmpty());
        }
    }

    private void assertExportMatchesProfiles(String firstDistributed, String firstPending, String secondPending) {
        try (SqlSession session = sessions.openSession()) {
            OrderUserMapper mapper = session.getMapper(OrderUserMapper.class);
            Map<Long, OrderUser> exported = mapper.selectOrderUserExportStatistics(List.of(8L, 7L)).stream()
                    .collect(Collectors.toMap(OrderUser::getId, Function.identity()));
            assertEquals(java.util.Set.of(7L, 8L), exported.keySet());
            for (long id : List.of(7L, 8L)) {
                OrderUser actual = exported.get(id);
                OrderUser profile = mapper.selectUserProfileById(id);
                assertEquals(0, profile.getTotalCommission().compareTo(actual.getTotalCommission()), "commission for " + id);
                assertEquals(0, profile.getLuckyBonus().compareTo(actual.getLuckyBonus()), "distributed bonus for " + id);
                assertEquals(0, profile.getPendingBonusAmount().compareTo(actual.getPendingBonusAmount()), "pending bonus for " + id);
            }
            assertEquals(0, new BigDecimal("12.75").compareTo(exported.get(7L).getTotalCommission()));
            assertEquals(0, new BigDecimal("81.00").compareTo(exported.get(8L).getTotalCommission()));
            assertEquals(0, new BigDecimal(firstDistributed).compareTo(exported.get(7L).getLuckyBonus()));
            assertEquals(0, new BigDecimal("8.75").compareTo(exported.get(8L).getLuckyBonus()));
            assertEquals(0, new BigDecimal(firstPending).compareTo(exported.get(7L).getPendingBonusAmount()));
            assertEquals(0, new BigDecimal(secondPending).compareTo(exported.get(8L).getPendingBonusAmount()));
        }
    }

    private void flow(long userId, String type, String amount, String createdTime) {
        jdbc.update("""
                INSERT INTO goods_transaction_flow(user_id, transaction_type, transaction_amount, created_time)
                VALUES (?, ?, ?, ?)
                """, userId, type, new BigDecimal(amount), Timestamp.valueOf(createdTime));
    }

    private void assertTotalCommission(String expected) {
        try (SqlSession session = sessions.openSession()) {
            UserProfileResponse profile = UserProfileResponse.from(
                    session.getMapper(OrderUserMapper.class).selectUserProfileById(7L));
            assertNotNull(profile);
            assertNotNull(profile.totalCommission());
            assertEquals(0, new BigDecimal(expected).compareTo(profile.totalCommission()), "total commission");
        }
    }

    private void bonus(long id, long userId, long orderNum, String amount) {
        jdbc.update("INSERT INTO order_bonus_table(id, user_id, order_num, amount) VALUES (?, ?, ?, ?)",
                id, userId, orderNum, new BigDecimal(amount));
    }

    private void assertAmounts(String pending, String distributed) {
        try (SqlSession session = sessions.openSession()) {
            UserProfileResponse profile = UserProfileResponse.from(
                    session.getMapper(OrderUserMapper.class).selectUserProfileById(7L));
            assertNotNull(profile);
            assertEquals(0, new BigDecimal(pending).compareTo(profile.pendingBonusAmount()), "pending amount");
            assertEquals(0, new BigDecimal(distributed).compareTo(profile.luckyBonus()), "distributed total");
        }
    }

    private void assertMatchesOrderCreation(String expected) {
        assertAmounts(expected, "0");
        try (SqlSession session = sessions.openSession()) {
            OrderBonusTable bonus = session.getMapper(OrderBonusTableMapper.class)
                    .selectActiveDistributedReceivedByUserAndOrder(7L, 1L);
            assertNotNull(bonus);
            assertEquals(0, new BigDecimal(expected).compareTo(bonus.getAmount()));
        }
    }
}
