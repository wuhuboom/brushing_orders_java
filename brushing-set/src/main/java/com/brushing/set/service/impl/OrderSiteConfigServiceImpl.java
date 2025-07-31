package com.brushing.set.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.set.mapper.OrderSiteConfigMapper;
import com.brushing.set.domain.OrderSiteConfig;
import com.brushing.set.service.IOrderSiteConfigService;

/**
 * 网站设置Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-31
 */
@Service
public class OrderSiteConfigServiceImpl implements IOrderSiteConfigService 
{
    @Autowired
    private OrderSiteConfigMapper orderSiteConfigMapper;

    /**
     * 查询网站设置
     * 
     * @param id 网站设置主键
     * @return 网站设置
     */
    @Override
    public OrderSiteConfig selectOrderSiteConfigById(Long id)
    {
        return orderSiteConfigMapper.selectOrderSiteConfigById(id);
    }

    /**
     * 查询网站设置列表
     * 
     * @param orderSiteConfig 网站设置
     * @return 网站设置
     */
    @Override
    public List<OrderSiteConfig> selectOrderSiteConfigList(OrderSiteConfig orderSiteConfig)
    {
        return orderSiteConfigMapper.selectOrderSiteConfigList(orderSiteConfig);
    }

    /**
     * 新增网站设置
     * 
     * @param orderSiteConfig 网站设置
     * @return 结果
     */
    @Override
    public int insertOrderSiteConfig(OrderSiteConfig orderSiteConfig)
    {
        orderSiteConfig.setCreateTime(DateUtils.getNowDate());
        return orderSiteConfigMapper.insertOrderSiteConfig(orderSiteConfig);
    }

    /**
     * 修改网站设置
     * 
     * @param orderSiteConfig 网站设置
     * @return 结果
     */
    @Override
    public int updateOrderSiteConfig(OrderSiteConfig orderSiteConfig)
    {
        orderSiteConfig.setUpdateTime(DateUtils.getNowDate());
        return orderSiteConfigMapper.updateOrderSiteConfig(orderSiteConfig);
    }

    /**
     * 批量删除网站设置
     * 
     * @param ids 需要删除的网站设置主键
     * @return 结果
     */
    @Override
    public int deleteOrderSiteConfigByIds(Long[] ids)
    {
        return orderSiteConfigMapper.deleteOrderSiteConfigByIds(ids);
    }

    /**
     * 删除网站设置信息
     * 
     * @param id 网站设置主键
     * @return 结果
     */
    @Override
    public int deleteOrderSiteConfigById(Long id)
    {
        return orderSiteConfigMapper.deleteOrderSiteConfigById(id);
    }
}
