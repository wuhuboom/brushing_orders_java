package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.brushing.member.mapper.OrderUserLoginLogMapper;
import com.brushing.member.domain.OrderUserLoginLog;
import com.brushing.member.service.IOrderUserLoginLogService;

/**
 * 会员登录日志Service业务层处理
 *
 * @author brushing
 * @date 2026-01-11
 */
@Service
public class OrderUserLoginLogServiceImpl implements IOrderUserLoginLogService
{
    @Autowired
    private OrderUserLoginLogMapper orderUserLoginLogMapper;

    /**
     * 查询会员登录日志
     *
     * @param id 会员登录日志主键
     * @return 会员登录日志
     */
    @Override
    public OrderUserLoginLog selectOrderUserLoginLogById(Long id)
    {
        return orderUserLoginLogMapper.selectOrderUserLoginLogById(id);
    }

    /**
     * 查询会员登录日志列表
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 会员登录日志
     */
    @Override
    public List<OrderUserLoginLog> selectOrderUserLoginLogList(OrderUserLoginLog orderUserLoginLog)
    {
        return orderUserLoginLogMapper.selectOrderUserLoginLogList(orderUserLoginLog);
    }

    /**
     * 新增会员登录日志
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 结果
     */
    @Override
    public int insertOrderUserLoginLog(OrderUserLoginLog orderUserLoginLog)
    {
        orderUserLoginLog.setCreateTime(DateUtils.getNowDate());
        return orderUserLoginLogMapper.insertOrderUserLoginLog(orderUserLoginLog);
    }

    /**
     * 新增会员登录日志（独立事务，确保写库不被外部事务回滚）
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 结果
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int insertOrderUserLoginLogNewTx(OrderUserLoginLog orderUserLoginLog) {
        orderUserLoginLog.setCreateTime(DateUtils.getNowDate());
        return orderUserLoginLogMapper.insertOrderUserLoginLog(orderUserLoginLog);
    }

    /**
     * 修改会员登录日志
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 结果
     */
    @Override
    public int updateOrderUserLoginLog(OrderUserLoginLog orderUserLoginLog)
    {
        return orderUserLoginLogMapper.updateOrderUserLoginLog(orderUserLoginLog);
    }

    /**
     * 批量删除会员登录日志
     *
     * @param ids 需要删除的会员登录日志主键
     * @return 结果
     */
    @Override
    public int deleteOrderUserLoginLogByIds(Long[] ids)
    {
        return orderUserLoginLogMapper.deleteOrderUserLoginLogByIds(ids);
    }

    /**
     * 删除会员登录日志信息
     *
     * @param id 会员登录日志主键
     * @return 结果
     */
    @Override
    public int deleteOrderUserLoginLogById(Long id)
    {
        return orderUserLoginLogMapper.deleteOrderUserLoginLogById(id);
    }
}
