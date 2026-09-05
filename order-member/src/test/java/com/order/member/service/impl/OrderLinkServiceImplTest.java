package com.order.member.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.order.member.domain.OrderLink;
import com.order.member.domain.dto.GoodsDetails;
import com.order.member.mapper.OrderLinkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLinkServiceImplTest
{
    @Mock
    private OrderLinkMapper orderLinkMapper;

    @InjectMocks
    private OrderLinkServiceImpl orderLinkService;

    @Test
    void insertDefaultsMissingStatusToPendingForEveryDetail()
    {
        OrderLink orderLink = new OrderLink();
        orderLink.setUserId(7L);
        orderLink.setDetails(List.of(detail(101L), detail(102L)));
        when(orderLinkMapper.selectMaxLinkOrderId()).thenReturn(40L);
        when(orderLinkMapper.insertOrderLink(any(OrderLink.class))).thenReturn(1);

        assertEquals(2, orderLinkService.insertOrderLink(orderLink));

        ArgumentCaptor<OrderLink> captor = ArgumentCaptor.forClass(OrderLink.class);
        verify(orderLinkMapper, times(2)).insertOrderLink(captor.capture());
        assertEquals("1", orderLink.getStatus());
        for (OrderLink inserted : captor.getAllValues()) {
            assertEquals("1", inserted.getStatus());
            assertEquals(41L, inserted.getLinkOrderId());
        }
    }

    @Test
    void insertDefaultsBlankStatusToPendingForSingleRecord()
    {
        OrderLink orderLink = new OrderLink();
        orderLink.setStatus("  ");
        when(orderLinkMapper.selectMaxLinkOrderId()).thenReturn(null);
        when(orderLinkMapper.insertOrderLink(orderLink)).thenReturn(1);

        assertEquals(1, orderLinkService.insertOrderLink(orderLink));

        ArgumentCaptor<OrderLink> captor = ArgumentCaptor.forClass(OrderLink.class);
        verify(orderLinkMapper).insertOrderLink(captor.capture());
        assertSame(orderLink, captor.getValue());
        assertEquals("1", captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getLinkOrderId());
    }

    private static GoodsDetails detail(Long goodsId)
    {
        GoodsDetails detail = new GoodsDetails();
        detail.setGoodsId(goodsId);
        detail.setPriceType("fixed");
        detail.setPrice(BigDecimal.TEN);
        return detail;
    }
}
