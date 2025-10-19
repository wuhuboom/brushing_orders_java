package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderBankWallet;

/**
 * 银行钱包Service接口
 * 
 * @author brushing
 * @date 2025-10-13
 */
public interface IOrderBankWalletService 
{
    /**
     * 查询银行钱包
     * 
     * @param id 银行钱包主键
     * @return 银行钱包
     */
    public OrderBankWallet selectOrderBankWalletById(Long id);

    /**
     * 查询银行钱包列表
     * 
     * @param orderBankWallet 银行钱包
     * @return 银行钱包集合
     */
    public List<OrderBankWallet> selectOrderBankWalletList(OrderBankWallet orderBankWallet);

    /**
     * 新增银行钱包
     * 
     * @param orderBankWallet 银行钱包
     * @return 结果
     */
    public int insertOrderBankWallet(OrderBankWallet orderBankWallet);

    /**
     * 修改银行钱包
     * 
     * @param orderBankWallet 银行钱包
     * @return 结果
     */
    public int updateOrderBankWallet(OrderBankWallet orderBankWallet);

    /**
     * 批量删除银行钱包
     * 
     * @param ids 需要删除的银行钱包主键集合
     * @return 结果
     */
    public int deleteOrderBankWalletByIds(Long[] ids);

    /**
     * 删除银行钱包信息
     * 
     * @param id 银行钱包主键
     * @return 结果
     */
    public int deleteOrderBankWalletById(Long id);
}
