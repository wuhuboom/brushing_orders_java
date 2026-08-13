package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderFieldSetting;

/**
 * 字段设置Mapper接口
 *
 * @author brushing
 * @date 2026-01-26
 */
public interface OrderFieldSettingMapper
{
    /**
     * 查询字段设置
     *
     * @param id 字段设置主键
     * @return 字段设置
     */
    public OrderFieldSetting selectOrderFieldSettingById(Long id);


    public OrderFieldSetting selectOrderFieldSettingByType(String type);

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
     * 删除字段设置
     *
     * @param id 字段设置主键
     * @return 结果
     */
    public int deleteOrderFieldSettingById(Long id);

    /**
     * 批量删除字段设置
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderFieldSettingByIds(Long[] ids);

    /**
     * 校验类型是否唯一
     *
     * @param type 类型
     * @return OrderFieldSetting
     */
    public OrderFieldSetting checkTypeUnique(String type);

}
