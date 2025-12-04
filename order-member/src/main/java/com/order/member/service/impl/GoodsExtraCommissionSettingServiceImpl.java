package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsExtraCommissionSettingMapper;
import com.order.member.domain.GoodsExtraCommissionSetting;
import com.order.member.service.IGoodsExtraCommissionSettingService;

/**
 * 额外佣金设置Service业务层处理
 * 
 * @author order
 * @date 2025-11-08
 */
@Service
public class GoodsExtraCommissionSettingServiceImpl implements IGoodsExtraCommissionSettingService 
{
    @Autowired
    private GoodsExtraCommissionSettingMapper goodsExtraCommissionSettingMapper;

    /**
     * 查询额外佣金设置
     * 
     * @param id 额外佣金设置主键
     * @return 额外佣金设置
     */
    @Override
    public GoodsExtraCommissionSetting selectGoodsExtraCommissionSettingById(Long id)
    {
        return goodsExtraCommissionSettingMapper.selectGoodsExtraCommissionSettingById(id);
    }

    /**
     * 查询额外佣金设置列表
     * 
     * @param goodsExtraCommissionSetting 额外佣金设置
     * @return 额外佣金设置
     */
    @Override
    public List<GoodsExtraCommissionSetting> selectGoodsExtraCommissionSettingList(GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        return goodsExtraCommissionSettingMapper.selectGoodsExtraCommissionSettingList(goodsExtraCommissionSetting);
    }

    /**
     * 新增额外佣金设置
     * 
     * @param goodsExtraCommissionSetting 额外佣金设置
     * @return 结果
     */
    @Override
    public int insertGoodsExtraCommissionSetting(GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        goodsExtraCommissionSetting.setCreateTime(DateUtils.getNowDate());
        return goodsExtraCommissionSettingMapper.insertGoodsExtraCommissionSetting(goodsExtraCommissionSetting);
    }

    /**
     * 修改额外佣金设置
     * 
     * @param goodsExtraCommissionSetting 额外佣金设置
     * @return 结果
     */
    @Override
    public int updateGoodsExtraCommissionSetting(GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        return goodsExtraCommissionSettingMapper.updateGoodsExtraCommissionSetting(goodsExtraCommissionSetting);
    }

    /**
     * 批量删除额外佣金设置
     * 
     * @param ids 需要删除的额外佣金设置主键
     * @return 结果
     */
    @Override
    public int deleteGoodsExtraCommissionSettingByIds(Long[] ids)
    {
        return goodsExtraCommissionSettingMapper.deleteGoodsExtraCommissionSettingByIds(ids);
    }

    /**
     * 删除额外佣金设置信息
     * 
     * @param id 额外佣金设置主键
     * @return 结果
     */
    @Override
    public int deleteGoodsExtraCommissionSettingById(Long id)
    {
        return goodsExtraCommissionSettingMapper.deleteGoodsExtraCommissionSettingById(id);
    }
}
