package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.OrderNoGenerator;
import com.brushing.common.utils.SnowflakeIdGenerator;
import com.brushing.member.domain.OrderAccountChange;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.mapper.OrderAccountChangeMapper;
import com.brushing.member.mapper.OrderMemberUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderTopupMapper;
import com.brushing.member.domain.OrderTopup;
import com.brushing.member.domain.OrderWithdrawal;
import com.brushing.member.domain.OrderBankWallet;
import com.brushing.member.mapper.OrderWithdrawalMapper;
import com.brushing.member.mapper.OrderBankWalletMapper;
import com.brushing.member.service.IOrderTopupService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import com.brushing.common.utils.ip.IpUtils;

/**
 * 充值记录Service业务层处理
 * 
 * @author brushing
 * @date 2025-08-04
 */
@Service
@Transactional
public class OrderTopupServiceImpl implements IOrderTopupService 
{
    @Autowired
    private OrderTopupMapper orderTopupMapper;

    @Autowired
    private OrderAccountChangeMapper orderAccountChangeMapper;

    @Autowired
    private OrderMemberUserMapper orderMemberUserMapper;

    @Autowired
    private OrderWithdrawalMapper orderWithdrawalMapper;

    @Autowired
    private OrderBankWalletMapper orderBankWalletMapper;

    /**
     * 查询充值记录
     * 
     * @param id 充值记录主键
     * @return 充值记录
     */
    @Override
    public OrderTopup selectOrderTopupById(Long id)
    {
        return orderTopupMapper.selectOrderTopupById(id);
    }

    /**
     * 查询充值记录列表
     * 
     * @param orderTopup 充值记录
     * @return 充值记录
     */
    @Override
    public List<OrderTopup> selectOrderTopupList(OrderTopup orderTopup)
    {
        return orderTopupMapper.selectOrderTopupList(orderTopup);
    }

    @Override
    public List<OrderTopup> selectOrderTopupByUserId(Long userId) {
        return orderTopupMapper.selectOrderTopupByUserId(userId);
    }

    /**
     * 新增充值记录
     * 
     * @param orderTopup 充值记录
     * @return 结果
     */
    @Override
    public int insertOrderTopup(OrderTopup orderTopup)
    {
        String code = generateUniqueTopupNo();
        if (code == null) {
            throw new ServiceException("Please try again later");
        }
        orderTopup.setCode(code);
        orderTopup.setCreateTime(DateUtils.getNowDate());
        return orderTopupMapper.insertOrderTopup(orderTopup);
    }

    /**
     * 修改充值记录
     * 
     * @param orderTopup 充值记录
     * @return 结果
     */
    @Override
    public int updateOrderTopup(OrderTopup orderTopup)
    {
        if (orderTopup.getStatus().equals("0")){
            OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserById(orderTopup.getUserId());
            BigDecimal balance = orderMemberUser.getBalance();
            BigDecimal add = balance.add(orderTopup.getRealMoney());
            recordAccountChange(orderTopup.getUserId(),orderMemberUser.getUsername(),"2",balance,orderTopup.getRealMoney(),add,"增加用户余额, 操作","0");
            //保存用户信息
            orderMemberUser.setBalance(add);
            orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
        }
        return orderTopupMapper.updateOrderTopup(orderTopup);
    }

    /**
     * 批量删除充值记录
     * 
     * @param ids 需要删除的充值记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderTopupByIds(Long[] ids)
    {
        return orderTopupMapper.deleteOrderTopupByIds(ids);
    }

    /**
     * 删除充值记录信息
     * 
     * @param id 充值记录主键
     * @return 结果
     */
    @Override
    public int deleteOrderTopupById(Long id)
    {
        return orderTopupMapper.deleteOrderTopupById(id);
    }

    @Override
    public int upOrDown(Long userId, BigDecimal amount, String type, String requestIp, String requestIpAddress) {

        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserById(userId);
        //上
        if (type.equals("0")){
            BigDecimal balance = orderMemberUser.getBalance();
            BigDecimal add = balance.add(amount);
            //记录账变信息
            recordAccountChange(userId,orderMemberUser.getUsername(),"2",balance,amount,add,"增加用户余额, 操作","0");
            //保存用户信息
            orderMemberUser.setBalance(add);
            orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
            //充值记录
           return setLog(userId,amount);
        }else{
            //当前余额
            BigDecimal balance = orderMemberUser.getBalance();
            //变动后的余额
            BigDecimal subtract = balance.subtract(amount);
            //变动金额
            BigDecimal subtract1 = new BigDecimal("0").subtract(amount);
            //记录账变信息
            recordAccountChange(userId,orderMemberUser.getUsername(),"2",balance,subtract1,subtract,"减少用户余额, 操作","0");
            //保存用户信息
            orderMemberUser.setBalance(subtract);
            orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
            //提现记录
            return setWithdrawLog(userId,amount,requestIp,requestIpAddress);
        }
    }

    @Override
    public int uPamount(Long userId, BigDecimal amount, Long adminId, String adminUsername) {
        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserById(userId);
        BigDecimal balance = orderMemberUser.getBalance();
        BigDecimal add = balance.add(amount);
        //记录账变信息
        recordAccountChange(userId,orderMemberUser.getUsername(),"8",balance,amount,add,"后台管理员ID: "+adminId+", 管理员用户名: "+adminUsername+"," +
                " 赠送,增加用户余额, 操作金额为"+amount,"1");
        //保存用户信息
        orderMemberUser.setBalance(add);

        return orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
    }

    @Override
    public int updateAmount(Long userId, BigDecimal amount, Long adminId, String adminUsername) {
        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserById(userId);
        BigDecimal balance = orderMemberUser.getBalance();
        //记录账变信息
        recordAccountChange(userId,orderMemberUser.getUsername(),"2",balance,amount,amount,"后台管理员ID: "+adminId+", 管理员用户名: "+adminUsername+"," +
                " 覆盖用户余额, 操作金额为：:"+amount,"1");
        orderMemberUser.setBalance(amount);
        orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);

          return setLog(userId,amount);
    }

    private void recordAccountChange(Long userId, String username, String changeType,
                                           BigDecimal beforeAmount, BigDecimal changeAmount,
                                           BigDecimal afterAmount, String action,String type) {
        String changeNo = generateUniqueChangeNo();
        if (changeNo == null) {
            throw new ServiceException("Please try again later");
        }

        OrderAccountChange change = new OrderAccountChange();
        change.setChangeNo(changeNo);
        change.setType(changeType);
        change.setUserId(userId);
        change.setBeforeAmount(beforeAmount);
        change.setChangeAmount(changeAmount);
        change.setAfterAmount(afterAmount);
        if (type.equals("0")){
            String s = buildChangeDescription(userId, username, action, changeAmount);
            change.setDescription(s);
        }else{
            change.setDescription(action);
        }
        change.setCreateTime(DateUtils.getNowDate());
        orderAccountChangeMapper.insertOrderAccountChange(change);
    }

    private String generateUniqueChangeNo() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String changeNo = OrderNoGenerator.generateOrderId();
            if (orderAccountChangeMapper.selectOrderAccountChangeByCode(changeNo) == null) {
                return changeNo;
            }
        }
        return null;
    }

    private String buildChangeDescription(Long userId, String username, String action, BigDecimal amount) {
        return String.format("用户ID: %d, 用户名: %s, %s, 金额: %s",
                userId, username, action, amount.toPlainString());
    }

    public int setLog(Long userId,BigDecimal changeAmount){
        String code = generateUniqueTopupNo();
        if (code == null) {
            throw new ServiceException("Please try again later");
        }
        OrderTopup orderTopup =new OrderTopup();
        orderTopup.setAmout(changeAmount);
        orderTopup.setUserId(userId);
        orderTopup.setCode(code);
        orderTopup.setType("0");
        orderTopup.setStatus("0");
        orderTopup.setCreateTime(DateUtils.getNowDate());
      return  orderTopupMapper.insertOrderTopup(orderTopup);
    }

    public int setWithdrawLog(Long userId, BigDecimal changeAmount, String requestIp, String requestIpAddress) {
        String code = generateUniqueWithdrawNo();
        if (code == null) {
            throw new ServiceException("Please try again later");
        }
        OrderWithdrawal withdrawal = new OrderWithdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setCode(code);
        withdrawal.setAmount(changeAmount);

        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserById(userId);
        if (orderMemberUser != null && orderMemberUser.getUserLevel() != null && orderMemberUser.getUserLevel().getWithdrawFee() != null) {
            BigDecimal feePercent = orderMemberUser.getUserLevel().getWithdrawFee();
            BigDecimal fee = calculateFee(changeAmount, feePercent);
            withdrawal.setCreditedAmount(changeAmount.subtract(fee).setScale(2, java.math.RoundingMode.HALF_UP));
            withdrawal.setFee(fee);
            withdrawal.setWithdrawFee(feePercent);
        } else {
            withdrawal.setCreditedAmount(changeAmount);
            withdrawal.setFee(BigDecimal.ZERO);
            withdrawal.setWithdrawFee(BigDecimal.ZERO);
        }

        withdrawal.setStatus("0"); // 审核通过 (0为通过)

        // 获取用户默认钱包地址
        OrderBankWallet queryWallet = new OrderBankWallet();
        queryWallet.setUserId(userId);
        List<OrderBankWallet> walletList = orderBankWalletMapper.selectOrderBankWalletList(queryWallet);
        if (!CollectionUtils.isEmpty(walletList)) {
            OrderBankWallet wallet = walletList.get(0);
            withdrawal.setWalletId(wallet.getId());
            withdrawal.setWithdrawAddress(wallet.getWalletAddress() != null ? wallet.getWalletAddress() : wallet.getBankCard()); //设置地址
        }

        withdrawal.setIp(requestIp);
        withdrawal.setIpAddress(requestIpAddress);

        withdrawal.setCreateTime(DateUtils.getNowDate());
        withdrawal.setRemark("后台下分操作");
        withdrawal.setAuditTime(DateUtils.getNowDate());
        return orderWithdrawalMapper.insertOrderWithdrawal(withdrawal);
    }

    private String generateUniqueWithdrawNo() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String code = OrderNoGenerator.generateOrderId();
            if (orderWithdrawalMapper.selectOrderWithdrawalByCode(code) == null) {
                return code;
            }
        }
        return null;
    }

    private String generateUniqueTopupNo() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
            long l = generator.nextId();
            String code = String.valueOf(l);
            if (orderAccountChangeMapper.selectOrderAccountChangeByCode(code) == null) {
                return code;
            }
        }
        return null;
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal feePercent) {
        if (feePercent != null && feePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal feeRate = feePercent.divide(new BigDecimal(100), 8, java.math.RoundingMode.HALF_UP);
            return amount.multiply(feeRate).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
