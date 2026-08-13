package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.GoodsRechargeRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 充值记录Mapper接口
 * 
 * @author order
 * @date 2025-10-27
 */
public interface GoodsRechargeRecordMapper 
{
    /**
     * 查询充值记录
     * 
     * @param id 充值记录主键
     * @return 充值记录
     */
    public GoodsRechargeRecord selectGoodsRechargeRecordById(Long id);

    /**
     * 查询充值记录列表
     * 
     * @param goodsRechargeRecord 充值记录
     * @return 充值记录集合
     */
    public List<GoodsRechargeRecord> selectGoodsRechargeRecordList(GoodsRechargeRecord goodsRechargeRecord);

    /**
     * 新增充值记录
     * 
     * @param goodsRechargeRecord 充值记录
     * @return 结果
     */
    public int insertGoodsRechargeRecord(GoodsRechargeRecord goodsRechargeRecord);

    /**
     * 修改充值记录
     * 
     * @param goodsRechargeRecord 充值记录
     * @return 结果
     */
    public int updateGoodsRechargeRecord(GoodsRechargeRecord goodsRechargeRecord);

    /**
     * 删除充值记录
     * 
     * @param id 充值记录主键
     * @return 结果
     */
    public int deleteGoodsRechargeRecordById(Long id);

    /**
     * 批量删除充值记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsRechargeRecordByIds(Long[] ids);

    List<GoodsRechargeRecord> selectPublicByUserId(@Param("userId") Long userId);

    GoodsRechargeRecord selectForUpdate(@Param("id") Long id);

    int transitionStatus(
            @Param("id") Long id,
            @Param("expectedStatus") String expectedStatus,
            @Param("targetStatus") String targetStatus,
            @Param("remark") String remark,
            @Param("updateBy") String updateBy);
}
