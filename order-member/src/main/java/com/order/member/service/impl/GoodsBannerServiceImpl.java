package com.order.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsBannerMapper;
import com.order.member.domain.GoodsBanner;
import com.order.member.service.IGoodsBannerService;

/**
 * 横幅：用于存储横幅广告相关信息Service业务层处理
 * 
 * @author order
 * @date 2025-11-11
 */
@Service
public class GoodsBannerServiceImpl implements IGoodsBannerService 
{
    @Autowired
    private GoodsBannerMapper goodsBannerMapper;

    /**
     * 查询横幅：用于存储横幅广告相关信息
     * 
     * @param id 横幅：用于存储横幅广告相关信息主键
     * @return 横幅：用于存储横幅广告相关信息
     */
    @Override
    public GoodsBanner selectGoodsBannerById(Long id)
    {
        return goodsBannerMapper.selectGoodsBannerById(id);
    }

    /**
     * 查询横幅：用于存储横幅广告相关信息列表
     * 
     * @param goodsBanner 横幅：用于存储横幅广告相关信息
     * @return 横幅：用于存储横幅广告相关信息
     */
    @Override
    public List<GoodsBanner> selectGoodsBannerList(GoodsBanner goodsBanner)
    {
        return goodsBannerMapper.selectGoodsBannerList(goodsBanner);
    }

    /**
     * 新增横幅：用于存储横幅广告相关信息
     * 
     * @param goodsBanner 横幅：用于存储横幅广告相关信息
     * @return 结果
     */
    @Override
    public int insertGoodsBanner(GoodsBanner goodsBanner)
    {
        return goodsBannerMapper.insertGoodsBanner(goodsBanner);
    }

    /**
     * 修改横幅：用于存储横幅广告相关信息
     * 
     * @param goodsBanner 横幅：用于存储横幅广告相关信息
     * @return 结果
     */
    @Override
    public int updateGoodsBanner(GoodsBanner goodsBanner)
    {
        return goodsBannerMapper.updateGoodsBanner(goodsBanner);
    }

    /**
     * 批量删除横幅：用于存储横幅广告相关信息
     * 
     * @param ids 需要删除的横幅：用于存储横幅广告相关信息主键
     * @return 结果
     */
    @Override
    public int deleteGoodsBannerByIds(Long[] ids)
    {
        return goodsBannerMapper.deleteGoodsBannerByIds(ids);
    }

    /**
     * 删除横幅：用于存储横幅广告相关信息信息
     * 
     * @param id 横幅：用于存储横幅广告相关信息主键
     * @return 结果
     */
    @Override
    public int deleteGoodsBannerById(Long id)
    {
        return goodsBannerMapper.deleteGoodsBannerById(id);
    }
}
