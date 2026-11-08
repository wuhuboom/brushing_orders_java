package com.order.member.service.impl;

import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.MemberDailyStatisticsService;
import com.github.pagehelper.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUserKeywordSearchServiceTest
{
    @Mock
    private OrderUserMapper orderUserMapper;

    @Mock
    private MemberDailyStatisticsService dailyStatisticsService;

    @InjectMocks
    private OrderUserServiceImpl orderUserService;

    @Test
    void listTrimsKeywordBeforeItReachesTheMapper()
    {
        OrderUser query = new OrderUser();
        query.setKeyword("  IF2XQ  ");
        when(orderUserMapper.selectOrderUserList(query)).thenReturn(List.of());

        orderUserService.selectOrderUserList(query);

        assertEquals("IF2XQ", query.getKeyword());
        verify(orderUserMapper).selectOrderUserList(query);
    }

    @Test
    void whitespaceOnlyKeywordBecomesAnEmptyUnfilteredKeyword()
    {
        OrderUser query = new OrderUser();
        query.setKeyword("   ");
        when(orderUserMapper.selectOrderUserList(query)).thenReturn(List.of());

        orderUserService.selectOrderUserList(query);

        assertEquals("", query.getKeyword());
        verify(orderUserMapper).selectOrderUserList(query);
    }

    @Test
    void dailyStatisticsAreLoadedAfterPaginationAndKeepItsMetadata()
    {
        OrderUser query = new OrderUser();
        Page<OrderUser> page = new Page<>(2, 10);
        page.setTotal(37);
        page.add(new OrderUser());
        when(orderUserMapper.selectOrderUserList(query)).thenReturn(page);

        assertSame(page, orderUserService.selectOrderUserList(query));
        assertEquals(37, page.getTotal());
        assertEquals(2, page.getPageNum());
        var calls = inOrder(orderUserMapper, dailyStatisticsService);
        calls.verify(orderUserMapper).selectOrderUserList(query);
        calls.verify(dailyStatisticsService).populate(page);
    }

    @Test
    void detailsAndChildListsAlsoPopulateDailyStatistics()
    {
        OrderUser user = new OrderUser();
        List<OrderUser> children = List.of(user);
        when(orderUserMapper.selectOrderUserById(247L)).thenReturn(user);
        when(orderUserMapper.selectOrderUserByName("Katherine1487")).thenReturn(user);
        when(orderUserMapper.selectOrderUserByInviteCode("ABCDE")).thenReturn(user);
        when(orderUserMapper.selectChildrenById(246L, "direct")).thenReturn(children);

        assertSame(user, orderUserService.selectOrderUserById(247L));
        assertSame(user, orderUserService.selectOrderUserByName("Katherine1487"));
        assertSame(user, orderUserService.selectOrderUserByInviteCode("ABCDE"));
        assertSame(children, orderUserService.selectChildrenById(246L, "direct"));

        verify(dailyStatisticsService, org.mockito.Mockito.times(3)).populate(user);
        verify(dailyStatisticsService).populate(children);
    }
}
