package com.order.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.service.OrderApplicationService;
import com.order.api.service.OrderTradePolicyService;
import com.order.api.service.TradeConfigSnapshotService;
import com.order.common.i18n.ITranslationsService;
import com.order.member.mapper.*;
import com.order.member.service.IOrderSequenceManagerService;
import com.order.member.service.ITransactionService;
import com.order.member.service.impl.OrderConfigServiceImpl;
import com.order.system.domain.SysTimeZone;
import com.order.system.service.ISysTimeZoneService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.order.api.service.OrderErrorCodes.NO_SUITABLE_PRODUCT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real API controller -> transactional order service -> real MyBatis/MySQL reads and writes.
 * Global trade JSON is also read and parsed through the real configuration mapper/services.
 * Authentication is represented by the request userId; timezone and sequence are controlled
 * test inputs. The flow adapter writes to the same local transaction. No business mapper is
 * mocked, and no live account or production connection is used.
 */
class OrderProductMatchingApiMySqlTest {
    private static final String DATABASE = "codex_order_product_matching_test";
    private static final long USER_ID = 10L;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final AtomicReference<BigDecimal> LAST_TARGET = new AtomicReference<>();
    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;
    private static AnnotationConfigApplicationContext context;
    private static MockMvc mvc;

    @BeforeAll
    static void connectToIsolatedDatabaseAndStartApi() throws Exception {
        String override = System.getProperty("order.product.matching.jdbcUrl");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (override == null) {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Docker unavailable; set order.product.matching.jdbcUrl for an isolated local MySQL");
            mysql = new MySQLContainer("mysql:8.0.42")
                    .withDatabaseName(DATABASE).withUsername("order").withPassword("order");
            mysql.start();
            dataSource.setUrl(mysql.getJdbcUrl());
            dataSource.setUsername(mysql.getUsername());
            dataSource.setPassword(mysql.getPassword());
        } else {
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
        }
        jdbc = new JdbcTemplate(dataSource);
        assertEquals(DATABASE, jdbc.queryForObject("SELECT DATABASE()", String.class));
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.order.member.domain");
        factory.setMapperLocations(new ClassPathResource("mapper/OrderUserMapper.xml"),
                new ClassPathResource("mapper/GoodsMapper.xml"),
                new ClassPathResource("mapper/OrderInfoMapper.xml"),
                new ClassPathResource("mapper/OrderApiRequestMapper.xml"),
                new ClassPathResource("mapper/OrderBonusTableMapper.xml"),
                new ClassPathResource("mapper/OrderLinkMapper.xml"),
                new ClassPathResource("mapper/GoodsExtraCommissionSettingMapper.xml"),
                new ClassPathResource("mapper/OrderConfigMapper.xml"));
        factory.afterPropertiesSet();
        SqlSessionTemplate sessions = new SqlSessionTemplate(factory.getObject());
        GoodsMapper realGoodsMapper = sessions.getMapper(GoodsMapper.class);
        // Observe the input without stubbing the query or replacing its real database result.
        GoodsMapper observedGoodsMapper = (GoodsMapper) Proxy.newProxyInstance(
                GoodsMapper.class.getClassLoader(), new Class<?>[] { GoodsMapper.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("selectNearestPriceGoods")) {
                        LAST_TARGET.set((BigDecimal) arguments[0]);
                    }
                    try {
                        return method.invoke(realGoodsMapper, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
        ISysTimeZoneService timeZones = mock(ISysTimeZoneService.class);
        SysTimeZone zone = new SysTimeZone();
        zone.setTzName("Asia/Shanghai");
        when(timeZones.getActive()).thenReturn(zone);
        OrderConfigServiceImpl configs = new OrderConfigServiceImpl(
                sessions.getMapper(OrderConfigMapper.class), JSON, mock(ITranslationsService.class));
        OrderTradePolicyService policies = new OrderTradePolicyService(
                new TradeConfigSnapshotService(configs, timeZones, JSON));
        IOrderSequenceManagerService sequence = mock(IOrderSequenceManagerService.class);
        when(sequence.generateCode(anyString())).thenAnswer(call -> "LOCAL-MATCH-" + SEQUENCE.incrementAndGet());
        ITransactionService flows = mock(ITransactionService.class);
        when(flows.recordFlow(anyLong(), anyString(), any(), any(), anyString())).thenAnswer(call -> {
            jdbc.update("INSERT INTO test_transaction_flow(user_id,amount,balance_before,remark) VALUES (?,?,?,?)",
                    call.<Long>getArgument(0), call.<BigDecimal>getArgument(2),
                    call.<BigDecimal>getArgument(3), call.<String>getArgument(4));
            return null;
        });
        context = new AnnotationConfigApplicationContext();
        context.register(Transactions.class);
        context.registerBean(DataSource.class, () -> dataSource);
        context.registerBean(OrderApplicationService.class, () -> new OrderApplicationService(
                sessions.getMapper(OrderUserMapper.class), sessions.getMapper(OrderInfoMapper.class),
                sessions.getMapper(OrderApiRequestMapper.class), sessions.getMapper(OrderLinkMapper.class),
                sessions.getMapper(OrderBonusTableMapper.class), observedGoodsMapper,
                sessions.getMapper(GoodsExtraCommissionSettingMapper.class), policies, sequence, flows));
        context.refresh();
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(context.getBean(OrderApplicationService.class)))
                .setControllerAdvice(new OrderApiExceptionHandler())
                .build();
    }

    @AfterAll
    static void releaseOwnedResources() {
        if (context != null) context.close();
        if (mysql != null) mysql.stop();
    }

    @BeforeEach
    void createMinimalRealSchema() {
        for (String table : List.of("test_transaction_flow", "order_api_request", "order_info",
                "order_bonus_table", "order_link", "goods_extra_commission_setting", "goods",
                "order_user", "goods_member_level", "order_config")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("""
                CREATE TABLE order_user (
                    id BIGINT PRIMARY KEY, parent_id BIGINT, vip_id BIGINT,
                    balance DECIMAL(20,2), frozen_balance DECIMAL(20,2) NOT NULL DEFAULT 0,
                    task_progress BIGINT NOT NULL DEFAULT 0, is_banned CHAR(1) DEFAULT '1',
                    product_matching CHAR(1) DEFAULT '1', product_match_min DECIMAL(5,2),
                    product_match_max DECIMAL(5,2), version BIGINT NOT NULL DEFAULT 0, update_time DATETIME(3)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_member_level (
                    id BIGINT PRIMARY KEY, order_count_per_day BIGINT,
                    min_commission_rate DECIMAL(5,2), min_continuous_commission_rate DECIMAL(5,2),
                    max_continuous_commission_rate DECIMAL(5,2)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods (
                    id BIGINT PRIMARY KEY, title VARCHAR(255), image VARCHAR(255),
                    is_enabled CHAR(1), price DECIMAL(20,2)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_info (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, order_number VARCHAR(64), user_id BIGINT,
                    type CHAR(1), order_count BIGINT, amount DECIMAL(20,2),
                    rebate_percentage DECIMAL(5,2), rebate DECIMAL(20,2),
                    upper_rebate_percentage DECIMAL(5,2), upper_rebate DECIMAL(20,2),
                    status CHAR(1), expiry_time DATETIME(3), product_id BIGINT,
                    product_image VARCHAR(255), product_title VARCHAR(255),
                    extra_commission_id BIGINT, create_time DATETIME(3), remarks VARCHAR(255),
                    comment_id BIGINT, link_id BIGINT
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_api_request (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, operation_type VARCHAR(64),
                    request_id VARCHAR(64), result_type VARCHAR(20), result_id BIGINT, create_time DATETIME(3),
                    UNIQUE KEY uk_order_api_request_key(user_id,operation_type,request_id)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_bonus_table (
                    id BIGINT PRIMARY KEY, order_num BIGINT, user_id BIGINT, amount DECIMAL(20,2),
                    animation_duration BIGINT, display_duration BIGINT, distribution_type CHAR(1),
                    expiry_time DATETIME(3), push_type CHAR(1), create_time DATETIME(3),
                    is_distributed CHAR(1), is_received CHAR(1), received_time DATETIME(3),
                    distribution_time DATETIME(3), to_users VARCHAR(255)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_link (
                    id BIGINT PRIMARY KEY, link_order_id BIGINT, user_id BIGINT, order_count BIGINT,
                    commission_multiple INT, product_id BIGINT, price_type CHAR(1), price DECIMAL(20,2),
                    status CHAR(1), create_by VARCHAR(64), create_time DATETIME(3),
                    update_by VARCHAR(64), update_time DATETIME(3)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE goods_extra_commission_setting (
                    id BIGINT PRIMARY KEY, user_id BIGINT, order_count BIGINT, product_price DECIMAL(20,2),
                    amount DECIMAL(20,2), is_locked CHAR(1), status CHAR(1), create_time DATETIME(3)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE test_transaction_flow (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, amount DECIMAL(20,2),
                    balance_before DECIMAL(20,2), remark VARCHAR(255)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_config (
                    id BIGINT PRIMARY KEY, type VARCHAR(64), sort BIGINT, name VARCHAR(64),
                    content LONGTEXT, content_relative LONGTEXT, translations_id BIGINT, create_time DATETIME(3), update_time DATETIME(3)
                ) ENGINE=InnoDB
                """);
        jdbc.update("INSERT INTO order_config(id,type,content) VALUES (1,'trade','{}')");
        globalRange("30-60");
        jdbc.update("INSERT INTO goods_member_level VALUES (1,45,1,2,2)");
        jdbc.update("""
                INSERT INTO order_user(id,parent_id,vip_id,balance,product_matching,product_match_min,product_match_max)
                VALUES (10,0,1,1000,'0',70,80)
                """);
    }

    @Test
    void aPersonalTargetRangeStillAllowsACheaperCatalogProduct() throws Exception {
        product(200, "200.00", "0");

        sampleOrders(1, "personal-target-700-800-product-200-allowed",
                "1000.00", "700.00", "800.00", "200.00");
    }

    @Test
    void fixedPersonalPercentageOverridesRealGlobalTradeJson() throws Exception {
        jdbc.update("UPDATE order_user SET product_match_min=35.55,product_match_max=35.55 WHERE id=10");
        globalRange("50-50");
        product(200, "200.00", "0");
        product(355, "355.49", "0");
        product(356, "355.50", "0");
        product(357, "355.51", "0");
        product(500, "500.00", "0");

        sampleOrders(20, "personal-35.55-over-global-50",
                "1000.00", "355.50", "355.50", "355.50");
    }

    @Test
    void personalRandomTargetsStayInThePersonalPercentageRange() throws Exception {
        product(200, "200.00", "0");
        product(650, "650.00", "0");
        product(700, "700.00", "0");
        product(750, "750.00", "0");
        product(780, "780.00", "1");
        product(800, "800.00", "0");
        product(900, "900.00", "0");

        sampleOrders(20, "random-personal-targets",
                "1000.00", "700.00", "800.00", null);
    }

    @Test
    void disabledPersonalMatchingUsesTheRealFixedGlobalConfiguration() throws Exception {
        jdbc.update("UPDATE order_user SET product_matching='1',product_match_min=35.55,product_match_max=35.55 WHERE id=10");
        globalRange("50-50");
        product(355, "355.50", "0");
        product(499, "499.99", "0");
        product(500, "500.00", "0");
        product(501, "500.01", "0");

        sampleOrders(20, "personal-disabled-global-50",
                "1000.00", "500.00", "500.00", "500.00");
    }

    @Test
    void globalRandomTargetsUseTheUpdatedRealTradeJson() throws Exception {
        jdbc.update("UPDATE order_user SET product_matching='1' WHERE id=10");
        globalRange("35-45");
        product(200, "200.00", "0");
        product(349, "349.00", "0");
        product(400, "400.00", "0");
        product(450, "450.00", "0");
        product(750, "750.00", "0");

        sampleOrders(20, "random-global-targets",
                "1000.00", "350.00", "450.00", null);
    }

    @Test
    void legacyEnabledSwitchWithoutPersonalRatiosFallsBackToTheRealGlobalConfiguration() throws Exception {
        jdbc.update("UPDATE order_user SET product_match_min=NULL,product_match_max=NULL WHERE id=10");
        globalRange("50-50");
        product(200, "200.00", "0");
        product(500, "500.00", "0");
        product(700, "700.00", "0");

        sampleOrders(8, "legacy-personal-null-global-50",
                "1000.00", "500.00", "500.00", "500.00");
    }

    @Test
    void aGlobalTargetRangeStillAllowsACheaperCatalogProduct() throws Exception {
        jdbc.update("UPDATE order_user SET product_matching='1' WHERE id=10");
        product(200, "200.00", "0");

        sampleOrders(1, "global-target-300-600-product-200-allowed",
                "1000.00", "300.00", "600.00", "200.00");
    }

    @Test
    void aFixedEightyPercentTargetIncludesAnEightHundredProduct() throws Exception {
        jdbc.update("UPDATE order_user SET product_match_min=80,product_match_max=80 WHERE id=10");
        product(800, "800.00", "0");

        sampleOrders(4, "fixed-80-exact-product-800",
                "1000.00", "800.00", "800.00", "800.00");
    }

    @Test
    void fractionalTargetAmountsContinueToRoundDownToCents() throws Exception {
        jdbc.update("UPDATE order_user SET balance=906.25,product_match_min=70,product_match_max=70 WHERE id=10");
        product(634, "634.37", "0");
        product(635, "634.38", "0");

        sampleOrders(4, "906.25-times-70-percent-rounds-down",
                "906.25", "634.37", "634.37", "634.37");
    }

    @Test
    void aSubCentPercentageRangeStillUsesTheRoundedDownTarget() throws Exception {
        jdbc.update("UPDATE order_user SET balance=1,product_match_min=33.33,product_match_max=33.34 WHERE id=10");
        product(33, "0.33", "0");
        product(34, "0.34", "0");

        sampleOrders(4, "sub-cent-target-range-rounds-down",
                "1.00", "0.33", "0.33", "0.33");
    }

    @Test
    void disabledClosestProductsAreIgnoredAndTheBestEnabledCheaperProductIsReturned() throws Exception {
        jdbc.update("UPDATE order_user SET product_match_min=75,product_match_max=75 WHERE id=10");
        product(700, "700.00", "0");
        product(750, "750.00", "1");
        product(760, "760.00", "0");

        sampleOrders(1, "disabled-750-enabled-700",
                "1000.00", "750.00", "750.00", "700.00");
    }

    @Test
    void productsAboveTheActualTargetCannotCreateAnOrderOrFreezeFunds() throws Exception {
        jdbc.update("UPDATE order_user SET product_match_min=70,product_match_max=70 WHERE id=10");
        product(701, "700.01", "0");
        product(800, "800.00", "0");

        assertNoSuitableProduct("all-products-above-target", new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("700.00"), LAST_TARGET.get());
    }

    @Test
    void onlyDisabledProductsCannotCreateAnOrderOrFreezeFunds() throws Exception {
        product(200, "200.00", "1");
        product(750, "750.00", "1");

        assertNoSuitableProduct("only-disabled-products", new BigDecimal("1000.00"));
    }

    @Test
    void anEmptyCatalogCannotCreateAnOrderOrFreezeFunds() throws Exception {
        assertNoSuitableProduct("empty-catalog", new BigDecimal("1000.00"));
    }

    @Test
    void anIdempotentRetryReturnsTheSameProductWithoutReservingFundsTwice() throws Exception {
        jdbc.update("UPDATE order_user SET product_match_min=75,product_match_max=75 WHERE id=10");
        product(750, "750.00", "0");
        String requestId = "local-match-idempotent-retry";
        ObservedOrder first = createOrder(requestId);
        assertSuccessfulStoredOrder(first, new BigDecimal("1000.00"),
                new BigDecimal("750.00"), new BigDecimal("750.00"), new BigDecimal("750.00"));

        ObservedOrder retry = createOrder(requestId);

        assertEquals(first.body().path("data"), retry.body().path("data"));
        assertNull(retry.target(), "An idempotent retry must not select a second product");
        assertSuccessfulStoredOrder(new ObservedOrder(retry.body(), first.target()), new BigDecimal("1000.00"),
                new BigDecimal("750.00"), new BigDecimal("750.00"), new BigDecimal("750.00"));
    }

    private static void globalRange(String range) {
        String content = "{\"tradeTimeRange\":[\"00:00\",\"00:00\"],\"minTradeBalance\":1,"
                + "\"parentRebatePercentage\":20,\"matchRangePercentage\":\"" + range + "\"}";
        jdbc.update("UPDATE order_config SET content=? WHERE id=1", content);
    }

    private static void sampleOrders(int count, String label, String originalBalance,
            String targetMinimum, String targetMaximum, String exactProductPrice) throws Exception {
        BigDecimal balance = new BigDecimal(originalBalance);
        BigDecimal lower = new BigDecimal(targetMinimum);
        BigDecimal upper = new BigDecimal(targetMaximum);
        BigDecimal expectedPrice = exactProductPrice == null ? null : new BigDecimal(exactProductPrice);
        Set<BigDecimal> observedTargets = new TreeSet<>();
        Set<BigDecimal> observedPrices = new TreeSet<>();
        for (int attempt = 0; attempt < count; attempt++) {
            resetCreatedOrders(balance);
            ObservedOrder response = createOrder("local-match-sample-" + SEQUENCE.incrementAndGet());
            BigDecimal amount = assertSuccessfulStoredOrder(response, balance, lower, upper, expectedPrice);
            observedTargets.add(response.target());
            observedPrices.add(amount);
        }
        System.out.printf("MATCH_API scenario=%s samples=%d balance=%s targetBounds=[%s,%s] targets=%s actualPrices=%s%n",
                label, count, balance, lower, upper, observedTargets, observedPrices);
    }

    private static ObservedOrder createOrder(String requestId) throws Exception {
        LAST_TARGET.set(null);
        MvcResult result = mvc.perform(post("/api/order").requestAttr("userId", USER_ID)
                        .header("Idempotency-Key", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.resultType").value("ORDER"))
                .andReturn();
        return new ObservedOrder(JSON.readTree(result.getResponse().getContentAsString()), LAST_TARGET.get());
    }

    private static BigDecimal assertSuccessfulStoredOrder(ObservedOrder response, BigDecimal originalBalance,
            BigDecimal targetMinimum, BigDecimal targetMaximum, BigDecimal exactProductPrice) {
        assertNotNull(response.target(), "The real goods mapper must receive the matching target");
        assertTrue(response.target().compareTo(targetMinimum) >= 0,
                "Target " + response.target() + " is below " + targetMinimum);
        assertTrue(response.target().compareTo(targetMaximum) <= 0,
                "Target " + response.target() + " is above " + targetMaximum);
        JsonNode order = response.body().path("data");
        BigDecimal amount = order.path("amount").decimalValue().setScale(2);
        assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(amount.compareTo(response.target()) <= 0,
                "Returned product price " + amount + " is above target " + response.target());
        if (exactProductPrice != null) assertEquals(exactProductPrice, amount);
        long productId = order.path("productId").asLong();
        BigDecimal storedPrice = jdbc.queryForObject("SELECT price FROM goods WHERE id=?", BigDecimal.class, productId);
        assertEquals(0, amount.compareTo(storedPrice), "The API must preserve the actual catalog price");
        assertEquals("0", jdbc.queryForObject("SELECT is_enabled FROM goods WHERE id=?", String.class, productId));
        assertEquals("Local product " + productId, order.path("productTitle").asText());
        assertEquals("/local/" + productId + ".png", order.path("productImage").asText());
        assertEquals(amount, jdbc.queryForObject("SELECT amount FROM order_info WHERE id=?", BigDecimal.class,
                order.path("id").asLong()));
        assertEquals(originalBalance.subtract(amount), jdbc.queryForObject(
                "SELECT balance FROM order_user WHERE id=10", BigDecimal.class));
        assertEquals(amount, jdbc.queryForObject("SELECT frozen_balance FROM order_user WHERE id=10", BigDecimal.class));
        assertEquals(0L, jdbc.queryForObject("SELECT task_progress FROM order_user WHERE id=10", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM order_info", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM order_api_request", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM test_transaction_flow", Long.class));
        return amount;
    }

    private static void assertNoSuitableProduct(String requestId, BigDecimal originalBalance) throws Exception {
        LAST_TARGET.set(null);
        mvc.perform(post("/api/order").requestAttr("userId", USER_ID)
                        .header("Idempotency-Key", requestId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(NO_SUITABLE_PRODUCT));
        assertUnchangedFundsAndNoOrder(originalBalance);
        System.out.printf("MATCH_API scenario=%s target=%s result=NO_SUITABLE_PRODUCT unchangedBalance=%s%n",
                requestId, LAST_TARGET.get(), originalBalance);
    }

    private static void resetCreatedOrders(BigDecimal originalBalance) {
        jdbc.update("DELETE FROM test_transaction_flow");
        jdbc.update("DELETE FROM order_api_request");
        jdbc.update("DELETE FROM order_info");
        jdbc.update("UPDATE order_user SET balance=?,frozen_balance=0,task_progress=0,version=0 WHERE id=10", originalBalance);
    }

    private static void product(long id, String price, String enabled) {
        jdbc.update("INSERT INTO goods(id,title,image,is_enabled,price) VALUES (?,?,?,?,?)",
                id, "Local product " + id, "/local/" + id + ".png", enabled, new BigDecimal(price));
    }

    private static void assertUnchangedFundsAndNoOrder(BigDecimal balance) {
        assertEquals(balance, jdbc.queryForObject("SELECT balance FROM order_user WHERE id=10", BigDecimal.class));
        assertEquals(new BigDecimal("0.00"), jdbc.queryForObject("SELECT frozen_balance FROM order_user WHERE id=10", BigDecimal.class));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM order_info", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM order_api_request", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM test_transaction_flow", Long.class));
    }

    private record ObservedOrder(JsonNode body, BigDecimal target) { }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {
        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
