package com.brushing.member.mapper;

import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.vo.UserLevel;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMemberUserMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws IOException {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("OrderMemberUser", OrderMemberUser.class);
        configuration.getTypeAliasRegistry().registerAlias("OrderMemberLevel", OrderMemberLevel.class);
        configuration.getTypeAliasRegistry().registerAlias("UserLevel", UserLevel.class);

        String resource = "mapper/OrderMemberUserMapper.xml";
        try (InputStream inputStream = OrderMemberUserMapperSqlTest.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(inputStream);
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void userListProjectionDoesNotRunPerUserFinancialOrSubordinateQueries() {
        String sql = sql("selectOrderMemberUserList", new OrderMemberUser());

        assertTrue(sql.contains("from order_member_user u"));
        assertTrue(sql.contains("left join order_member_level l on u.level_id = l.id"));
        assertTrue(sql.contains("left join order_member_user parent_user on u.parent_id = parent_user.id"));
        assertFalse(sql.contains("from order_topup"));
        assertFalse(sql.contains("from order_withdrawal"));
        assertFalse(sql.contains("find_in_set(u.id"));
        assertFalse(sql.contains("direct_sub_count"));
        assertFalse(sql.contains("all_sub_count"));
        assertFalse(sql.contains("as max_level_price"));
        assertFalse(sql.contains("as next_level_price"));
    }

    @Test
    void pageHelperManualCountUsesTheExactListFiltersAndNoProjectionJoins() {
        OrderMemberUser filter = new OrderMemberUser();
        filter.setUsername("alice");
        filter.setInviteCode("INVITE");
        filter.setPhone("1234");
        filter.setIsReal("N");
        filter.setLevelId(2L);
        filter.setAgentUserId(88L);
        filter.setForceNoResult("1");
        filter.getParams().put("beginTime", "2026-08-01");
        filter.getParams().put("endTime", "2026-08-12");

        String listSql = sql("selectOrderMemberUserList", filter);
        String countSql = sql("selectOrderMemberUserList_COUNT", filter);

        assertTrue(countSql.startsWith("select count(*) from order_member_user u where "));
        assertFalse(countSql.contains("order_member_level"));
        assertFalse(countSql.contains("order_topup"));
        assertFalse(countSql.contains("order_withdrawal"));
        assertFalse(countSql.contains("parent_user"));
        assertEquals(outerWhere(listSql).replace(" order by u.create_time desc", ""),
                outerWhere(countSql));
        assertTrue(countSql.contains("right(u.phone, 4) = ?"));
        assertTrue(countSql.contains("find_in_set(?, u.ancestors)"));
        assertTrue(countSql.endsWith("and 1 = 0"));
    }

    @Test
    void pageHelperActuallyExecutesTheManualCountMappedStatement() throws Throwable {
        AtomicReference<String> executedCountStatementId = new AtomicReference<>();
        AtomicReference<String> executedCountSql = new AtomicReference<>();
        AtomicReference<String> executedPageSql = new AtomicReference<>();

        Executor executor = (Executor) Proxy.newProxyInstance(
                Executor.class.getClassLoader(),
                new Class<?>[]{Executor.class},
                (proxy, method, args) -> {
                    if ("createCacheKey".equals(method.getName())) {
                        return new CacheKey();
                    }
                    if ("query".equals(method.getName())) {
                        MappedStatement statement = (MappedStatement) args[0];
                        BoundSql boundSql = args.length == 6
                                ? (BoundSql) args[5]
                                : statement.getBoundSql(args[1]);
                        if (statement.getId().endsWith("_COUNT")) {
                            executedCountStatementId.set(statement.getId());
                            executedCountSql.set(normalize(boundSql.getSql()));
                            return List.of(23L);
                        }
                        executedPageSql.set(normalize(boundSql.getSql()));
                        return Collections.emptyList();
                    }
                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }
                    return primitiveDefault(method.getReturnType());
                });

        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql");
        pageInterceptor.setProperties(properties);

        MappedStatement listStatement = configuration.getMappedStatement(
                "com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserList");
        OrderMemberUser filter = new OrderMemberUser();
        filter.setUsername("alice");
        BoundSql listBoundSql = listStatement.getBoundSql(filter);
        Invocation invocation = new Invocation(
                executor,
                Executor.class.getMethod(
                        "query",
                        MappedStatement.class,
                        Object.class,
                        RowBounds.class,
                        ResultHandler.class,
                        CacheKey.class,
                        BoundSql.class),
                new Object[]{
                        listStatement,
                        filter,
                        RowBounds.DEFAULT,
                        Executor.NO_RESULT_HANDLER,
                        new CacheKey(),
                        listBoundSql
                });

        PageHelper.startPage(1, 10);
        try {
            pageInterceptor.intercept(invocation);
        } finally {
            PageHelper.clearPage();
        }

        assertEquals(
                "com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserList_COUNT",
                executedCountStatementId.get());
        assertTrue(executedCountSql.get().startsWith(
                "select count(*) from order_member_user u where u.username = ?"));
        assertFalse(executedCountSql.get().contains("order_member_level"));
        assertTrue(executedPageSql.get().endsWith("limit ?"));
    }

    @Test
    void aggregateSortUsesGlobalCandidateAggregateAndAProjectionFreeManualCount() {
        OrderMemberUser filter = aggregateSortFilter("total_recharge", "desc");

        String sortedIdsSql = sql("selectOrderMemberUserAggregateSortedIds", filter);
        String countSql = sql("selectOrderMemberUserAggregateSortedIds_COUNT", filter);

        assertTrue(sortedIdsSql.startsWith("with candidate as ( select u.id from order_member_user u"));
        assertTrue(sortedIdsSql.contains("from order_topup topup"));
        assertTrue(sortedIdsSql.contains("order by coalesce(aggregate_sort.total_recharge, 0) desc"));
        assertTrue(sortedIdsSql.endsWith(", candidate.id desc"));
        assertFalse(sortedIdsSql.contains("${"));
        assertEquals("select count(*) from order_member_user u", countSql);
    }

    @Test
    void aggregateSortPageHelperActuallyUsesItsLightweightCount() throws Throwable {
        AtomicReference<String> executedCountStatementId = new AtomicReference<>();
        AtomicReference<String> executedCountSql = new AtomicReference<>();
        AtomicReference<String> executedPageSql = new AtomicReference<>();
        Executor executor = capturingExecutor(
                executedCountStatementId, executedCountSql, executedPageSql, List.of(2355L));

        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql");
        pageInterceptor.setProperties(properties);

        MappedStatement statement = configuration.getMappedStatement(
                "com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserAggregateSortedIds");
        OrderMemberUser filter = aggregateSortFilter("all_sub_count", "asc");
        BoundSql boundSql = statement.getBoundSql(filter);
        Invocation invocation = new Invocation(
                executor,
                Executor.class.getMethod(
                        "query", MappedStatement.class, Object.class, RowBounds.class,
                        ResultHandler.class, CacheKey.class, BoundSql.class),
                new Object[]{
                        statement, filter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER,
                        new CacheKey(), boundSql
                });

        PageHelper.startPage(1, 10);
        try {
            pageInterceptor.intercept(invocation);
        } finally {
            PageHelper.clearPage();
        }

        assertEquals(
                "com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserAggregateSortedIds_COUNT",
                executedCountStatementId.get());
        assertEquals("select count(*) from order_member_user u", executedCountSql.get());
        assertFalse(executedCountSql.get().contains("json_table"));
        assertTrue(executedPageSql.get().contains("json_quote(ifnull(descendant.ancestors, ''))"));
        assertTrue(executedPageSql.get().endsWith("limit ?"));
    }

    @Test
    void fixedLegacyLevelExpressionsPassThroughPageHelperAndKeepTheLightweightCount()
            throws Throwable {
        List<String> fixedOrderByExpressions = List.of(
                "(SELECT MAX(member_sort_level.price) "
                        + "FROM order_member_level member_sort_level "
                        + "WHERE member_sort_level.price <= u.balance) desc, u.id desc",
                "(SELECT MIN(member_sort_next_level.price) "
                        + "FROM order_member_level member_sort_next_level "
                        + "WHERE member_sort_next_level.price > COALESCE("
                        + "(SELECT MAX(member_sort_current_level.price) "
                        + "FROM order_member_level member_sort_current_level "
                        + "WHERE member_sort_current_level.price <= u.balance), -1)) asc, u.id desc");

        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql");
        pageInterceptor.setProperties(properties);

        MappedStatement statement = configuration.getMappedStatement(
                "com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserList");
        for (String orderBy : fixedOrderByExpressions) {
            AtomicReference<String> executedCountStatementId = new AtomicReference<>();
            AtomicReference<String> executedCountSql = new AtomicReference<>();
            AtomicReference<String> executedPageSql = new AtomicReference<>();
            Executor executor = capturingExecutor(
                    executedCountStatementId, executedCountSql, executedPageSql, List.of(2355L));
            OrderMemberUser filter = new OrderMemberUser();
            BoundSql boundSql = statement.getBoundSql(filter);
            Invocation invocation = new Invocation(
                    executor,
                    Executor.class.getMethod(
                            "query", MappedStatement.class, Object.class, RowBounds.class,
                            ResultHandler.class, CacheKey.class, BoundSql.class),
                    new Object[]{
                            statement, filter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER,
                            new CacheKey(), boundSql
                    });

            PageHelper.startPage(1, 10).setUnsafeOrderBy(orderBy);
            try {
                pageInterceptor.intercept(invocation);
            } finally {
                PageHelper.clearPage();
            }

            assertEquals(
                    "com.brushing.member.mapper.OrderMemberUserMapper.selectOrderMemberUserList_COUNT",
                    executedCountStatementId.get());
            assertEquals("select count(*) from order_member_user u", executedCountSql.get());
            assertTrue(executedPageSql.get().contains("order by " + normalize(orderBy)), orderBy);
            assertTrue(executedPageSql.get().endsWith("limit ?"), orderBy);
        }
    }

    @Test
    void everyLegacyAggregateSortAliasHasAWhitelistedFixedSqlBranch() {
        Map<String, String> expectedFragments = Map.of(
                "total_recharge", "aggregate_sort.total_recharge",
                "total_withdraw", "aggregate_sort.total_withdraw",
                "diff_amount", "topup_sort.total_recharge",
                "withdraw_frozen_amount", "aggregate_sort.withdraw_frozen_amount",
                "today_withdraw_count", "aggregate_sort.today_withdraw_count",
                "direct_sub_count", "aggregate_sort.direct_sub_count",
                "all_sub_count", "aggregate_sort.all_sub_count");

        for (Map.Entry<String, String> entry : expectedFragments.entrySet()) {
            String statementSql = sql(
                    "selectOrderMemberUserAggregateSortedIds",
                    aggregateSortFilter(entry.getKey(), "asc"));
            assertTrue(statementSql.contains(entry.getValue()), entry.getKey());
            assertTrue(statementSql.endsWith("asc , candidate.id desc"), entry.getKey());
            if ("all_sub_count".equals(entry.getKey())) {
                assertTrue(statementSql.contains(
                        "json_quote(ifnull(descendant.ancestors, ''))"));
                assertTrue(statementSql.contains(
                        "aggregate_sort.ancestor_id = cast(candidate.id as char)"));
                assertFalse(statementSql.contains(
                        "cast(ancestor_token.ancestor_id as unsigned)"));
                assertFalse(statementSql.contains(
                        "ancestor_token.ancestor_id = cast(matched_candidate.id as char)"));
            }
        }
    }

    @Test
    void financialAggregateScansEachTransactionTableOnce() {
        String sql = sql("selectMemberListFinancialAggregates",
                Map.of("userIds", List.of(11L, 12L)));

        assertEquals(1, occurrences(sql, "from order_topup topup"));
        assertEquals(1, occurrences(sql, "from order_withdrawal withdrawal"));
        assertTrue(sql.contains("topup.status = '0'"));
        assertTrue(sql.contains("withdrawal.status = '0'"));
        assertTrue(sql.contains("withdrawal.status = '1'"));
        assertTrue(sql.contains("withdrawal.application_time >= curdate()"));
        assertTrue(sql.contains("group by financial.user_id"));
    }

    @Test
    void subordinateAggregateParsesAncestorsOnceAndKeepsDirectCountIndexed() {
        String sql = sql("selectMemberListSubordinateAggregates",
                Map.of("userIds", List.of(11L, 12L)));

        assertTrue(sql.contains("direct_user.parent_id in ( ? , ? )"));
        assertTrue(sql.contains("inner join json_table("));
        assertTrue(sql.contains("json_quote(ifnull(descendant.ancestors, ''))"));
        assertTrue(sql.contains("count(distinct descendant.id) as all_sub_count"));
        assertFalse(sql.contains("find_in_set"));
        assertEquals(1, occurrences(sql, "from order_member_user descendant"));
    }

    @Test
    void detailLookupStillUsesTheExistingFullProjection() {
        String sql = sql("selectOrderMemberUserById", 11L);

        assertTrue(sql.contains("from order_withdrawal pending_withdrawal"));
        assertTrue(sql.contains("find_in_set(u.id, a.ancestors)"));
        assertTrue(sql.contains("as total_recharge"));
    }

    @Test
    void repeatableIndexMigrationRequiresVisibleExactDefinitions() throws IOException {
        String resource = "db/add_member_list_performance_indexes.sql";
        String migration;
        try (InputStream inputStream = OrderMemberUserMapperSqlTest.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(inputStream);
            migration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT);
        }

        assertEquals(3, occurrences(migration, "and definition.is_visible = 'yes'"));
        assertTrue(migration.contains("min(is_visible) as is_visible"));
        assertTrue(migration.contains("index_type"));
        assertTrue(migration.contains("index_columns"));
    }

    private static String sql(String statementId, Object parameter) {
        BoundSql boundSql = configuration
                .getMappedStatement("com.brushing.member.mapper.OrderMemberUserMapper." + statementId)
                .getBoundSql(parameter);
        return normalize(boundSql.getSql());
    }

    private static OrderMemberUser aggregateSortFilter(String sortKey, String direction) {
        OrderMemberUser filter = new OrderMemberUser();
        filter.getParams().put("memberListSortKey", sortKey);
        filter.getParams().put("memberListSortDirection", direction);
        return filter;
    }

    private static Executor capturingExecutor(AtomicReference<String> countStatementId,
                                              AtomicReference<String> countSql,
                                              AtomicReference<String> pageSql,
                                              List<?> countResult) {
        return (Executor) Proxy.newProxyInstance(
                Executor.class.getClassLoader(),
                new Class<?>[]{Executor.class},
                (proxy, method, args) -> {
                    if ("createCacheKey".equals(method.getName())) {
                        return new CacheKey();
                    }
                    if ("query".equals(method.getName())) {
                        MappedStatement executed = (MappedStatement) args[0];
                        BoundSql executedBoundSql = args.length == 6
                                ? (BoundSql) args[5]
                                : executed.getBoundSql(args[1]);
                        if (executed.getId().endsWith("_COUNT")) {
                            countStatementId.set(executed.getId());
                            countSql.set(normalize(executedBoundSql.getSql()));
                            return countResult;
                        }
                        pageSql.set(normalize(executedBoundSql.getSql()));
                        return Collections.emptyList();
                    }
                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }
                    return primitiveDefault(method.getReturnType());
                });
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private static String outerWhere(String sql) {
        int whereIndex = sql.lastIndexOf(" where ");
        assertTrue(whereIndex >= 0, sql);
        return sql.substring(whereIndex + 1);
    }

    private static int occurrences(String value, String fragment) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(fragment, fromIndex)) >= 0) {
            count++;
            fromIndex += fragment.length();
        }
        return count;
    }
}
