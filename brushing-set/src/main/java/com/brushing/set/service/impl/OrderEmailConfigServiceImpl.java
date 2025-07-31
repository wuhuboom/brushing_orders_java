package com.brushing.set.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.set.mapper.OrderEmailConfigMapper;
import com.brushing.set.domain.OrderEmailConfig;
import com.brushing.set.service.IOrderEmailConfigService;

/**
 * 系统邮箱配置Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-31
 */
@Service
public class OrderEmailConfigServiceImpl implements IOrderEmailConfigService 
{
    @Autowired
    private OrderEmailConfigMapper orderEmailConfigMapper;

    /**
     * 查询系统邮箱配置
     * 
     * @param id 系统邮箱配置主键
     * @return 系统邮箱配置
     */
    @Override
    public OrderEmailConfig selectOrderEmailConfigById(Long id)
    {
        return orderEmailConfigMapper.selectOrderEmailConfigById(id);
    }

    /**
     * 查询系统邮箱配置列表
     * 
     * @param orderEmailConfig 系统邮箱配置
     * @return 系统邮箱配置
     */
    @Override
    public List<OrderEmailConfig> selectOrderEmailConfigList(OrderEmailConfig orderEmailConfig)
    {
        return orderEmailConfigMapper.selectOrderEmailConfigList(orderEmailConfig);
    }

    /**
     * 新增系统邮箱配置
     * 
     * @param orderEmailConfig 系统邮箱配置
     * @return 结果
     */
    @Override
    public int insertOrderEmailConfig(OrderEmailConfig orderEmailConfig)
    {
        orderEmailConfig.setCreateTime(DateUtils.getNowDate());
        return orderEmailConfigMapper.insertOrderEmailConfig(orderEmailConfig);
    }

    /**
     * 修改系统邮箱配置
     * 
     * @param orderEmailConfig 系统邮箱配置
     * @return 结果
     */
    @Override
    public int updateOrderEmailConfig(OrderEmailConfig orderEmailConfig)
    {
        orderEmailConfig.setUpdateTime(DateUtils.getNowDate());
        return orderEmailConfigMapper.updateOrderEmailConfig(orderEmailConfig);
    }

    /**
     * 批量删除系统邮箱配置
     * 
     * @param ids 需要删除的系统邮箱配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderEmailConfigByIds(Long[] ids)
    {
        return orderEmailConfigMapper.deleteOrderEmailConfigByIds(ids);
    }

    /**
     * 删除系统邮箱配置信息
     * 
     * @param id 系统邮箱配置主键
     * @return 结果
     */
    @Override
    public int deleteOrderEmailConfigById(Long id)
    {
        return orderEmailConfigMapper.deleteOrderEmailConfigById(id);
    }
}
