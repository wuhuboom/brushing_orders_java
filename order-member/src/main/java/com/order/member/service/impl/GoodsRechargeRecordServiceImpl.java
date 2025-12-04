package com.order.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsRechargeRecordMapper;
import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.service.IGoodsRechargeRecordService;

/**
 * 充值记录Service业务层处理
 * 
 * @author order
 * @date 2025-10-27
 */
@Service
public class GoodsRechargeRecordServiceImpl implements IGoodsRechargeRecordService 
{
    @Autowired
    private GoodsRechargeRecordMapper goodsRechargeRecordMapper;

    /**
     * 查询充值记录
     * 
     * @param id 充值记录主键
     * @return 充值记录
     */
    @Override
    public GoodsRechargeRecord selectGoodsRechargeRecordById(Long id)
    {
        return goodsRechargeRecordMapper.selectGoodsRechargeRecordById(id);
    }

    /**
     * 查询充值记录列表
     * 
     * @param goodsRechargeRecord 充值记录
     * @return 充值记录
     */
    @Override
    public List<GoodsRechargeRecord> selectGoodsRechargeRecordList(GoodsRechargeRecord goodsRechargeRecord)
    {
        return goodsRechargeRecordMapper.selectGoodsRechargeRecordList(goodsRechargeRecord);
    }

    /**
     * 新增充值记录
     * 
     * @param goodsRechargeRecord 充值记录
     * @return 结果
     */
    @Override
    public int insertGoodsRechargeRecord(GoodsRechargeRecord goodsRechargeRecord)
    {
        return goodsRechargeRecordMapper.insertGoodsRechargeRecord(goodsRechargeRecord);
    }

    /**
     * 修改充值记录
     * 
     * @param goodsRechargeRecord 充值记录
     * @return 结果
     */
    @Override
    public int updateGoodsRechargeRecord(GoodsRechargeRecord goodsRechargeRecord)
    {
        return goodsRechargeRecordMapper.updateGoodsRechargeRecord(goodsRechargeRecord);
    }

    /**
     * 批量删除充值记录
     * 
     * @param ids 需要删除的充值记录主键
     * @return 结果
     */
    @Override
    public int deleteGoodsRechargeRecordByIds(Long[] ids)
    {
        return goodsRechargeRecordMapper.deleteGoodsRechargeRecordByIds(ids);
    }

    /**
     * 删除充值记录信息
     * 
     * @param id 充值记录主键
     * @return 结果
     */
    @Override
    public int deleteGoodsRechargeRecordById(Long id)
    {
        return goodsRechargeRecordMapper.deleteGoodsRechargeRecordById(id);
    }
}
