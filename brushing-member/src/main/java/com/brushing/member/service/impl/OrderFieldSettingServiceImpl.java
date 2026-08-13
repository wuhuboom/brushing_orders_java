package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderFieldSettingMapper;
import com.brushing.member.domain.OrderFieldSetting;
import com.brushing.member.service.IOrderFieldSettingService;
import com.brushing.common.constant.UserConstants;
import com.brushing.common.utils.StringUtils;

/**
 * 字段设置Service业务层处理
 *
 * @author brushing
 * @date 2026-01-26
 */
@Service
public class OrderFieldSettingServiceImpl implements IOrderFieldSettingService
{
    @Autowired
    private OrderFieldSettingMapper orderFieldSettingMapper;

    /**
     * 查询字段设置
     *
     * @param id 字段设置主键
     * @return 字段设置
     */
    @Override
    public OrderFieldSetting selectOrderFieldSettingById(Long id)
    {
        return orderFieldSettingMapper.selectOrderFieldSettingById(id);
    }

    /**
     * 查询字段设置列表
     *
     * @param orderFieldSetting 字段设置
     * @return 字段设置
     */
    @Override
    public List<OrderFieldSetting> selectOrderFieldSettingList(OrderFieldSetting orderFieldSetting)
    {
        return orderFieldSettingMapper.selectOrderFieldSettingList(orderFieldSetting);
    }

    /**
     * 新增字段设置
     *
     * @param orderFieldSetting 字段设置
     * @return 结果
     */
    @Override
    public int insertOrderFieldSetting(OrderFieldSetting orderFieldSetting)
    {
        orderFieldSetting.setCreateTime(DateUtils.getNowDate());
        return orderFieldSettingMapper.insertOrderFieldSetting(orderFieldSetting);
    }

    /**
     * 修改字段设置
     *
     * @param orderFieldSetting 字段设置
     * @return 结果
     */
    @Override
    public int updateOrderFieldSetting(OrderFieldSetting orderFieldSetting)
    {
        return orderFieldSettingMapper.updateOrderFieldSetting(orderFieldSetting);
    }

    /**
     * 批量删除字段设置
     *
     * @param ids 需要删除的字段设置主键
     * @return 结果
     */
    @Override
    public int deleteOrderFieldSettingByIds(Long[] ids)
    {
        return orderFieldSettingMapper.deleteOrderFieldSettingByIds(ids);
    }

    /**
     * 删除字段设置信息
     *
     * @param id 字段设置主键
     * @return 结果
     */
    @Override
    public int deleteOrderFieldSettingById(Long id)
    {
        return orderFieldSettingMapper.deleteOrderFieldSettingById(id);
    }

    /**
     * 校验类型是否唯一
     *
     * @param orderFieldSetting 字段设置
     * @return 结果
     */
    @Override
    public boolean checkTypeUnique(OrderFieldSetting orderFieldSetting)
    {
        Long id = StringUtils.isNull(orderFieldSetting.getId()) ? -1L : orderFieldSetting.getId();
        OrderFieldSetting info = orderFieldSettingMapper.checkTypeUnique(orderFieldSetting.getType());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != id.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public OrderFieldSetting selectOrderFieldSettingByType(String type) {
        return orderFieldSettingMapper.selectOrderFieldSettingByType(type);
    }

}
