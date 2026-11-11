package com.order.api.service;

import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderInfoMapper;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.MemberTaskProgressService;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberTaskProgressMySqlTest {
    private static final String DATABASE = "codex_member_task_progress_test";
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory sessions;

    @BeforeAll
    static void connectToIsolatedDatabase() throws Exception {
        String override = System.getProperty("member.task.progress.jdbcUrl");
        assumeTrue(override != null, "Requires isolated codex_member_task_progress_test database");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // Schema recreation must only run against the dedicated loopback test database.
        assertTrue(override.startsWith("jdbc:mysql://"));
        URI endpoint = URI.create(override.substring("jdbc:".length()));
        assertEquals("127.0.0.1", endpoint.getHost());
        assertEquals("/" + DATABASE, endpoint.getPath());
        assertTrue(endpoint.getPort() > 0 && endpoint.getPort() <= 65535);
        assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
        assertNull(endpoint.getRawQuery());
        assertNull(endpoint.getRawFragment());
        dataSource.setUrl(override);
        dataSource.setUsername(System.getenv("PENDING_LINKED_DB_USER"));
        dataSource.setPassword(System.getenv("PENDING_LINKED_DB_PASSWORD"));
        jdbc = new JdbcTemplate(dataSource);
        assertEquals(DATABASE, jdbc.queryForObject("SELECT DATABASE()", String.class));
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.order.member.domain");
        factory.setMapperLocations(new ClassPathResource("mapper/OrderInfoMapper.xml"));
        factory.afterPropertiesSet();
        sessions = factory.getObject();
        assertNotNull(sessions);
    }

    @BeforeEach
    void createMinimalSchema() {
        jdbc.execute("DROP TABLE IF EXISTS order_info");
        jdbc.execute("""
                CREATE TABLE order_info (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    type CHAR(1),
                    link_id BIGINT,
                    order_count BIGINT,
                    status CHAR(1)
                ) ENGINE=InnoDB
                """);
    }

    @Test
    void pendingAndFrozenLinkedDetailsContributeOneGroupIndexPerMember() {
        order(1, 7, "1", 101L, 2L, "1");
        order(2, 7, "1", 102L, 2L, "2");
        order(3, 7, "1", 103L, 2L, "2");
        order(4, 8, "1", 104L, 4L, "2");
        order(5, 9, "1", 105L, 15L, "1");

        assertEquals(Map.of(7L, 2L, 8L, 4L), progress(List.of(7L, 8L)));
        assertEquals(Map.of(9L, 15L), progress(List.of(9L)));
    }

    @Test
    void completedCancelledNormalAndUnlinkedRowsNeverIncreaseTheActiveCount() {
        order(1, 7, "1", 101L, 2L, "1");
        order(2, 7, "1", 102L, 20L, "0");
        order(3, 7, "1", 103L, 21L, "3");
        order(4, 7, "0", null, 22L, "1");
        order(5, 7, "0", 104L, 23L, "2");
        order(6, 7, "1", null, 24L, "1");
        order(7, 7, null, 105L, 25L, "2");
        order(8, 7, "1", 106L, 26L, null);
        order(9, 8, "1", 107L, 27L, "0");
        order(10, 8, "1", 108L, 28L, "3");

        assertEquals(Map.of(7L, 2L), progress(List.of(7L, 8L)));
    }

    @Test
    void multipleActiveRowsUseTheMaximumSequenceRatherThanTheirCountOrSum() {
        order(1, 7, "1", 101L, 2L, "2");
        order(2, 7, "1", 102L, 3L, "1");
        order(3, 7, "1", 103L, 3L, "2");

        assertEquals(Map.of(7L, 3L), progress(List.of(7L)));
    }

    @Test
    void cancellationClearsAnOnlyPendingDetailButRetainsTheCountForFrozenSiblings() {
        IOrderConfigService configs = enabledConfig();
        OrderUser member = member(7L, 1L);
        order(1, 7, "1", 101L, 2L, "1");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        jdbc.update("UPDATE order_info SET status = '3' WHERE id = 1");
        populate(configs, member);
        assertProgress(member, 1L, 1L);

        order(2, 7, "1", 102L, 2L, "1");
        populate(configs, member);
        assertProgress(member, 1L, 2L);
        jdbc.update("UPDATE order_info SET status = '2' WHERE id = 2");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        order(3, 7, "1", 103L, 2L, "1");
        jdbc.update("UPDATE order_info SET status = '3' WHERE id = 3");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        jdbc.update("UPDATE order_info SET status = '0' WHERE id = 2");
        member.setTaskProgress(2L);
        populate(configs, member);
        assertProgress(member, 2L, 2L);
        assertTrue(progress(List.of(7L)).isEmpty());
    }

    @Test
    void theThreeDetailExampleDisplaysTwoFromFirstPendingThroughCompletion() {
        IOrderConfigService configs = enabledConfig();
        OrderUser member = member(7L, 1L);

        order(1, 7, "1", 101L, 2L, "1");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        jdbc.update("UPDATE order_info SET status = '2' WHERE id = 1");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        order(2, 7, "1", 102L, 2L, "1");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        jdbc.update("UPDATE order_info SET status = '2' WHERE id = 2");
        order(3, 7, "1", 103L, 2L, "1");
        populate(configs, member);
        assertProgress(member, 1L, 2L);

        jdbc.update("UPDATE order_info SET status = '0' WHERE user_id = 7");
        member.setTaskProgress(2L);
        populate(configs, member);
        assertProgress(member, 2L, 2L);
    }

    @Test
    void switchingConfigurationOffAndOnChangesOnlyTheDisplayForTheSameRealGroup() {
        IOrderConfigService configs = mock(IOrderConfigService.class);
        when(configs.getConfigValue("trade", "includePendingLinkedOrderInTaskProgress"))
                .thenReturn(Optional.of("0"), Optional.of("1"), Optional.of("0"));
        OrderUser member = member(7L, 1L);
        order(1, 7, "1", 101L, 2L, "1");

        populate(configs, member);
        assertProgress(member, 1L, 2L);
        populate(configs, member);
        assertProgress(member, 1L, 1L);
        populate(configs, member);
        assertProgress(member, 1L, 2L);
    }

    private IOrderConfigService enabledConfig() {
        IOrderConfigService configs = mock(IOrderConfigService.class);
        when(configs.getConfigValue("trade", "includePendingLinkedOrderInTaskProgress"))
                .thenReturn(Optional.of("0"));
        return configs;
    }

    private OrderUser member(Long userId, Long completed) {
        OrderUser member = new OrderUser();
        member.setId(userId);
        member.setTaskProgress(completed);
        return member;
    }

    private void populate(IOrderConfigService configs, OrderUser member) {
        try (SqlSession session = sessions.openSession()) {
            new MemberTaskProgressService(configs, session.getMapper(OrderInfoMapper.class)).populate(member);
        }
    }

    private Map<Long, Long> progress(Collection<Long> userIds) {
        try (SqlSession session = sessions.openSession()) {
            return session.getMapper(OrderInfoMapper.class)
                    .selectActiveLinkedTaskProgress(userIds).stream()
                    .collect(Collectors.toMap(OrderUser::getId, OrderUser::getTaskProgress));
        }
    }

    private void order(long id, long userId, String type, Long linkId, Long orderCount, String status) {
        jdbc.update("""
                INSERT INTO order_info (id, user_id, type, link_id, order_count, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, userId, type, linkId, orderCount, status);
    }

    private void assertProgress(OrderUser user, long completed, long displayed) {
        assertEquals(completed, user.getTaskProgress(), "completed task progress");
        assertEquals(displayed, user.getDisplayTaskProgress(), "displayed task progress");
    }
}
