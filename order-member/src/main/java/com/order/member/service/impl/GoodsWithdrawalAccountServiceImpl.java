package com.order.member.service.impl;

import java.util.List;
import com.order.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.GoodsWithdrawalAccountMapper;
import com.order.member.domain.GoodsWithdrawalAccount;
import com.order.member.service.IGoodsWithdrawalAccountService;

/**
 * 提现账户Service业务层处理
 * 
 * @author order
 * @date 2025-11-05
 */
@Service
public class GoodsWithdrawalAccountServiceImpl implements IGoodsWithdrawalAccountService 
{
    @Autowired
    private GoodsWithdrawalAccountMapper goodsWithdrawalAccountMapper;

    /**
     * 查询提现账户
     * 
     * @param id 提现账户主键
     * @return 提现账户
     */
    @Override
    public GoodsWithdrawalAccount selectGoodsWithdrawalAccountById(Long id)
    {
        return goodsWithdrawalAccountMapper.selectGoodsWithdrawalAccountById(id);
    }

    /**
     * 查询提现账户列表
     * 
     * @param goodsWithdrawalAccount 提现账户
     * @return 提现账户
     */
    @Override
    public List<GoodsWithdrawalAccount> selectGoodsWithdrawalAccountList(GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        return goodsWithdrawalAccountMapper.selectGoodsWithdrawalAccountList(goodsWithdrawalAccount);
    }

    /**
     * 新增提现账户
     * 
     * @param goodsWithdrawalAccount 提现账户
     * @return 结果
     */
    @Override
    public int insertGoodsWithdrawalAccount(GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        goodsWithdrawalAccount.setCreateTime(DateUtils.getNowDate());
        return goodsWithdrawalAccountMapper.insertGoodsWithdrawalAccount(goodsWithdrawalAccount);
    }

    /**
     * 修改提现账户
     * 
     * @param goodsWithdrawalAccount 提现账户
     * @return 结果
     */
    @Override
    public int updateGoodsWithdrawalAccount(GoodsWithdrawalAccount goodsWithdrawalAccount)
    {
        return goodsWithdrawalAccountMapper.updateGoodsWithdrawalAccount(goodsWithdrawalAccount);
    }

    /**
     * 批量删除提现账户
     * 
     * @param ids 需要删除的提现账户主键
     * @return 结果
     */
    @Override
    public int deleteGoodsWithdrawalAccountByIds(Long[] ids)
    {
        return goodsWithdrawalAccountMapper.deleteGoodsWithdrawalAccountByIds(ids);
    }

    /**
     * 删除提现账户信息
     * 
     * @param id 提现账户主键
     * @return 结果
     */
    @Override
    public int deleteGoodsWithdrawalAccountById(Long id)
    {
        return goodsWithdrawalAccountMapper.deleteGoodsWithdrawalAccountById(id);
    }
}
