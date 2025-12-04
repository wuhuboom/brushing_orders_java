package com.order.member.service;

import java.util.List;
import com.order.member.domain.GoodsExtraCommissionSetting;

/**
 * 额外佣金设置Service接口
 * 
 * @author order
 * @date 2025-11-08
 */
public interface IGoodsExtraCommissionSettingService 
{
    /**
     * 查询额外佣金设置
     * 
     * @param id 额外佣金设置主键
     * @return 额外佣金设置
     */
    public GoodsExtraCommissionSetting selectGoodsExtraCommissionSettingById(Long id);

    /**
     * 查询额外佣金设置列表
     * 
     * @param goodsExtraCommissionSetting 额外佣金设置
     * @return 额外佣金设置集合
     */
    public List<GoodsExtraCommissionSetting> selectGoodsExtraCommissionSettingList(GoodsExtraCommissionSetting goodsExtraCommissionSetting);

    /**
     * 新增额外佣金设置
     * 
     * @param goodsExtraCommissionSetting 额外佣金设置
     * @return 结果
     */
    public int insertGoodsExtraCommissionSetting(GoodsExtraCommissionSetting goodsExtraCommissionSetting);

    /**
     * 修改额外佣金设置
     * 
     * @param goodsExtraCommissionSetting 额外佣金设置
     * @return 结果
     */
    public int updateGoodsExtraCommissionSetting(GoodsExtraCommissionSetting goodsExtraCommissionSetting);

    /**
     * 批量删除额外佣金设置
     * 
     * @param ids 需要删除的额外佣金设置主键集合
     * @return 结果
     */
    public int deleteGoodsExtraCommissionSettingByIds(Long[] ids);

    /**
     * 删除额外佣金设置信息
     * 
     * @param id 额外佣金设置主键
     * @return 结果
     */
    public int deleteGoodsExtraCommissionSettingById(Long id);
}
