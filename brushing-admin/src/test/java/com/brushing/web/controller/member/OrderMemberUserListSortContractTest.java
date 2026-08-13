package com.brushing.web.controller.member;

import com.brushing.common.core.page.PageDomain;
import com.brushing.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMemberUserListSortContractTest {

    @Test
    void noSortKeepsTheExistingCreateTimeDescendingDefault() {
        OrderMemberUserController.MemberListSort sort = resolve(null, null);

        assertTrue(sort.isDefault());
        assertFalse(sort.aggregate());
        assertNull(sort.sqlOrderBy());
    }

    @Test
    void basicSortUsesOnlyAWhitelistedSqlExpressionAndStableTieBreaker() {
        OrderMemberUserController.MemberListSort sort = resolve("parentUsername", "descending");

        assertEquals("parent_username", sort.key());
        assertEquals("desc", sort.direction());
        assertEquals("parent_user.username desc, u.id desc", sort.sqlOrderBy());
        assertFalse(sort.aggregate());

        OrderMemberUserController.MemberListSort levelTime = resolve("level_time", "ASC");
        assertEquals("l.create_time asc, u.id desc", levelTime.sqlOrderBy());
    }

    @Test
    void everyLegacyAggregateAliasRoutesToGlobalAggregateSorting() {
        for (String field : new String[]{
                "totalRecharge", "totalWithdraw", "diffAmount",
                "withdrawFrozenAmount", "todayWithdrawCount",
                "directSubCount", "allSubCount"}) {
            OrderMemberUserController.MemberListSort sort = resolve(field, "asc");
            assertTrue(sort.aggregate(), field);
            assertNull(sort.sqlOrderBy(), field);
        }
    }

    @Test
    void unknownOrComposableRequestValuesAreRejectedBeforePageHelper() {
        for (String field : new String[]{
                "notAColumn", "u.id", "id,username", "id desc", "1"}) {
            assertThrows(ServiceException.class, () -> resolve(field, "asc"), field);
        }
        for (String direction : new String[]{
                "desc,id asc", "desc nulls first", "sideways"}) {
            assertThrows(ServiceException.class, () -> resolve("id", direction), direction);
        }
    }

    @Test
    void publicComputedAliasesRemainWhitelisted() {
        assertEquals("total_balance asc, u.id desc",
                resolve("totalBalance", "ascending").sqlOrderBy());
        assertEquals("recharge_needed_for_next_level desc, u.id desc",
                resolve("rechargeNeededForNextLevel", "desc").sqlOrderBy());

        OrderMemberUserController.MemberListSort maxLevel =
                resolve("maxLevelPrice", "descending");
        assertTrue(maxLevel.fixedExpression());
        assertFalse(maxLevel.aggregate());
        assertTrue(maxLevel.sqlOrderBy().contains(
                "SELECT MAX(member_sort_level.price)"));
        assertTrue(maxLevel.sqlOrderBy().endsWith("desc, u.id desc"));

        OrderMemberUserController.MemberListSort nextLevel =
                resolve("nextLevelPrice", "asc");
        assertTrue(nextLevel.fixedExpression());
        assertFalse(nextLevel.aggregate());
        assertTrue(nextLevel.sqlOrderBy().contains(
                "SELECT MIN(member_sort_next_level.price)"));
        assertTrue(nextLevel.sqlOrderBy().contains(
                "SELECT MAX(member_sort_current_level.price)"));
        assertTrue(nextLevel.sqlOrderBy().endsWith("asc, u.id desc"));
    }

    private OrderMemberUserController.MemberListSort resolve(String column, String direction) {
        PageDomain request = new PageDomain();
        request.setOrderByColumn(column);
        request.setIsAsc(direction);
        return OrderMemberUserController.resolveMemberListSort(request);
    }
}
