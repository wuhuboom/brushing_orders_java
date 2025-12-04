package com.order.member.service;

import java.util.List;
import com.order.member.domain.GoodsWithdrawalAccount;

/**
 * 提现账户Service接口
 * 
 * @author order
 * @date 2025-11-05
 */
public interface IGoodsWithdrawalAccountService 
{
    /**
     * 查询提现账户
     * 
     * @param id 提现账户主键
     * @return 提现账户
     */
    public GoodsWithdrawalAccount selectGoodsWithdrawalAccountById(Long id);

    /**
     * 查询提现账户列表
     * 
     * @param goodsWithdrawalAccount 提现账户
     * @return 提现账户集合
     */
    public List<GoodsWithdrawalAccount> selectGoodsWithdrawalAccountList(GoodsWithdrawalAccount goodsWithdrawalAccount);

    /**
     * 新增提现账户
     * 
     * @param goodsWithdrawalAccount 提现账户
     * @return 结果
     */
    public int insertGoodsWithdrawalAccount(GoodsWithdrawalAccount goodsWithdrawalAccount);

    /**
     * 修改提现账户
     * 
     * @param goodsWithdrawalAccount 提现账户
     * @return 结果
     */
    public int updateGoodsWithdrawalAccount(GoodsWithdrawalAccount goodsWithdrawalAccount);

    /**
     * 批量删除提现账户
     * 
     * @param ids 需要删除的提现账户主键集合
     * @return 结果
     */
    public int deleteGoodsWithdrawalAccountByIds(Long[] ids);

    /**
     * 删除提现账户信息
     * 
     * @param id 提现账户主键
     * @return 结果
     */
    public int deleteGoodsWithdrawalAccountById(Long id);
}
