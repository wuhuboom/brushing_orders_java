package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderLoginLogMapper;
import com.order.member.domain.OrderLoginLog;
import com.order.member.service.IOrderLoginLogService;

/**
 * 登录日志Service业务层处理
 * 
 * @author order
 * @date 2025-11-10
 */
@Service
public class OrderLoginLogServiceImpl implements IOrderLoginLogService 
{
    @Autowired
    private OrderLoginLogMapper orderLoginLogMapper;

    /**
     * 查询登录日志
     * 
     * @param id 登录日志主键
     * @return 登录日志
     */
    @Override
    public OrderLoginLog selectOrderLoginLogById(Long id)
    {
        return orderLoginLogMapper.selectOrderLoginLogById(id);
    }

    /**
     * 查询登录日志列表
     * 
     * @param orderLoginLog 登录日志
     * @return 登录日志
     */
    @Override
    public List<OrderLoginLog> selectOrderLoginLogList(OrderLoginLog orderLoginLog)
    {
        return orderLoginLogMapper.selectOrderLoginLogList(orderLoginLog);
    }

    @Override
    public int insertLoginAttempt(Long userId,
                                  String ip,
                                  String address,
                                  String success,
                                  String requestHeaders) {
        OrderLoginLog orderLoginLog =new OrderLoginLog();
        orderLoginLog.setCreateTime(DateUtils.getNowDate());
        orderLoginLog.setAddress(address);
        orderLoginLog.setIp(ip);
        orderLoginLog.setUserId(userId);
        orderLoginLog.setSuccess(success);
        orderLoginLog.setRequestHeaders(requestHeaders);

        return  orderLoginLogMapper.insertOrderLoginLog(orderLoginLog);
    }



    /**
     * 修改登录日志
     * 
     * @param orderLoginLog 登录日志
     * @return 结果
     */
    @Override
    public int updateOrderLoginLog(OrderLoginLog orderLoginLog)
    {
        return orderLoginLogMapper.updateOrderLoginLog(orderLoginLog);
    }

    /**
     * 批量删除登录日志
     * 
     * @param ids 需要删除的登录日志主键
     * @return 结果
     */
    @Override
    public int deleteOrderLoginLogByIds(Long[] ids)
    {
        return orderLoginLogMapper.deleteOrderLoginLogByIds(ids);
    }

    /**
     * 删除登录日志信息
     * 
     * @param id 登录日志主键
     * @return 结果
     */
    @Override
    public int deleteOrderLoginLogById(Long id)
    {
        return orderLoginLogMapper.deleteOrderLoginLogById(id);
    }
}
