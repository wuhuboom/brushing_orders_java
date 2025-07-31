package com.brushing.set.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.set.mapper.OrderGlobalConfigMapper;
import com.brushing.set.domain.OrderGlobalConfig;
import com.brushing.set.service.IOrderGlobalConfigService;

/**
 * 全局配置（中英文内容）Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-31
 */
@Service
public class OrderGlobalConfigServiceImpl implements IOrderGlobalConfigService 
{
    @Autowired
    private OrderGlobalConfigMapper orderGlobalConfigMapper;

    /**
     * 查询全局配置（中英文内容）
     * 
     * @param id 全局配置（中英文内容）主键
     * @return 全局配置（中英文内容）
     */
    @Override
    public OrderGlobalConfig selectOrderGlobalConfigById(Long id)
    {
        return orderGlobalConfigMapper.selectOrderGlobalConfigById(id);
    }

    /**
     * 查询全局配置（中英文内容）列表
     * 
     * @param orderGlobalConfig 全局配置（中英文内容）
     * @return 全局配置（中英文内容）
     */
    @Override
    public List<OrderGlobalConfig> selectOrderGlobalConfigList(OrderGlobalConfig orderGlobalConfig)
    {
        return orderGlobalConfigMapper.selectOrderGlobalConfigList(orderGlobalConfig);
    }

    /**
     * 新增全局配置（中英文内容）
     * 
     * @param orderGlobalConfig 全局配置（中英文内容）
     * @return 结果
     */
    @Override
    public int insertOrderGlobalConfig(OrderGlobalConfig orderGlobalConfig)
    {
        orderGlobalConfig.setCreateTime(DateUtils.getNowDate());
        return orderGlobalConfigMapper.insertOrderGlobalConfig(orderGlobalConfig);
    }

    /**
     * 修改全局配置（中英文内容）
     * 
     * @param orderGlobalConfig 全局配置（中英文内容）
     * @return 结果
     */
    @Override
    public int updateOrderGlobalConfig(OrderGlobalConfig orderGlobalConfig)
    {
        orderGlobalConfig.setUpdateTime(DateUtils.getNowDate());
        return orderGlobalConfigMapper.updateOrderGlobalConfig(orderGlobalConfig);
    }

    /**
     * 批量删除全局配置（中英文内容）
     * 
     * @param ids 需要删除的全局配置（中英文内容）主键
     * @return 结果
     */
    @Override
    public int deleteOrderGlobalConfigByIds(Long[] ids)
    {
        return orderGlobalConfigMapper.deleteOrderGlobalConfigByIds(ids);
    }

    /**
     * 删除全局配置（中英文内容）信息
     * 
     * @param id 全局配置（中英文内容）主键
     * @return 结果
     */
    @Override
    public int deleteOrderGlobalConfigById(Long id)
    {
        return orderGlobalConfigMapper.deleteOrderGlobalConfigById(id);
    }
}
