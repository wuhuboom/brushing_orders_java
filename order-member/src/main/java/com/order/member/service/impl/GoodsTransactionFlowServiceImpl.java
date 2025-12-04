package com.order.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsTransactionFlowMapper;
import com.order.member.domain.GoodsTransactionFlow;
import com.order.member.service.IGoodsTransactionFlowService;

/**
 * 交易流水Service业务层处理
 * 
 * @author order
 * @date 2025-10-27
 */
@Service
public class GoodsTransactionFlowServiceImpl implements IGoodsTransactionFlowService 
{
    @Autowired
    private GoodsTransactionFlowMapper goodsTransactionFlowMapper;

    /**
     * 查询交易流水
     * 
     * @param id 交易流水主键
     * @return 交易流水
     */
    @Override
    public GoodsTransactionFlow selectGoodsTransactionFlowById(Long id)
    {
        return goodsTransactionFlowMapper.selectGoodsTransactionFlowById(id);
    }

    /**
     * 查询交易流水列表
     * 
     * @param goodsTransactionFlow 交易流水
     * @return 交易流水
     */
    @Override
    public List<GoodsTransactionFlow> selectGoodsTransactionFlowList(GoodsTransactionFlow goodsTransactionFlow)
    {
        return goodsTransactionFlowMapper.selectGoodsTransactionFlowList(goodsTransactionFlow);
    }

    /**
     * 新增交易流水
     * 
     * @param goodsTransactionFlow 交易流水
     * @return 结果
     */
    @Override
    public int insertGoodsTransactionFlow(GoodsTransactionFlow goodsTransactionFlow)
    {
        return goodsTransactionFlowMapper.insertGoodsTransactionFlow(goodsTransactionFlow);
    }

    /**
     * 修改交易流水
     * 
     * @param goodsTransactionFlow 交易流水
     * @return 结果
     */
    @Override
    public int updateGoodsTransactionFlow(GoodsTransactionFlow goodsTransactionFlow)
    {
        return goodsTransactionFlowMapper.updateGoodsTransactionFlow(goodsTransactionFlow);
    }

    /**
     * 批量删除交易流水
     * 
     * @param ids 需要删除的交易流水主键
     * @return 结果
     */
    @Override
    public int deleteGoodsTransactionFlowByIds(Long[] ids)
    {
        return goodsTransactionFlowMapper.deleteGoodsTransactionFlowByIds(ids);
    }

    /**
     * 删除交易流水信息
     * 
     * @param id 交易流水主键
     * @return 结果
     */
    @Override
    public int deleteGoodsTransactionFlowById(Long id)
    {
        return goodsTransactionFlowMapper.deleteGoodsTransactionFlowById(id);
    }
}
