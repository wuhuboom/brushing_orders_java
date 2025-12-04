package com.order.member.service;

import java.util.List;
import com.order.member.domain.GoodsRechargeRecord;

/**
 * 充值记录Service接口
 * 
 * @author order
 * @date 2025-10-27
 */
public interface IGoodsRechargeRecordService 
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
     * 批量删除充值记录
     * 
     * @param ids 需要删除的充值记录主键集合
     * @return 结果
     */
    public int deleteGoodsRechargeRecordByIds(Long[] ids);

    /**
     * 删除充值记录信息
     * 
     * @param id 充值记录主键
     * @return 结果
     */
    public int deleteGoodsRechargeRecordById(Long id);
}
