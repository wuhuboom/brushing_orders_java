package com.order.member.service.impl;

import java.util.List;

import com.order.member.domain.dto.GoodsDetails;
import com.order.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderLinkMapper;
import com.order.member.domain.OrderLink;
import com.order.member.service.IOrderLinkService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 连单Service业务层处理
 * 
 * @author order
 * @date 2025-10-29
 */
@Service
@Transactional
public class OrderLinkServiceImpl implements IOrderLinkService 
{
    private static final Logger log = LoggerFactory.getLogger(OrderLinkServiceImpl.class);
    private static final String PENDING_STATUS = "1";

    @Autowired
    private OrderLinkMapper orderLinkMapper;

    @Autowired
    private SequenceAllocator sequenceAllocator;

    /**
     * 查询连单
     * 
     * @param id 连单主键
     * @return 连单
     */
    @Override
    public OrderLink selectOrderLinkById(Long id)
    {
        return orderLinkMapper.selectOrderLinkById(id);
    }

    /**
     * 查询连单列表
     * 
     * @param orderLink 连单
     * @return 连单
     */
    @Override
    public List<OrderLink> selectOrderLinkList(OrderLink orderLink)
    {
        return orderLinkMapper.selectOrderLinkList(orderLink);
    }

    /**
     * 新增连单
     * 
     * @param orderLink 连单
     * @return 结果
     */
    @Override
    public int insertOrderLink(OrderLink orderLink)
    {
        // defensive checks
        if (orderLink == null) return 0;
        if (orderLink.getStatus() == null || orderLink.getStatus().isBlank()) {
            orderLink.setStatus(PENDING_STATUS);
        }

        orderLink.setCreateTime(DateUtils.getNowDate());
        orderLink.setUpdateTime(orderLink.getCreateTime());
        List<GoodsDetails> details = orderLink.getDetails();

        // A separately committed database allocation works across JVMs and rollbacks.
        Long batchLinkOrderId = sequenceAllocator.allocateLinkBatch();
        log.debug("Generated batchLinkOrderId={}", batchLinkOrderId);

        int inserted = 0;
        if (details != null && !details.isEmpty()) {
            for (GoodsDetails detail : details) {
                OrderLink link = new OrderLink();
                link.setLinkOrderId(batchLinkOrderId);
                link.setUserId(orderLink.getUserId());
                link.setProductId(detail.getGoodsId());
                link.setOrderCount(orderLink.getOrderCount());
                link.setCommissionMultiple(orderLink.getCommissionMultiple());
                link.setPriceType(detail.getPriceType());
                link.setPrice(detail.getPrice());
                link.setStatus(orderLink.getStatus());
                link.setCreateBy(orderLink.getCreateBy());
                link.setCreateTime(orderLink.getCreateTime());
                link.setUpdateBy(orderLink.getUpdateBy());
                link.setUpdateTime(orderLink.getUpdateTime());
                inserted += orderLinkMapper.insertOrderLink(link);
            }
            // don't insert the parent container object again to avoid duplicate entries
        } else {
            // no details, insert the provided orderLink as a single record with generated batch id
            orderLink.setLinkOrderId(batchLinkOrderId);
            inserted = orderLinkMapper.insertOrderLink(orderLink);
        }

        return inserted;
    }

    /**
     * 修改连单
     * 
     * @param orderLink 连单
     * @return 结果
     */
    @Override
    public int updateOrderLink(OrderLink orderLink)
    {
        orderLink.setUpdateTime(DateUtils.getNowDate());
        return orderLinkMapper.updateOrderLink(orderLink);
    }

    /**
     * 批量删除连单
     * 
     * @param ids 需要删除的连单主键
     * @return 结果
     */
    @Override
    public int deleteOrderLinkByIds(Long[] ids)
    {
        return orderLinkMapper.deleteOrderLinkByIds(ids);
    }

    /**
     * 删除连单信息
     * 
     * @param id 连单主键
     * @return 结果
     */
    @Override
    public int deleteOrderLinkById(Long id)
    {
        return orderLinkMapper.deleteOrderLinkById(id);
    }

    @Override
    public List<OrderLink> selectOrderLinkByUserId(Long userId, Long orderCount) {
        return orderLinkMapper.selectOrderLinkByUserId(userId, orderCount);
    }
}
