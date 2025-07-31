package com.brushing.set.service;

import java.util.List;
import com.brushing.set.domain.OrderBanner;

/**
 * 轮播图管理Service接口
 * 
 * @author brushing
 * @date 2025-07-31
 */
public interface IOrderBannerService 
{
    /**
     * 查询轮播图管理
     * 
     * @param id 轮播图管理主键
     * @return 轮播图管理
     */
    public OrderBanner selectOrderBannerById(Long id);

    /**
     * 查询轮播图管理列表
     * 
     * @param orderBanner 轮播图管理
     * @return 轮播图管理集合
     */
    public List<OrderBanner> selectOrderBannerList(OrderBanner orderBanner);

    /**
     * 新增轮播图管理
     * 
     * @param orderBanner 轮播图管理
     * @return 结果
     */
    public int insertOrderBanner(OrderBanner orderBanner);

    /**
     * 修改轮播图管理
     * 
     * @param orderBanner 轮播图管理
     * @return 结果
     */
    public int updateOrderBanner(OrderBanner orderBanner);

    /**
     * 批量删除轮播图管理
     * 
     * @param ids 需要删除的轮播图管理主键集合
     * @return 结果
     */
    public int deleteOrderBannerByIds(Long[] ids);

    /**
     * 删除轮播图管理信息
     * 
     * @param id 轮播图管理主键
     * @return 结果
     */
    public int deleteOrderBannerById(Long id);
}
