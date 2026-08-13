package com.order.member.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.order.common.exception.ServiceException;
import com.order.common.utils.DateUtils;
import com.order.member.domain.GoodsExtraCommissionSetting;
import com.order.member.mapper.GoodsExtraCommissionSettingMapper;
import com.order.member.service.IGoodsExtraCommissionSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 额外佣金设置Service业务层处理
 * 
 * @author order
 * @date 2025-11-08
 */
@Service
public class GoodsExtraCommissionSettingServiceImpl implements IGoodsExtraCommissionSettingService 
{
    private final GoodsExtraCommissionSettingMapper goodsExtraCommissionSettingMapper;

    public GoodsExtraCommissionSettingServiceImpl(
            GoodsExtraCommissionSettingMapper goodsExtraCommissionSettingMapper) {
        this.goodsExtraCommissionSettingMapper = goodsExtraCommissionSettingMapper;
    }

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
        if (goodsExtraCommissionSetting == null) {
            throw new ServiceException("额外佣金配置不能为空");
        }
        goodsExtraCommissionSetting.setIsLocked("1");
        goodsExtraCommissionSetting.setStatus("1");
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
    @Transactional(rollbackFor = Exception.class)
    public int updateGoodsExtraCommissionSetting(GoodsExtraCommissionSetting goodsExtraCommissionSetting)
    {
        if (goodsExtraCommissionSetting == null || goodsExtraCommissionSetting.getId() == null) {
            throw new ServiceException("额外佣金配置ID不能为空");
        }
        requireEditable(goodsExtraCommissionSetting.getId());
        // Lifecycle fields belong to order binding/settlement, not the generic edit form.
        goodsExtraCommissionSetting.setIsLocked(null);
        goodsExtraCommissionSetting.setStatus(null);
        goodsExtraCommissionSetting.setCreateTime(null);
        int updated = goodsExtraCommissionSettingMapper.updateGoodsExtraCommissionSetting(
                goodsExtraCommissionSetting);
        if (updated != 1) {
            throw new ServiceException("额外佣金配置状态已变更，请刷新后重试");
        }
        return updated;
    }

    /**
     * 批量删除额外佣金设置
     * 
     * @param ids 需要删除的额外佣金设置主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteGoodsExtraCommissionSettingByIds(Long[] ids)
    {
        Long[] normalizedIds = normalizeIds(ids);
        for (Long id : normalizedIds) {
            requireEditable(id);
        }
        int deleted = goodsExtraCommissionSettingMapper.deleteGoodsExtraCommissionSettingByIds(
                normalizedIds);
        if (deleted != normalizedIds.length) {
            throw new ServiceException("额外佣金配置状态已变更，请刷新后重试");
        }
        return deleted;
    }

    /**
     * 删除额外佣金设置信息
     * 
     * @param id 额外佣金设置主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteGoodsExtraCommissionSettingById(Long id)
    {
        requireEditable(id);
        int deleted = goodsExtraCommissionSettingMapper.deleteGoodsExtraCommissionSettingById(id);
        if (deleted != 1) {
            throw new ServiceException("额外佣金配置状态已变更，请刷新后重试");
        }
        return deleted;
    }

    private GoodsExtraCommissionSetting requireEditable(Long id) {
        if (id == null) {
            throw new ServiceException("额外佣金配置ID不能为空");
        }
        GoodsExtraCommissionSetting current =
                goodsExtraCommissionSettingMapper.selectByIdForUpdate(id);
        if (current == null) {
            throw new ServiceException("额外佣金配置不存在");
        }
        if (!"1".equals(current.getStatus()) || !"1".equals(current.getIsLocked())) {
            throw new ServiceException("已绑定或已完成的额外佣金配置不能修改或删除");
        }
        return current;
    }

    private Long[] normalizeIds(Long[] ids) {
        if (ids == null || ids.length == 0 || Arrays.stream(ids).anyMatch(Objects::isNull)) {
            throw new ServiceException("额外佣金配置ID不能为空");
        }
        return Arrays.stream(ids).distinct().sorted().toArray(Long[]::new);
    }
}
