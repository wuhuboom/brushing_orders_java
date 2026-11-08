package com.order.api.service;

import com.order.member.domain.Goods;
import com.order.member.mapper.GoodsMapper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GoodsRandomSelectionMySqlTest {
    private static final String DATABASE = "codex_goods_random_test";
    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory sessions;

    @BeforeAll
    static void connectToIsolatedDatabase() throws Exception {
        String override = System.getProperty("goods.random.jdbcUrl");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (override != null) {
            // These tests recreate goods; only allow a dedicated loopback test database.
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
                    "Docker is unavailable; use -Dgoods.random.jdbcUrl for an isolated local MySQL");
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
        factory.setMapperLocations(new ClassPathResource("mapper/GoodsMapper.xml"));
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
        jdbc.execute("DROP TABLE IF EXISTS goods");
        jdbc.execute("""
                CREATE TABLE goods (
                    id BIGINT PRIMARY KEY, title VARCHAR(255), type_id BIGINT,
                    is_enabled CHAR(1), price DECIMAL(20,2), serial_number BIGINT,
                    image VARCHAR(500), sub_title VARCHAR(255), unit_price DECIMAL(20,2),
                    quantity INT, star_rating DECIMAL(10,2), rating DECIMAL(10,2),
                    description TEXT, create_by VARCHAR(64), create_time DATETIME
                ) ENGINE=InnoDB
                """);
    }

    @Test
    void samplesAcrossHighStartingIdsInsteadOfRepeatingTheFirstCheapProducts() {
        Set<Long> expectedIds = new HashSet<>();
        for (int index = 0; index < 64; index++) {
            long id = 1_000_001L + index * 2L;
            expectedIds.add(id);
            product(id, "0", 1 + index * 25);
        }

        Set<Long> seen = new HashSet<>();
        for (int attempt = 0; attempt < 12; attempt++) {
            List<Goods> rows = selectRandomGoods();
            assertEquals(8, rows.size());
            Set<Long> ids = ids(rows);
            assertEquals(8, ids.size(), "A response must not repeat a product");
            assertTrue(expectedIds.containsAll(ids));
            seen.addAll(ids);
            for (Goods goods : rows) {
                assertEquals("product-" + goods.getId(), goods.getTitle());
                long expectedPrice = 1 + (goods.getId() - 1_000_001L) / 2 * 25;
                assertEquals(0, BigDecimal.valueOf(expectedPrice).compareTo(goods.getPrice()));
            }
        }
        // Across 96 draws this threshold is comfortably below normal random coverage.
        assertTrue(seen.size() > 16, "Selections must reach beyond the first few IDs: " + seen);
    }

    @Test
    void returnsAllEnabledProductsWhenFewerThanEightExistEvenWithLargeIdGaps() {
        product(1_000_001L, "0", 1);
        product(5_000_000L, "0", 500);
        product(90_000_000L, "0", 2000);
        product(100_000_000L, "1", 1000);

        for (int attempt = 0; attempt < 5; attempt++) {
            List<Goods> rows = selectRandomGoods();
            assertEquals(3, rows.size());
            assertEquals(Set.of(1_000_001L, 5_000_000L, 90_000_000L), ids(rows));
            assertTrue(rows.stream().allMatch(goods -> "0".equals(goods.getIsEnabled())));
        }
    }

    @Test
    void returnsEmptyForEmptyCatalogOrOnlyDisabledProducts() {
        assertTrue(selectRandomGoods().isEmpty());
        product(1_000_001L, "1", 100);
        assertTrue(selectRandomGoods().isEmpty());
    }

    @Test
    void includesLowPricesAndCeilingAndPreservesOriginalDecimalPrices() {
        product(1_000_001L, "0", new BigDecimal("99.99"));
        product(1_000_002L, "0", new BigDecimal("100.00"));
        product(1_000_003L, "0", new BigDecimal("1999.99"));
        product(1_000_004L, "0", new BigDecimal("2000.00"));
        product(1_000_005L, "0", new BigDecimal("2000.01"));
        product(1_000_006L, "1", new BigDecimal("500.00"));
        product(1_000_007L, "0", (BigDecimal) null);
        product(1_000_008L, "0", new BigDecimal("1.00"));

        for (int attempt = 0; attempt < 5; attempt++) {
            List<Goods> rows = selectRandomGoods();
            assertEquals(5, rows.size());
            assertEquals(Set.of(1_000_001L, 1_000_002L, 1_000_003L, 1_000_004L, 1_000_008L), ids(rows));
            for (Goods goods : rows) {
                BigDecimal storedPrice = jdbc.queryForObject(
                        "SELECT price FROM goods WHERE id = ?", BigDecimal.class, goods.getId());
                assertEquals(0, goods.getPrice().compareTo(storedPrice));
            }
        }
    }

    @Test
    void returnsEmptyWhenAllEnabledProductsAreOutsideDisplayPriceRange() {
        product(1_000_001L, "0", new BigDecimal("2000.01"));
        product(1_000_002L, "0", 500000);
        assertTrue(selectRandomGoods().isEmpty());
    }

    private static List<Goods> selectRandomGoods() {
        // Separate sessions reproduce separate HTTP requests rather than a local session cache.
        try (SqlSession session = sessions.openSession()) {
            return session.getMapper(GoodsMapper.class).selectRandomGoods();
        }
    }

    private static Set<Long> ids(List<Goods> rows) {
        return rows.stream().map(Goods::getId).collect(Collectors.toSet());
    }

    private void product(long id, String enabled, int price) {
        product(id, enabled, BigDecimal.valueOf(price));
    }

    private void product(long id, String enabled, BigDecimal price) {
        jdbc.update("INSERT INTO goods(id, title, is_enabled, price) VALUES (?, ?, ?, ?)",
                id, "product-" + id, enabled, price);
    }
}
