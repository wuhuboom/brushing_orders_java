package com.brushing.set.mapper;

import java.util.List;
import com.brushing.set.domain.OrderSiteConfig;

/**
 * 网站设置Mapper接口
 * 
 * @author brushing
 * @date 2025-07-31
 */
public interface OrderSiteConfigMapper 
{
    /**
     * 查询网站设置
     * 
     * @param id 网站设置主键
     * @return 网站设置
     */
    public OrderSiteConfig selectOrderSiteConfigById(Long id);

    /**
     * 查询网站设置列表
     * 
     * @param orderSiteConfig 网站设置
     * @return 网站设置集合
     */
    public List<OrderSiteConfig> selectOrderSiteConfigList(OrderSiteConfig orderSiteConfig);

    /**
     * 新增网站设置
     * 
     * @param orderSiteConfig 网站设置
     * @return 结果
     */
    public int insertOrderSiteConfig(OrderSiteConfig orderSiteConfig);

    /**
     * 修改网站设置
     * 
     * @param orderSiteConfig 网站设置
     * @return 结果
     */
    public int updateOrderSiteConfig(OrderSiteConfig orderSiteConfig);

    /**
     * 删除网站设置
     * 
     * @param id 网站设置主键
     * @return 结果
     */
    public int deleteOrderSiteConfigById(Long id);

    /**
     * 批量删除网站设置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderSiteConfigByIds(Long[] ids);
}
