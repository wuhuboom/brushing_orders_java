package com.order.member.service.impl;

import com.order.member.domain.OrderUser;
import com.order.member.mapper.OrderUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUserKeywordSearchServiceTest
{
    @Mock
    private OrderUserMapper orderUserMapper;

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
}
