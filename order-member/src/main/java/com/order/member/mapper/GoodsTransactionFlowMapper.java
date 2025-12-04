package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.GoodsTransactionFlow;

/**
 * 交易流水Mapper接口
 * 
 * @author order
 * @date 2025-10-27
 */
public interface GoodsTransactionFlowMapper 
{
    /**
     * 查询交易流水
     * 
     * @param id 交易流水主键
     * @return 交易流水
     */
    public GoodsTransactionFlow selectGoodsTransactionFlowById(Long id);

    /**
     * 查询交易流水列表
     * 
     * @param goodsTransactionFlow 交易流水
     * @return 交易流水集合
     */
    public List<GoodsTransactionFlow> selectGoodsTransactionFlowList(GoodsTransactionFlow goodsTransactionFlow);

    /**
     * 新增交易流水
     * 
     * @param goodsTransactionFlow 交易流水
     * @return 结果
     */
    public int insertGoodsTransactionFlow(GoodsTransactionFlow goodsTransactionFlow);

    /**
     * 修改交易流水
     * 
     * @param goodsTransactionFlow 交易流水
     * @return 结果
     */
    public int updateGoodsTransactionFlow(GoodsTransactionFlow goodsTransactionFlow);

    /**
     * 删除交易流水
     * 
     * @param id 交易流水主键
     * @return 结果
     */
    public int deleteGoodsTransactionFlowById(Long id);

    /**
     * 批量删除交易流水
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoodsTransactionFlowByIds(Long[] ids);
}
