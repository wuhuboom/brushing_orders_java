package com.order.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.i18n.ITranslationsService;
import com.order.member.domain.OrderConfig;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.mapper.OrderConfigMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.impl.OrderConfigServiceImpl;
import com.order.member.service.impl.OrderUserServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real registration/configuration SQL, restricted to a dedicated disposable database. */
class OrderUserRegistrationMySqlTest {
    private static final String DATABASE = "codex_registration_full_order_test";
    private static MySQLContainer mysql;
    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory sessions;

    private OrderConfigServiceImpl config;
    private IOrderUserService registration;
    private ITransactionService money;

    @BeforeAll
    static void connectToIsolatedDatabase() throws Exception {
        String override = System.getProperty("registration.fullorder.jdbcUrl");
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (override != null) {
            // These tests drop tables. Never accept a remote or business database override.
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
                    "Docker is unavailable; use -Dregistration.fullorder.jdbcUrl for an isolated local MySQL");
            mysql = new MySQLContainer("mysql:8.0.42")
                    .withDatabaseName(DATABASE).withUsername("order").withPassword("order");
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
        factory.setMapperLocations(new ClassPathResource("mapper/OrderUserMapper.xml"),
                new ClassPathResource("mapper/GoodsMemberLevelMapper.xml"),
                new ClassPathResource("mapper/OrderConfigMapper.xml"));
        factory.afterPropertiesSet();
        sessions = factory.getObject();
        assertNotNull(sessions);
    }

    @AfterAll
    static void stopOwnedContainer() {
        if (mysql != null) mysql.stop();
    }

    @BeforeEach
    void createSchemaAndServices() {
        for (String table : List.of("goods_transaction_flow", "order_user", "goods_member_level", "order_config")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("""
                CREATE TABLE order_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) UNIQUE,
                    phone_number VARCHAR(64), password VARCHAR(255), trade_password VARCHAR(255),
                    vip_id BIGINT, parent_id BIGINT, invite_code VARCHAR(64) UNIQUE,
                    ancestors VARCHAR(255), allow_invite CHAR(1) DEFAULT '1',
                    account_status CHAR(1) DEFAULT '1', balance DECIMAL(20,2) DEFAULT 0,
                    task_progress BIGINT NOT NULL DEFAULT 0, create_time DATETIME
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
                    create_time DATETIME, product_match_enabled CHAR(1),
                    product_match_min DECIMAL(20,2), product_match_max DECIMAL(20,2), translations_id BIGINT
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_config (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, type VARCHAR(64) UNIQUE,
                    sort BIGINT, name VARCHAR(64), content LONGTEXT, translations_id BIGINT,
                    create_time DATETIME, update_time DATETIME
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_transaction_flow (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT,
                    transaction_type VARCHAR(16), transaction_amount DECIMAL(20,2),
                    balance_before DECIMAL(20,2)
                ) ENGINE=InnoDB
                """);
        jdbc.update("""
                INSERT INTO goods_member_level(id, name, level, price, order_count_per_day)
                VALUES (1, 'VIP1', 1, 0, 40), (2, 'VIP2', 2, 100, 45), (3, 'VIP3', 3, 200, 50)
                """);
        jdbc.update("""
                INSERT INTO order_user(id, username, invite_code, ancestors, task_progress)
                VALUES (7, 'referrer', 'REF77', '0,3', 12)
                """);

        SqlSessionTemplate template = new SqlSessionTemplate(sessions);
        config = new OrderConfigServiceImpl(template.getMapper(OrderConfigMapper.class),
                new ObjectMapper(), mock(ITranslationsService.class));
        money = mock(ITransactionService.class);
        // Keep the external money subsystem small while verifying shared-transaction writes.
        when(money.recordFlow(anyLong(), anyString(), any(), any(), anyString())).thenAnswer(call -> {
            jdbc.update("""
                    INSERT INTO goods_transaction_flow(user_id, transaction_type, transaction_amount, balance_before)
                    VALUES (?, ?, ?, ?)
                    """, call.<Long>getArgument(0), call.<String>getArgument(1),
                    call.<BigDecimal>getArgument(2), call.<BigDecimal>getArgument(3));
            return null;
        });
        OrderUserServiceImpl target = new OrderUserServiceImpl();
        ReflectionTestUtils.setField(target, "orderUserMapper", template.getMapper(OrderUserMapper.class));
        ReflectionTestUtils.setField(target, "memberLevelMapper", template.getMapper(GoodsMemberLevelMapper.class));
        ReflectionTestUtils.setField(target, "configService", config);
        ReflectionTestUtils.setField(target, "transactionService", money);
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        registration = (IOrderUserService) proxy.getProxy();
    }

    @Test
    void absentWebsiteConfigurationDefaultsToZeroOrders() {
        assertRegistration("no_website", 1, 0);
        verifyNoInteractions(money);
    }

    @Test
    void legacyWebsiteConfigurationWithoutTheNewKeyDefaultsToZeroAndKeepsOtherSettings() {
        saveConfig("website", "{\"title\":\"Existing site\",\"transactionStatus\":\"1\"}");
        assertRegistration("legacy_website", 1, 0);
        assertEquals("Existing site", config.getConfigValue("website", "title").orElseThrow());
    }

    @Test
    void disabledStringNumericAndBooleanValuesAllKeepNewUsersAtZero() {
        int index = 0;
        for (String value : List.of("\"0\"", "0", "false", "null", "\"invalid\"")) {
            saveConfig("website", "{\"registerFullOrder\":" + value + "}");
            assertRegistration("disabled_" + index++, 1, 0);
        }
        verifyNoInteractions(money);
    }

    @Test
    void enabledStringNumericAndBooleanValuesPersistVipOneAtFortyOrders() {
        int index = 0;
        for (String value : List.of("\"1\"", "1", "true")) {
            saveConfig("website", "{\"registerFullOrder\":" + value + "}");
            OrderUser user = assertRegistration("enabled_" + index++, 1, 40);
            assertEquals(7L, user.getParentId());
            assertEquals("0,3,7", user.getAncestors());
            assertNotEquals("REF77", user.getInviteCode());
            assertEquals(new BigDecimal("0.00"), storedBalance(user));
        }
        assertEquals(0, flowCount());
        verifyNoInteractions(money);
    }

    @Test
    void registrationUsesTheActualAssignedVipTwoAndVipThreeOrderLimits() {
        saveConfig("website", "{\"registerFullOrder\":\"1\"}");
        jdbc.update("UPDATE goods_member_level SET price = -1 WHERE id = 2");
        assertRegistration("vip_two", 2, 45);
        jdbc.update("UPDATE goods_member_level SET price = -2 WHERE id = 3");
        assertRegistration("vip_three", 3, 50);
    }

    @Test
    void savedSwitchAndLevelChangesApplyToLaterRegistrationsWithoutChangingExistingUsers() {
        saveConfig("website", "{\"registerFullOrder\":\"0\",\"title\":\"Existing site\"}");
        OrderUser first = assertRegistration("before_enabled", 1, 0);
        saveConfig("website", "{\"registerFullOrder\":\"1\",\"title\":\"Existing site\"}");
        OrderUser second = assertRegistration("after_enabled", 1, 40);
        jdbc.update("UPDATE goods_member_level SET order_count_per_day = 52 WHERE id = 1");
        OrderUser third = assertRegistration("new_level_limit", 1, 52);
        saveConfig("website", "{\"registerFullOrder\":\"0\",\"title\":\"Existing site\"}");
        assertRegistration("after_disabled", 1, 0);
        assertEquals(0, storedProgress(first));
        assertEquals(40, storedProgress(second));
        assertEquals(52, storedProgress(third));
        assertEquals(12L, jdbc.queryForObject("SELECT task_progress FROM order_user WHERE id = 7", Long.class));
        assertEquals("Existing site", config.getConfigValue("website", "title").orElseThrow());
    }

    @Test
    void enabledRegistrationAcceptsAnExplicitZeroLevelLimit() {
        saveConfig("website", "{\"registerFullOrder\":\"1\"}");
        jdbc.update("UPDATE goods_member_level SET order_count_per_day = 0 WHERE id = 1");
        assertRegistration("zero_limit", 1, 0);
    }

    @Test
    void missingOrNegativeLevelLimitsRejectRegistrationWithoutPersistingAUser() {
        saveConfig("website", "{\"registerFullOrder\":\"1\"}");
        jdbc.update("UPDATE goods_member_level SET order_count_per_day = NULL WHERE id = 1");
        assertThrows(IllegalStateException.class, () -> registration.register(newUser("missing_limit")));
        jdbc.update("UPDATE goods_member_level SET order_count_per_day = -1 WHERE id = 1");
        assertThrows(IllegalStateException.class, () -> registration.register(newUser("negative_limit")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM order_user", Integer.class));
        verifyNoInteractions(money);
    }

    @Test
    void registerBonusStillCreditsTheNewFullOrderUserExactlyOnce() {
        saveConfig("website", "{\"registerFullOrder\":\"1\"}");
        saveConfig("trade", "{\"registerBonusAmount\":25.50}");
        OrderUser user = assertRegistration("with_bonus", 1, 40);
        assertEquals(new BigDecimal("25.50"), storedBalance(user));
        assertEquals(1, flowCount());
        verify(money).recordFlow(eq(user.getId()), eq("zczs"),
                argThat(amount -> amount.compareTo(new BigDecimal("25.50")) == 0),
                eq(BigDecimal.ZERO), eq("Registration bonus"));
        assertEquals("zczs", jdbc.queryForObject("SELECT transaction_type FROM goods_transaction_flow", String.class));
        assertEquals(user.getId(), jdbc.queryForObject("SELECT user_id FROM goods_transaction_flow", Long.class));
        assertEquals(new BigDecimal("25.50"), jdbc.queryForObject(
                "SELECT transaction_amount FROM goods_transaction_flow", BigDecimal.class));
    }

    @Test
    void bonusFailureRollsBackTheFullOrderUserAndItsFlow() {
        saveConfig("website", "{\"registerFullOrder\":\"1\"}");
        saveConfig("trade", "{\"registerBonusAmount\":25.50}");
        doAnswer(call -> {
            jdbc.update("INSERT INTO goods_transaction_flow(user_id, transaction_type) VALUES (?, 'zczs')",
                    call.<Long>getArgument(0));
            throw new IllegalStateException("simulated bonus failure");
        }).when(money).recordFlow(anyLong(), anyString(), any(), any(), anyString());
        assertThrows(IllegalStateException.class, () -> registration.register(newUser("bonus_failed")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM order_user WHERE username = 'bonus_failed'", Integer.class));
        assertEquals(0, flowCount());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM order_user", Integer.class));
    }

    private void saveConfig(String type, String content) {
        OrderConfig row = config.selectOrderConfigByType(type);
        if (row == null) {
            row = new OrderConfig();
            row.setType(type);
            row.setContent(content);
            assertEquals(1, config.insertOrderConfig(row));
        } else {
            row.setContent(content);
            assertEquals(1, config.updateOrderConfig(row));
        }
    }

    private OrderUser assertRegistration(String name, long vipId, long progress) {
        OrderUser user = newUser(name);
        assertEquals(RegistrationResult.SUCCESS, registration.register(user));
        assertNotNull(user.getId());
        assertEquals(vipId, user.getVipId());
        assertEquals(vipId, jdbc.queryForObject("SELECT vip_id FROM order_user WHERE id = ?", Long.class, user.getId()));
        assertEquals(progress, storedProgress(user));
        return user;
    }

    private OrderUser newUser(String name) {
        OrderUser user = new OrderUser();
        user.setUsername(name);
        user.setPhoneNumber("phone_" + name);
        user.setPassword("encoded_password");
        user.setTradePassword("encoded_trade_password");
        user.setInviteCode("REF77");
        return user;
    }

    private long storedProgress(OrderUser user) {
        return jdbc.queryForObject("SELECT task_progress FROM order_user WHERE id = ?", Long.class, user.getId());
    }

    private BigDecimal storedBalance(OrderUser user) {
        return jdbc.queryForObject("SELECT balance FROM order_user WHERE id = ?", BigDecimal.class, user.getId());
    }

    private int flowCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM goods_transaction_flow", Integer.class);
    }
}
