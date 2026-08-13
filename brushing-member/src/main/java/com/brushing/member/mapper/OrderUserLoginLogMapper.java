package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderUserLoginLog;

/**
 * 会员登录日志Mapper接口
 *
 * @author brushing
 * @date 2026-01-11
 */
public interface OrderUserLoginLogMapper
{
    /**
     * 查询会员登录日志
     *
     * @param id 会员登录日志主键
     * @return 会员登录日志
     */
    public OrderUserLoginLog selectOrderUserLoginLogById(Long id);

    /**
     * 查询会员登录日志列表
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 会员登录日志集合
     */
    public List<OrderUserLoginLog> selectOrderUserLoginLogList(OrderUserLoginLog orderUserLoginLog);

    /**
     * 新增会员登录日志
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 结果
     */
    public int insertOrderUserLoginLog(OrderUserLoginLog orderUserLoginLog);

    /**
     * 修改会员登录日志
     *
     * @param orderUserLoginLog 会员登录日志
     * @return 结果
     */
    public int updateOrderUserLoginLog(OrderUserLoginLog orderUserLoginLog);

    /**
     * 删除会员登录日志
     *
     * @param id 会员登录日志主键
     * @return 结果
     */
    public int deleteOrderUserLoginLogById(Long id);

    /**
     * 批量删除会员登录日志
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderUserLoginLogByIds(Long[] ids);
}
