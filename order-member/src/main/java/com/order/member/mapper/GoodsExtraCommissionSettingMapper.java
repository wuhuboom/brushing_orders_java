package com.order.member.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.order.member.domain.GoodsExtraCommissionSetting;
import org.apache.ibatis.annotations.Param;

/**
 * 额外佣金设置Mapper接口
 * 
 * @author order
 * @date 2025-11-08
 */
public interface GoodsExtraCommissionSettingMapper 
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
     * Lock the first incomplete, unlocked setting matching an order.
     */
    GoodsExtraCommissionSetting selectAvailableForUpdate(
            @Param("userId") Long userId,
            @Param("orderCount") Long orderCount,
            @Param("productPrice") BigDecimal productPrice);

    /**
     * Reserve a matched setting for the order being created.
     */
    int reserveForOrder(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("orderCount") Long orderCount,
            @Param("productPrice") BigDecimal productPrice);

    /**
     * Lock a previously reserved setting before settlement.
     */
    GoodsExtraCommissionSetting selectReservedForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("orderCount") Long orderCount,
            @Param("productPrice") BigDecimal productPrice);

    /**
     * Complete a reserved setting exactly once.
     */
    int completeReserved(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("orderCount") Long orderCount,
            @Param("productPrice") BigDecimal productPrice);

    /**
     * Release an incomplete setting when its pending order is cancelled.
     */
    int releaseReserved(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("orderCount") Long orderCount,
            @Param("productPrice") BigDecimal productPrice);

    /**
     * Lock a setting before an administrative edit or delete decision.
     */
    GoodsExtraCommissionSetting selectByIdForUpdate(@Param("id") Long id);

    /**
     * 删除额外佣金设置
     * 
     * @param id 额外佣金设置主键
     * @return 结果
     */
    public int deleteGoodsExtraCommissionSettingById(Long id);

    /**
     * 批量删除额外佣金设置
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsExtraCommissionSettingByIds(Long[] ids);
}
