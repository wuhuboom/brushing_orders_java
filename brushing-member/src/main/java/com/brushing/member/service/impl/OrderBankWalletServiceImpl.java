package com.brushing.member.service.impl;

import java.util.List;
import com.brushing.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderBankWalletMapper;
import com.brushing.member.domain.OrderBankWallet;
import com.brushing.member.service.IOrderBankWalletService;

/**
 * 银行钱包Service业务层处理
 * 
 * @author brushing
 * @date 2025-10-13
 */
@Service
public class OrderBankWalletServiceImpl implements IOrderBankWalletService 
{
    @Autowired
    private OrderBankWalletMapper orderBankWalletMapper;

    /**
     * 查询银行钱包
     * 
     * @param id 银行钱包主键
     * @return 银行钱包
     */
    @Override
    public OrderBankWallet selectOrderBankWalletById(Long id)
    {
        return orderBankWalletMapper.selectOrderBankWalletById(id);
    }

    /**
     * 查询银行钱包列表
     * 
     * @param orderBankWallet 银行钱包
     * @return 银行钱包
     */
    @Override
    public List<OrderBankWallet> selectOrderBankWalletList(OrderBankWallet orderBankWallet)
    {
        return orderBankWalletMapper.selectOrderBankWalletList(orderBankWallet);
    }

    /**
     * 新增银行钱包
     * 
     * @param orderBankWallet 银行钱包
     * @return 结果
     */
    @Override
    public int insertOrderBankWallet(OrderBankWallet orderBankWallet)
    {
        orderBankWallet.setCreateTime(DateUtils.getNowDate());
        return orderBankWalletMapper.insertOrderBankWallet(orderBankWallet);
    }

    /**
     * 修改银行钱包
     * 
     * @param orderBankWallet 银行钱包
     * @return 结果
     */
    @Override
    public int updateOrderBankWallet(OrderBankWallet orderBankWallet)
    {
        return orderBankWalletMapper.updateOrderBankWallet(orderBankWallet);
    }

    /**
     * 批量删除银行钱包
     * 
     * @param ids 需要删除的银行钱包主键
     * @return 结果
     */
    @Override
    public int deleteOrderBankWalletByIds(Long[] ids)
    {
        return orderBankWalletMapper.deleteOrderBankWalletByIds(ids);
    }

    /**
     * 删除银行钱包信息
     * 
     * @param id 银行钱包主键
     * @return 结果
     */
    @Override
    public int deleteOrderBankWalletById(Long id)
    {
        return orderBankWalletMapper.deleteOrderBankWalletById(id);
    }
}
