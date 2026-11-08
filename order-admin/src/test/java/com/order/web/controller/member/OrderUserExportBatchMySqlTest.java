package com.order.web.controller.member;

import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.LongStream;
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
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OrderUserExportBatchMySqlTest {
    private static final String DATABASE = "codex_order_user_export_batch_test";
    private static MySQLContainer mysql;
    private static JdbcTemplate jdbc;
    private static SqlSessionFactory sessions;

    @BeforeAll
    static void connectToAnIsolatedDatabase() throws Exception {
        String override = System.getProperty("order.user.export.batch.jdbcUrl");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (override == null) {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Docker unavailable; set order.user.export.batch.jdbcUrl for isolated local MySQL");
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
        factory.setMapperLocations(new ClassPathResource("mapper/OrderUserMapper.xml"));
        factory.afterPropertiesSet();
        sessions = factory.getObject();
        assertNotNull(sessions);
    }

    @AfterAll
    static void stopOwnedContainer() {
        if (mysql != null) mysql.stop();
    }

    @BeforeEach
    void createMinimalReportingSchema() throws IOException {
        for (String table : List.of("order_user", "goods_member_level", "order_login_log",
                "order_withdrawal", "goods_recharge_record", "goods_transaction_flow",
                "order_bonus_table", "order_info")) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        // Reuse the real user-column schema, without importing snapshot data or other tables.
        Path snapshot = Path.of("sql", "init", "order_base_20260803.sql");
        if (!Files.exists(snapshot)) snapshot = Path.of("..", "sql", "init", "order_base_20260803.sql");
        StringBuilder userSchema = new StringBuilder();
        try (var reader = Files.newBufferedReader(snapshot)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (userSchema.length() == 0 && !line.startsWith("CREATE TABLE `order_user` (")) continue;
                userSchema.append(line).append('\n');
                if (line.endsWith(";")) break;
            }
        }
        assertTrue(userSchema.toString().startsWith("CREATE TABLE `order_user` ("));
        assertTrue(userSchema.toString().endsWith(";\n"));
        jdbc.execute(userSchema.toString());
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/mysql/V2026_11_08_00__member_product_match_ratios.sql"))
                .execute(jdbc.getDataSource());
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
                    product_match_enabled CHAR(1), product_match_min DECIMAL(20,2), product_match_max DECIMAL(20,2)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE order_login_log (
                    id BIGINT PRIMARY KEY, user_id BIGINT, ip VARCHAR(45), address VARCHAR(255),
                    create_time DATETIME(3), INDEX idx_user_time(user_id, create_time)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("CREATE TABLE order_withdrawal (id BIGINT PRIMARY KEY, user_id BIGINT, amount DECIMAL(20,2), status CHAR(1)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE goods_recharge_record (id BIGINT PRIMARY KEY, user_id BIGINT, amount DECIMAL(20,2), status CHAR(1)) ENGINE=InnoDB");
        jdbc.execute("""
                CREATE TABLE goods_transaction_flow (
                    id BIGINT PRIMARY KEY, user_id BIGINT, transaction_type VARCHAR(16),
                    transaction_amount DECIMAL(20,2), created_time DATETIME,
                    INDEX idx_user_type(user_id, transaction_type)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("CREATE TABLE order_info (id BIGINT PRIMARY KEY, user_id BIGINT, status CHAR(1)) ENGINE=InnoDB");
        jdbc.execute("""
                CREATE TABLE order_bonus_table (
                    id BIGINT PRIMARY KEY, user_id BIGINT, order_num BIGINT, amount DECIMAL(20,2),
                    is_distributed CHAR(1), is_received CHAR(1), expiry_time DATETIME(3), create_time DATETIME(3)
                ) ENGINE=InnoDB
                """);
        jdbc.update("INSERT INTO goods_member_level(id,name,order_count_per_day,task_count_per_day,withdraw_count_per_day,withdraw_limit_per_day) VALUES (1,'VIP 1',40,2,5,1000),(2,'VIP 2',50,3,10,2000)");
    }

    @Test
    void keysetBatchesCoverTheFixedHighWaterWithoutGapsOrDuplicates() {
        List<Object[]> members = LongStream.rangeClosed(1, 1003).mapToObj(id -> new Object[] {
                id, "member-" + id, 1L, 0L, "secret-hash", "2026-11-01 12:00:00"
        }).toList();
        jdbc.batchUpdate("INSERT INTO order_user(id,username,vip_id,parent_id,password,create_time) VALUES (?,?,?,?,?,?)", members);
        long maxId;
        try (SqlSession session = sessions.openSession()) {
            maxId = session.getMapper(OrderUserMapper.class).selectOrderUserExportMaxId();
        }
        assertEquals(1003L, maxId);
        member(1004L, "created-after-export-start");
        jdbc.update("DELETE FROM order_user WHERE id = 405");

        List<Long> actual = exportIds(new OrderUser(), maxId, 200);

        assertEquals(LongStream.rangeClosed(1, 1003).filter(id -> id != 405).boxed().toList(), actual);
        assertEquals(actual.size(), actual.stream().distinct().count());
        assertFalse(actual.contains(1004L));
    }

    @Test
    void exactKeywordPriorityRemainsGlobalAcrossCursorBatches() {
        member(1, "target-prefix");
        member(2, "prefix-target");
        member(9, "target");
        OrderUser query = new OrderUser();
        query.setKeyword("target");
        assertEquals(List.of(9L), exportIds(query, 9L, 1));
        assertEquals(List.of(9L), batch(query, 2L, 9L, 1).stream().map(OrderUser::getId).toList());

        jdbc.update("DELETE FROM order_user WHERE id = 9");
        assertEquals(List.of(1L, 2L), exportIds(query, 9L, 1));
    }

    @Test
    void batchQueriesKeepAllExistingListFiltersIncludingLatestLoginAndDateBounds() {
        member(90, "supervisor");
        jdbc.update("UPDATE order_user SET invite_code='PARENT90' WHERE id=90");
        for (long id = 1; id <= 6; id++) member(id, "member-" + id);
        jdbc.update("""
                UPDATE order_user SET parent_id=90, phone_number=CONCAT('001',id),
                    invite_code=CONCAT('INV',id), vip_id=IF(MOD(id,2)=0,2,1),
                    balance=id*10-20, reputation_score=id*10, work_limit=id*100,
                    gender=IF(MOD(id,2)=0,'1','0'), create_time=DATE_ADD('2026-11-01',INTERVAL id DAY),
                    is_enabled=IF(MOD(id,2)=0,'1','0'), allow_invite=IF(MOD(id,2)=0,'1','0'),
                    is_frozen=IF(MOD(id,2)=0,'1','0'), is_fake=IF(MOD(id,2)=0,'1','0'),
                    is_banned=IF(MOD(id,2)=0,'1','0'), is_withdrawal_notification=IF(MOD(id,2)=0,'1','0'),
                    product_matching=IF(MOD(id,2)=0,'1','0'), account_status=IF(MOD(id,2)=0,'1','0'),
                    transaction_status=IF(MOD(id,2)=0,'1','0'), withdrawal_status=IF(MOD(id,2)=0,'1','0'),
                    assist_withdrawal_status=IF(MOD(id,2)=0,'1','0'), deposit_block_withdrawal=IF(MOD(id,2)=0,'1','0'),
                    web3_auth_enabled=IF(MOD(id,2)=0,'1','0'), is_invalid=IF(MOD(id,2)=0,'1','0'),
                    is_activity=IF(MOD(id,2)=0,'1','0'), verify_identity_before_task=IF(MOD(id,2)=0,'1','0'),
                    user_contract_enabled=IF(MOD(id,2)=0,'1','0'), user_contract_signed=IF(MOD(id,2)=0,'1','0'),
                    formal_contract_enabled=IF(MOD(id,2)=0,'1','0'), formal_contract_signed=IF(MOD(id,2)=0,'1','0')
                WHERE id < 90
                """);
        jdbc.update("INSERT INTO order_login_log VALUES (1,1,'old-ip','old','2026-11-01'),(2,1,'10.0.0.1','new','2026-11-03'),(3,2,'10.0.0.2','two','2026-11-04')");
        Map<String, Consumer<OrderUser>> filters = new LinkedHashMap<>();
        filters.put("username", q -> q.setUsername("member-1"));
        filters.put("phone", q -> q.setPhoneNumber("0012"));
        filters.put("id", q -> q.setId(3L));
        filters.put("vip", q -> q.setVipId(2L));
        filters.put("parent username", q -> q.setParentUsername("super"));
        filters.put("parent invite", q -> q.setParentInviteCode("PARENT90"));
        filters.put("username inclusion", q -> q.setUsernameList("member-1,member-3"));
        filters.put("username exclusion", q -> q.setUsernameList("__exclude__:member-1,member-3"));
        filters.put("balance range", q -> { q.setBalanceMin(BigDecimal.ZERO); q.setBalanceMax(new BigDecimal("30")); });
        filters.put("reputation range", q -> { q.setReputationMin(20L); q.setReputationMax(50L); });
        filters.put("work limit", q -> q.setWorkLimit(new BigDecimal("300")));
        filters.put("latest login", q -> q.setLastLoginIp("10.0.0"));
        filters.put("old login excluded", q -> q.setLastLoginIp("old-ip"));
        filters.put("keyword parent invite", q -> q.setKeyword("PARENT90"));
        filters.put("keyword latest IP", q -> q.setKeyword("10.0.0.1"));
        filters.put("keyword partial fallback", q -> q.setKeyword("member-"));
        filters.put("created bounds", q -> { q.getParams().put("beginTime", "2026-11-03"); q.getParams().put("endTime", "2026-11-05"); });
        filters.put("gender", q -> q.setGender("1"));
        filters.put("enabled", q -> q.setIsEnabled("1"));
        filters.put("invite", q -> q.setAllowInvite("1"));
        filters.put("frozen", q -> q.setIsFrozen("1"));
        filters.put("fake", q -> q.setIsFake("1"));
        filters.put("banned", q -> q.setIsBanned("1"));
        filters.put("notification", q -> q.setIsWithdrawalNotification("1"));
        filters.put("matching", q -> q.setProductMatching("1"));
        filters.put("account", q -> q.setAccountStatus("1"));
        filters.put("transaction", q -> q.setTransactionStatus("1"));
        filters.put("withdrawal", q -> q.setWithdrawalStatus("1"));
        filters.put("assist withdrawal", q -> q.setAssistWithdrawalStatus("1"));
        filters.put("deposit restriction", q -> q.setDepositBlockWithdrawal("1"));
        filters.put("web3", q -> q.setWeb3AuthEnabled("1"));
        filters.put("invalid", q -> q.setIsInvalid("1"));
        filters.put("activity", q -> q.setIsActivity("1"));
        filters.put("verify identity", q -> q.setVerifyIdentityBeforeTask("1"));
        filters.put("user contract enabled", q -> q.setUserContractEnabled("1"));
        filters.put("user contract signed", q -> q.setUserContractSigned("1"));
        filters.put("formal contract enabled", q -> q.setFormalContractEnabled("1"));
        filters.put("formal contract signed", q -> q.setFormalContractSigned("1"));
        for (var filter : filters.entrySet()) {
            OrderUser query = new OrderUser();
            filter.getValue().accept(query);
            List<Long> expected;
            try (SqlSession session = sessions.openSession()) {
                expected = session.getMapper(OrderUserMapper.class).selectOrderUserList(query).stream()
                        .map(OrderUser::getId).sorted().toList();
            }
            assertEquals(expected, exportIds(query, 90L, 2), filter.getKey());
        }
        OrderUser oldIp = new OrderUser();
        oldIp.setLastLoginIp("old-ip");
        assertTrue(exportIds(oldIp, 90L, 2).isEmpty());
    }

    @Test
    void projectionExcludesLargeSecretsAndPreservesFinancialAndLoginReporting() {
        member(7, "member-seven");
        member(8, "member-eight");
        member(9, "child-one");
        member(10, "child-two");
        jdbc.update("UPDATE order_user SET parent_id=7 WHERE id IN (9,10)");
        jdbc.update("UPDATE order_user SET balance=-12.34, frozen_balance=20.50, task_progress=4, identity_number='identity-secret', trade_password='trade-secret', user_contract_content=REPEAT('contract-secret-',4000), formal_contract_content=REPEAT('formal-secret-',4000) WHERE id=7");
        jdbc.update("INSERT INTO order_withdrawal VALUES (1,7,10.25,'0'),(2,7,20.50,'1'),(3,8,999,'0')");
        jdbc.update("INSERT INTO goods_recharge_record VALUES (1,7,30.25,'0'),(2,7,40.50,'1'),(3,8,999,'0')");
        jdbc.update("INSERT INTO order_login_log VALUES (1,7,'old','old','2026-11-01'),(2,7,'latest','latest address','2026-11-04')");
        jdbc.update("INSERT INTO goods_transaction_flow VALUES (1,7,'fy',3000000000.12,'2020-01-01'),(2,7,'fy',4.25,'2026-11-01'),(3,7,'xjfy',999,'2026-11-01'),(4,8,'fy',999,'2026-11-01')");
        jdbc.update("INSERT INTO order_bonus_table VALUES (1,7,3,7.50,'0','0',NULL,'2026-11-01'),(2,7,5,15.75,'1','1','2099-01-01','2026-11-01'),(3,7,2,21.25,'1','0','2020-01-01','2026-10-01')");

        OrderUser projected = batch(new OrderUser(), 0L, 7L, 1).get(0);
        assertNull(projected.getPassword());
        assertNull(projected.getTradePassword());
        assertNull(projected.getIdentityNumber());
        assertNull(projected.getIdentityFrontImage());
        assertNull(projected.getUserContractContent());
        assertNull(projected.getFormalContractContent());
        assertEquals(new BigDecimal("-12.34"), projected.getBalance());
        assertEquals("latest", projected.getLastLoginIp());
        assertEquals("latest address", projected.getLastLoginAddress());
        assertEquals(2, projected.getDirectChildrenCount());
        assertEquals("VIP 1", projected.getMemberLevel().getName());
        try (SqlSession session = sessions.openSession()) {
            OrderUserMapper mapper = session.getMapper(OrderUserMapper.class);
            OrderUser original = mapper.selectOrderUserById(7L);
            assertEquals(original.getTotalWithdrawalAmount(), projected.getTotalWithdrawalAmount());
            assertEquals(original.getTotalRechargeAmount(), projected.getTotalRechargeAmount());
            assertEquals(new BigDecimal("30.75"), projected.getTotalWithdrawalAmount());
            assertEquals(new BigDecimal("70.75"), projected.getTotalRechargeAmount());
            OrderUser financial = mapper.selectOrderUserExportStatistics(List.of(7L)).get(0);
            OrderUser profile = mapper.selectUserProfileById(7L);
            assertEquals(profile.getTotalCommission(), financial.getTotalCommission());
            assertEquals(profile.getLuckyBonus(), financial.getLuckyBonus());
            assertEquals(profile.getPendingBonusAmount(), financial.getPendingBonusAmount());
            assertEquals(new BigDecimal("3000000004.37"), financial.getTotalCommission());
            assertEquals(new BigDecimal("7.50"), financial.getLuckyBonus());
            assertEquals(new BigDecimal("21.25"), financial.getPendingBonusAmount());
        }
    }

    @Test
    void personalMatchRatiosPersistAndStaySeparateFromVipLevelRatios() {
        member(7, "personal-match-member");
        jdbc.update("UPDATE goods_member_level SET product_match_min=30, product_match_max=60 WHERE id=1");
        try (SqlSession session = sessions.openSession(true)) {
            OrderUserMapper mapper = session.getMapper(OrderUserMapper.class);
            OrderUser before = mapper.selectOrderUserById(7L);
            assertNull(before.getProductMatchMin());
            assertNull(before.getProductMatchMax());

            OrderUser update = new OrderUser();
            update.setId(7L);
            update.setVersion(before.getVersion());
            update.setProductMatching("0");
            update.setProductMatchMin(new BigDecimal("70.25"));
            update.setProductMatchMax(new BigDecimal("80.75"));
            assertEquals(1, mapper.updateOrderUser(update));
            assertEquals(0, mapper.updateOrderUser(update), "Stale settings must not overwrite newer edits");

            OrderUser query = new OrderUser();
            query.setId(7L);
            for (OrderUser loaded : List.of(mapper.selectOrderUserById(7L),
                    mapper.selectUserProfileById(7L), mapper.selectOrderUserList(query).get(0),
                    mapper.selectOrderTaskUserById(7L))) {
                assertEquals("0", loaded.getProductMatching());
                assertEquals(new BigDecimal("70.25"), loaded.getProductMatchMin());
                assertEquals(new BigDecimal("80.75"), loaded.getProductMatchMax());
            }
            OrderUser member = mapper.selectOrderUserById(7L);
            assertEquals(new BigDecimal("30.00"), member.getMemberLevel().getProductMatchMin());
            assertEquals(new BigDecimal("60.00"), member.getMemberLevel().getProductMatchMax());
        }
    }

    @Test
    void latestLoginTiesUseTheHighestIdForFilteringKeywordAndExportedValues() {
        member(7, "member-seven");
        jdbc.update("INSERT INTO order_login_log VALUES (100,7,'earlier-id','earlier address','2026-11-01 12:00:00.123'),(101,7,'latest-id','latest address','2026-11-01 12:00:00.123')");
        OrderUser query = new OrderUser();
        query.setLastLoginIp("latest-id");
        OrderUser exported = batch(query, 0L, 7L, 500).get(0);
        assertEquals("latest-id", exported.getLastLoginIp());
        assertEquals("latest address", exported.getLastLoginAddress());
        try (SqlSession session = sessions.openSession()) {
            assertEquals("latest-id", session.getMapper(OrderUserMapper.class)
                    .selectOrderUserList(query).get(0).getLastLoginIp());
        }
        query.setLastLoginIp("earlier-id");
        assertTrue(batch(query, 0L, 7L, 500).isEmpty());
        query.setLastLoginIp(null);
        query.setKeyword("latest-id");
        assertEquals(List.of(7L), exportIds(query, 7L, 1));
        query.setKeyword("earlier-id");
        assertTrue(exportIds(query, 7L, 1).isEmpty());
    }

    @Test
    void anEmptyDatabaseHasNoExportRowsOrHighWater() {
        try (SqlSession session = sessions.openSession()) {
            assertNull(session.getMapper(OrderUserMapper.class).selectOrderUserExportMaxId());
        }
        assertTrue(batch(new OrderUser(), 0L, 999L, 500).isEmpty());
    }

    private static void member(long id, String username) {
        jdbc.update("INSERT INTO order_user(id,username,vip_id,parent_id,password,create_time) VALUES (?,?,1,0,'login-secret','2026-11-01 12:00:00')", id, username);
    }

    private static List<OrderUser> batch(OrderUser query, long afterId, long maxId, int size) {
        OrderUser copy = new OrderUser();
        org.springframework.beans.BeanUtils.copyProperties(query, copy);
        copy.setParams(new HashMap<>(query.getParams()));
        copy.getParams().put("exportAfterId", afterId);
        copy.getParams().put("exportMaxId", maxId);
        copy.getParams().put("exportBatchSize", size);
        try (SqlSession session = sessions.openSession()) {
            return session.getMapper(OrderUserMapper.class).selectOrderUserExportBatch(copy);
        }
    }

    private static List<Long> exportIds(OrderUser query, long maxId, int size) {
        List<Long> result = new ArrayList<>();
        long afterId = 0L;
        while (true) {
            List<OrderUser> batch = batch(query, afterId, maxId, size);
            assertTrue(batch.size() <= size);
            if (batch.isEmpty()) return result;
            for (OrderUser user : batch) {
                assertTrue(user.getId() > afterId && user.getId() <= maxId);
                afterId = user.getId();
                result.add(afterId);
            }
        }
    }
}
