package com.order.api.service;

import com.order.member.domain.MemberDailyStatistics;
import com.order.member.mapper.MemberDailyStatisticsMapper;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MemberDailyStatisticsMySqlTest {
    private static final String DATABASE = "codex_member_stats_test";
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 5, 16, 0);
    private static final LocalDateTime END = START.plusDays(1);
    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory sessions;

    @BeforeAll
    static void connectToIsolatedDatabase() throws Exception {
        String override = System.getProperty("member.stats.jdbcUrl");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (override != null) {
            // This test recreates tables; never accept an arbitrary database URL.
            assertTrue(override.startsWith("jdbc:mysql://"), "Expected a MySQL JDBC URL");
            URI endpoint = URI.create(override.substring("jdbc:".length()));
            assertEquals("127.0.0.1", endpoint.getHost(), "Only a loopback test database is allowed");
            assertEquals("/" + DATABASE, endpoint.getPath(), "Only the dedicated test database is allowed");
            assertTrue(endpoint.getPort() > 0 && endpoint.getPort() <= 65535, "An explicit test port is required");
            assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
            assertNull(endpoint.getRawQuery(), "Pass a plain JDBC URL without connection options");
            assertNull(endpoint.getRawFragment());
            dataSource.setUrl(override);
            dataSource.setUsername("root");
            dataSource.setPassword("");
        } else {
            // Do not use @Testcontainers: its Docker check would also skip the local override.
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Docker is unavailable; use -Dmember.stats.jdbcUrl for an isolated local MySQL");
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
        factory.setMapperLocations(new ClassPathResource("mapper/MemberDailyStatisticsMapper.xml"));
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
        for (String table : List.of("goods_transaction_flow", "order_withdrawal", "order_info",
                "order_user", "sys_time_zone")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("""
                CREATE TABLE order_user (
                    id BIGINT PRIMARY KEY,
                    parent_id BIGINT
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_info (
                    order_number VARCHAR(64) PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    create_time DATETIME(3)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_transaction_flow (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    transaction_type VARCHAR(30) NOT NULL,
                    transaction_amount DECIMAL(20,2) NOT NULL,
                    created_time DATETIME(3) NOT NULL,
                    remark VARCHAR(255)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_withdrawal (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    create_time DATETIME(3) NOT NULL
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE sys_time_zone (
                    id BIGINT PRIMARY KEY,
                    tz_name VARCHAR(100) NOT NULL,
                    status INT NOT NULL
                ) ENGINE=InnoDB
                """);
    }

    @Test
    void reproducesCrossMidnightTotalsWithoutMultiplyingCommissionsByWithdrawals() {
        user(247, 246);
        user(246, 43);
        LocalDateTime yesterday = START.minusNanos(1_000_000);
        order("BEFORE-MIDNIGHT", 247, START.minusMinutes(10));
        order("SETTLED-AT-MIDNIGHT", 247, START.minusSeconds(3));
        order("SETTLED-TODAY", 247, START.plusMinutes(1));
        order("SETTLED-TOMORROW", 247, START.plusMinutes(2));
        flow(247, "fy", "179.78", yesterday, "order-rebate:BEFORE-MIDNIGHT");
        flow(246, "xjfy", "35.94", yesterday, "child-order-rebate:BEFORE-MIDNIGHT");
        flow(247, "fy", "200.00", START, "order-rebate:SETTLED-AT-MIDNIGHT");
        flow(246, "xjfy", "40.00", START, "child-order-rebate:SETTLED-AT-MIDNIGHT");
        flow(247, "fy", "201.47", START.plusMinutes(1), "order-rebate:SETTLED-TODAY");
        flow(246, "xjfy", "40.27", START.plusMinutes(1), "child-order-rebate:SETTLED-TODAY");
        flow(247, "fy", "999.00", END, "order-rebate:SETTLED-TOMORROW");
        flow(246, "xjfy", "199.80", END, "child-order-rebate:SETTLED-TOMORROW");
        withdrawal(247, yesterday);
        withdrawal(247, START);
        withdrawal(247, START.plusMinutes(2));
        withdrawal(247, END.minusNanos(1_000_000));
        withdrawal(247, END);

        MemberDailyStatistics previous = statistics(START.minusDays(1), START, 247L).get(247L);
        MemberDailyStatistics today = statistics(START, END, 247L).get(247L);
        MemberDailyStatistics both = statistics(START.minusDays(1), END, 247L).get(247L);

        assertTotals(previous, "179.78", "35.94", 1);
        assertTotals(today, "401.47", "80.27", 3);
        assertTotals(both, "581.25", "116.21", 4);
    }

    @Test
    void associatesParentCommissionByTheExactOrderAndTransactionType() {
        user(247, 246);
        user(1247, 246);
        user(246, 43);
        order("247", 247, START.minusDays(1));
        order("1247", 1247, START.minusDays(1));
        flow(247, "fy", "5.00", START, "order-rebate:247");
        flow(1247, "fy", "7.00", START, "order-rebate:1247");
        flow(246, "xjfy", "1.00", START, "child-order-rebate:247");
        flow(246, "xjfy", "1.40", START, "child-order-rebate:1247");
        flow(246, "fy", "700.00", START, "child-order-rebate:247");
        flow(246, "xjfy", "800.00", START, "child-order-rebate:247-suffix");
        flow(246, "xjfy", "900.00", START, "prefix-child-order-rebate:247");
        flow(246, "xjfy", "1000.00", START, "child-order-rebate:missing-order");
        flow(246, "xjfy", "1100.00", START, null);
        flow(247, "rw", "-300.00", START, "order-reserve:247");
        flow(247, "bjfh", "300.00", START, "order-principal:247");
        flow(247, "ck", "1120.00", START, null);

        Map<Long, MemberDailyStatistics> result = statistics(START, END, 247L, 1247L, 246L);

        assertEquals(3, result.size());
        assertTotals(result.get(247L), "5.00", "1.00", 0);
        assertTotals(result.get(1247L), "7.00", "1.40", 0);
        assertTotals(result.get(246L), "700.00", "0.00", 0);
    }

    @Test
    void returnsZeroForSelectedMembersWithoutActivityAndExcludesUnselectedMembers() {
        user(247, 246);
        user(1247, 246);
        order("UNSELECTED", 1247, START);
        flow(1247, "fy", "55.00", START, "order-rebate:UNSELECTED");
        flow(246, "xjfy", "11.00", START, "child-order-rebate:UNSELECTED");
        withdrawal(1247, START);

        Map<Long, MemberDailyStatistics> result = statistics(START, END, 247L);

        assertEquals(1, result.size());
        assertTotals(result.get(247L), "0.00", "0.00", 0);
    }

    @Test
    void readsTheFirstActiveTimeZoneAndReturnsNullWhenNoneIsConfigured() {
        try (SqlSession session = sessions.openSession()) {
            assertNull(session.getMapper(MemberDailyStatisticsMapper.class).selectActiveTimeZone());
        }
        jdbc.update("INSERT INTO sys_time_zone VALUES (1, 'UTC', 1), (3, 'Asia/Tokyo', 0), (2, 'Asia/Shanghai', 0)");

        try (SqlSession session = sessions.openSession()) {
            assertEquals("Asia/Shanghai",
                    session.getMapper(MemberDailyStatisticsMapper.class).selectActiveTimeZone());
        }
    }

    private Map<Long, MemberDailyStatistics> statistics(LocalDateTime start, LocalDateTime end, Long... ids) {
        try (SqlSession session = sessions.openSession()) {
            List<MemberDailyStatistics> rows = session.getMapper(MemberDailyStatisticsMapper.class)
                    .selectDailyStatistics(Arrays.asList(ids), start, end);
            return rows.stream().collect(Collectors.toMap(MemberDailyStatistics::getUserId, Function.identity()));
        }
    }

    private void user(long id, long parentId) {
        jdbc.update("INSERT INTO order_user(id, parent_id) VALUES (?, ?)", id, parentId);
    }

    private void order(String number, long userId, LocalDateTime created) {
        jdbc.update("INSERT INTO order_info(order_number, user_id, create_time) VALUES (?, ?, ?)",
                number, userId, Timestamp.valueOf(created));
    }

    private void flow(long userId, String type, String amount, LocalDateTime created, String remark) {
        jdbc.update("""
                INSERT INTO goods_transaction_flow(user_id, transaction_type, transaction_amount, created_time, remark)
                VALUES (?, ?, ?, ?, ?)
                """, userId, type, new BigDecimal(amount), Timestamp.valueOf(created), remark);
    }

    private void withdrawal(long userId, LocalDateTime created) {
        jdbc.update("INSERT INTO order_withdrawal(user_id, create_time) VALUES (?, ?)",
                userId, Timestamp.valueOf(created));
    }

    private void assertTotals(MemberDailyStatistics actual, String own, String parent, int withdrawals) {
        assertNotNull(actual);
        assertNotNull(actual.getTodayCommission());
        assertNotNull(actual.getTodayParentCommission());
        assertEquals(0, new BigDecimal(own).compareTo(actual.getTodayCommission()), "Own commission");
        assertEquals(0, new BigDecimal(parent).compareTo(actual.getTodayParentCommission()), "Parent commission");
        assertEquals(Integer.valueOf(withdrawals), actual.getTodayWithdrawalCount(), "Withdrawal count");
    }
}
