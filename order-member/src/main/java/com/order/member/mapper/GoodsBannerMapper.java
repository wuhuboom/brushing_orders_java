package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.GoodsBanner;

/**
 * 横幅：用于存储横幅广告相关信息Mapper接口
 * 
 * @author order
 * @date 2025-11-11
 */
public interface GoodsBannerMapper 
{
    /**
     * 查询横幅：用于存储横幅广告相关信息
     * 
     * @param id 横幅：用于存储横幅广告相关信息主键
     * @return 横幅：用于存储横幅广告相关信息
     */
    public GoodsBanner selectGoodsBannerById(Long id);

    /**
     * 查询横幅：用于存储横幅广告相关信息列表
     * 
     * @param goodsBanner 横幅：用于存储横幅广告相关信息
     * @return 横幅：用于存储横幅广告相关信息集合
     */
    public List<GoodsBanner> selectGoodsBannerList(GoodsBanner goodsBanner);

    /**
     * 新增横幅：用于存储横幅广告相关信息
     * 
     * @param goodsBanner 横幅：用于存储横幅广告相关信息
     * @return 结果
     */
    public int insertGoodsBanner(GoodsBanner goodsBanner);

    /**
     * 修改横幅：用于存储横幅广告相关信息
     * 
     * @param goodsBanner 横幅：用于存储横幅广告相关信息
     * @return 结果
     */
    public int updateGoodsBanner(GoodsBanner goodsBanner);

    /**
     * 删除横幅：用于存储横幅广告相关信息
     * 
     * @param id 横幅：用于存储横幅广告相关信息主键
     * @return 结果
     */
    public int deleteGoodsBannerById(Long id);

    /**
     * 批量删除横幅：用于存储横幅广告相关信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsBannerByIds(Long[] ids);
}
