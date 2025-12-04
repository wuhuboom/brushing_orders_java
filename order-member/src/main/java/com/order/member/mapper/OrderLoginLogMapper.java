package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderLoginLog;

/**
 * 登录日志Mapper接口
 * 
 * @author order
 * @date 2025-11-10
 */
public interface OrderLoginLogMapper 
{
    /**
     * 查询登录日志
     * 
     * @param id 登录日志主键
     * @return 登录日志
     */
    public OrderLoginLog selectOrderLoginLogById(Long id);

    /**
     * 查询登录日志列表
     * 
     * @param orderLoginLog 登录日志
     * @return 登录日志集合
     */
    public List<OrderLoginLog> selectOrderLoginLogList(OrderLoginLog orderLoginLog);

    /**
     * 新增登录日志
     * 
     * @param orderLoginLog 登录日志
     * @return 结果
     */
    public int insertOrderLoginLog(OrderLoginLog orderLoginLog);

    /**
     * 修改登录日志
     * 
     * @param orderLoginLog 登录日志
     * @return 结果
     */
    public int updateOrderLoginLog(OrderLoginLog orderLoginLog);

    /**
     * 删除登录日志
     * 
     * @param id 登录日志主键
     * @return 结果
     */
    public int deleteOrderLoginLogById(Long id);

    /**
     * 批量删除登录日志
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderLoginLogByIds(Long[] ids);
}
