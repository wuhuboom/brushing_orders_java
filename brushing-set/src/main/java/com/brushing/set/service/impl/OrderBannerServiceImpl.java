package com.brushing.set.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.set.mapper.OrderBannerMapper;
import com.brushing.set.domain.OrderBanner;
import com.brushing.set.service.IOrderBannerService;

/**
 * 轮播图管理Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-31
 */
@Service
public class OrderBannerServiceImpl implements IOrderBannerService 
{
    @Autowired
    private OrderBannerMapper orderBannerMapper;

    /**
     * 查询轮播图管理
     * 
     * @param id 轮播图管理主键
     * @return 轮播图管理
     */
    @Override
    public OrderBanner selectOrderBannerById(Long id)
    {
        return orderBannerMapper.selectOrderBannerById(id);
    }

    /**
     * 查询轮播图管理列表
     * 
     * @param orderBanner 轮播图管理
     * @return 轮播图管理
     */
    @Override
    public List<OrderBanner> selectOrderBannerList(OrderBanner orderBanner)
    {
        return orderBannerMapper.selectOrderBannerList(orderBanner);
    }

    /**
     * 新增轮播图管理
     * 
     * @param orderBanner 轮播图管理
     * @return 结果
     */
    @Override
    public int insertOrderBanner(OrderBanner orderBanner)
    {
        orderBanner.setCreateTime(DateUtils.getNowDate());
        return orderBannerMapper.insertOrderBanner(orderBanner);
    }

    /**
     * 修改轮播图管理
     * 
     * @param orderBanner 轮播图管理
     * @return 结果
     */
    @Override
    public int updateOrderBanner(OrderBanner orderBanner)
    {
        orderBanner.setUpdateTime(DateUtils.getNowDate());
        return orderBannerMapper.updateOrderBanner(orderBanner);
    }

    /**
     * 批量删除轮播图管理
     * 
     * @param ids 需要删除的轮播图管理主键
     * @return 结果
     */
    @Override
    public int deleteOrderBannerByIds(Long[] ids)
    {
        return orderBannerMapper.deleteOrderBannerByIds(ids);
    }

    /**
     * 删除轮播图管理信息
     * 
     * @param id 轮播图管理主键
     * @return 结果
     */
    @Override
    public int deleteOrderBannerById(Long id)
    {
        return orderBannerMapper.deleteOrderBannerById(id);
    }
}
