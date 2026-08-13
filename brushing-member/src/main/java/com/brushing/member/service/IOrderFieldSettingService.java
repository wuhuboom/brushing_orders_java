package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderFieldSetting;

/**
 * 字段设置Service接口
 *
 * @author brushing
 * @date 2026-01-26
 */
public interface IOrderFieldSettingService
{
    /**
     * 查询字段设置
     *
     * @param id 字段设置主键
     * @return 字段设置
     */
    public OrderFieldSetting selectOrderFieldSettingById(Long id);

    /**
     * 查询字段设置列表
     *
     * @param orderFieldSetting 字段设置
     * @return 字段设置集合
     */
    public List<OrderFieldSetting> selectOrderFieldSettingList(OrderFieldSetting orderFieldSetting);

    /**
     * 新增字段设置
     *
     * @param orderFieldSetting 字段设置
     * @return 结果
     */
    public int insertOrderFieldSetting(OrderFieldSetting orderFieldSetting);

    /**
     * 修改字段设置
     *
     * @param orderFieldSetting 字段设置
     * @return 结果
     */
    public int updateOrderFieldSetting(OrderFieldSetting orderFieldSetting);

    /**
     * 批量删除字段设置
     *
     * @param ids 需要删除的字段设置主键集合
     * @return 结果
     */
    public int deleteOrderFieldSettingByIds(Long[] ids);

    /**
     * 删除字段设置信息
     *
     * @param id 字段设置主键
     * @return 结果
     */
    public int deleteOrderFieldSettingById(Long id);

    /**
     * 校验类型是否唯一
     *
     * @param orderFieldSetting 字段设置
     * @return 结果
     */
    public boolean checkTypeUnique(OrderFieldSetting orderFieldSetting);

    public OrderFieldSetting selectOrderFieldSettingByType(String type);

}
