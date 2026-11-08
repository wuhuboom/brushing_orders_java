package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderSequenceManagerMapper;
import com.order.member.domain.OrderSequenceManager;
import com.order.member.service.IOrderSequenceManagerService;


/**
 * 序列管理Service业务层处理
 *
 * @author order
 * @date 2025-10-25
 */
@Service
public class OrderSequenceManagerServiceImpl implements IOrderSequenceManagerService
{
    @Autowired
    private OrderSequenceManagerMapper orderSequenceManagerMapper;

    @Autowired
    private SequenceAllocator sequenceAllocator;

    @Override
    public OrderSequenceManager selectOrderSequenceManagerById(Long id)
    {
        return orderSequenceManagerMapper.selectOrderSequenceManagerById(id);
    }

    @Override
    public List<OrderSequenceManager> selectOrderSequenceManagerList(OrderSequenceManager orderSequenceManager)
    {
        return orderSequenceManagerMapper.selectOrderSequenceManagerList(orderSequenceManager);
    }

    @Override
    public int insertOrderSequenceManager(OrderSequenceManager orderSequenceManager)
    {
        orderSequenceManager.setCreateTime(DateUtils.getNowDate());
        return orderSequenceManagerMapper.insertOrderSequenceManager(orderSequenceManager);
    }

    @Override
    public int updateOrderSequenceManager(OrderSequenceManager orderSequenceManager)
    {
        orderSequenceManager.setUpdateTime(DateUtils.getNowDate());
        return orderSequenceManagerMapper.incrementWithVersion(orderSequenceManager);
    }

    @Override
    public int deleteOrderSequenceManagerByIds(Long[] ids)
    {
        return orderSequenceManagerMapper.deleteOrderSequenceManagerByIds(ids);
    }

    @Override
    public int deleteOrderSequenceManagerById(Long id)
    {
        return orderSequenceManagerMapper.deleteOrderSequenceManagerById(id);
    }

    @Override
    public String generateCode(String seqType) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String date = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(java.time.format.DateTimeFormatter.ofPattern("HHmm"));
        long sequence = sequenceAllocator.allocate(seqType, date, time);
        return String.format("%s%s%08d", date, time, sequence);
    }

    @Override
    public Long getAndIncrementWithLock(String seqType, String dateStr, String timeStr) {
        return sequenceAllocator.allocate(seqType, dateStr, timeStr);
    }
}
