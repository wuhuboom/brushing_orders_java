package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.order.member.mapper.OrderSequenceManagerMapper;
import com.order.member.domain.OrderSequenceManager;
import com.order.member.service.IOrderSequenceManagerService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列管理Service业务层处理
 *
 * @author order
 * @date 2025-10-25
 */
@Service
public class OrderSequenceManagerServiceImpl implements IOrderSequenceManagerService
{
    private static final Logger log = LoggerFactory.getLogger(OrderSequenceManagerServiceImpl.class);

    @Autowired
    private OrderSequenceManagerMapper orderSequenceManagerMapper;

    private static final int MAX_RETRY = 5;

    // per-key locks to reduce JVM-local races
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

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
    @Transactional(rollbackFor = Exception.class)
    public String generateCode(String seqType) {
        // 使用当前日期和时间生成前缀
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HHmm");
        String dateStr = dateFormat.format(new java.util.Date());
        String timeStr = timeFormat.format(new java.util.Date());

        // 获取序列号（基于 seqType + dateStr + timeStr，确保每天每小时重置序列）
        Long seq = getAndIncrementWithLock(seqType, dateStr, timeStr);
        if (seq == null) {
            throw new RuntimeException("生成序列号失败");
        }

        // 格式化为指定编码：YYYYMMDDHHMM + 08位序列号
        return String.format("%s%s%08d", dateStr, timeStr, seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getAndIncrementWithLock(String seqType, String dateStr, String timeStr) {
        String key = seqType + "#" + dateStr + "#" + timeStr;
        Object lock = keyLocks.computeIfAbsent(key, k -> new Object());

        synchronized (lock) {
            int attempt = 0;
            while (true) {
                attempt++;
                // 1. 尝试在事务内 select ... for update
                OrderSequenceManager current = orderSequenceManagerMapper.getCurrentSeqForUpdate(seqType, dateStr, timeStr);
                if (current != null) {
                    long newSeq = (current.getCurrentSeq() == null ? 0L : current.getCurrentSeq()) + 1L;
                    current.setCurrentSeq(newSeq);
                    current.setUpdateTime(DateUtils.getNowDate());
                    int updated = orderSequenceManagerMapper.updateCurrentSeqById(current);
                    if (updated > 0) {
                        return newSeq;
                    } else {
                        // version or id mismatch unlikely here, retry
                        log.warn("updateCurrentSeqById returned 0, retrying, key={}, attempt={}", key, attempt);
                        if (attempt >= MAX_RETRY) throw new RuntimeException("序列生成失败，已重试 " + MAX_RETRY + " 次");
                        continue;
                    }
                }

                // 2. current == null -> try insert new row with current_seq=1
                try {
                    OrderSequenceManager newEntity = new OrderSequenceManager();
                    newEntity.setSeqType(seqType);
                    newEntity.setSeqDate(dateStr);
                    newEntity.setSeqTime(timeStr);
                    newEntity.setCurrentSeq(1L);
                    newEntity.setVersion(0L);
                    newEntity.setCreateTime(DateUtils.getNowDate());
                    newEntity.setUpdateTime(DateUtils.getNowDate());
                    orderSequenceManagerMapper.insertOrderSequenceManager(newEntity);
                    return 1L;
                } catch (DuplicateKeyException dk) {
                    // 并发插入导致唯一索引冲突，回到循环读取并更新（重试）
                    log.warn("DuplicateKey on insert for key={}, attempt={}, will retry", key, attempt);
                    if (attempt >= MAX_RETRY) throw new RuntimeException("序列生成失败，已重试 " + MAX_RETRY + " 次");
                    continue;
                }
            }
        }
    }
}