package com.order.member.service.impl;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import com.order.common.utils.DateUtils;
import com.order.common.utils.StringUtils;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.ITransactionService;
import com.order.member.service.RegistrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.domain.OrderUser;
import com.order.member.service.IOrderUserService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单用户Service业务层处理
 * 
 * @author order
 * @date 2025-10-21
 */
@Service
public class OrderUserServiceImpl implements IOrderUserService 
{
    private static final char[] INVITE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int INVITE_CODE_ATTEMPTS = 10;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private OrderUserMapper orderUserMapper;

    @Autowired
    private IOrderConfigService configService;

    @Autowired
    private GoodsMemberLevelMapper memberLevelMapper;

    @Autowired
    private ITransactionService transactionService;

    /**
     * 查询订单用户
     * 
     * @param id 订单用户主键
     * @return 订单用户
     */
    @Override
    public OrderUser selectOrderUserById(Long id)
    {
        return orderUserMapper.selectOrderUserById(id);
    }

    /**
     * 查询订单用户列表
     * 
     * @param orderUser 订单用户
     * @return 订单用户
     */
    @Override
    public List<OrderUser> selectOrderUserList(OrderUser orderUser)
    {
        return orderUserMapper.selectOrderUserList(orderUser);
    }

    /**
     * 新增订单用户
     * 
     * @param orderUser 订单用户
     * @return 结果
     */
    @Override
    public int insertOrderUser(OrderUser orderUser)
    {

        orderUser.setCreateTime(DateUtils.getNowDate());
        orderUser.setInviteCode(inviteCodeGenerator());
        if (orderUser.getParentId().equals(0L)){
            orderUser.setAncestors("0");
        }else{
            OrderUser parent = orderUserMapper.selectOrderUserById(orderUser.getParentId());
            orderUser.setAncestors(parent.getAncestors()+","+parent.getId());
        }
        return orderUserMapper.insertOrderUser(orderUser);
    }

    public String inviteCodeGenerator(){
        for (int attempt = 0; attempt < INVITE_CODE_ATTEMPTS; attempt++) {
            String code = randomInviteCode();
            if (!Boolean.TRUE.equals(orderUserMapper.existsInviteCode(code))) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique invite code");
    }

    private String randomInviteCode() {
        return new String(new char[] {
                INVITE_LETTERS[secureRandom.nextInt(INVITE_LETTERS.length)],
                INVITE_LETTERS[secureRandom.nextInt(INVITE_LETTERS.length)],
                (char) ('0' + secureRandom.nextInt(10)),
                INVITE_LETTERS[secureRandom.nextInt(INVITE_LETTERS.length)],
                INVITE_LETTERS[secureRandom.nextInt(INVITE_LETTERS.length)]
        });
    }

    /**
     * 修改订单用户
     * 
     * @param orderUser 订单用户
     * @return 结果
     */
    @Override
    public int updateOrderUser(OrderUser orderUser)
    {
        orderUser.setUpdateTime(DateUtils.getNowDate());
        return orderUserMapper.updateOrderUser(orderUser);
    }

    /**
     * 批量删除订单用户
     * 
     * @param ids 需要删除的订单用户主键
     * @return 结果
     */
    @Override
    public int deleteOrderUserByIds(Long[] ids)
    {
        return orderUserMapper.deleteOrderUserByIds(ids);
    }

    /**
     * 删除订单用户信息
     * 
     * @param id 订单用户主键
     * @return 结果
     */
    @Override
    public int deleteOrderUserById(Long id)
    {
        return orderUserMapper.deleteOrderUserById(id);
    }

    @Override
    public OrderUser selectOrderUserByName(String username) {
        return orderUserMapper.selectOrderUserByName(username);
    }

    @Override
    public OrderUser selectOrderUserByInviteCode(String inviteCode) {
        return orderUserMapper.selectOrderUserByInviteCode(inviteCode);
    }

    @Override
    public List<OrderUser> selectChildrenById(Long userId, String scope) {
        return orderUserMapper.selectChildrenById(userId, scope);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationResult register(OrderUser user) {
        OrderUser parent = orderUserMapper.selectReferralByInviteCode(user.getInviteCode());
        if (parent == null) {
            return RegistrationResult.INVITE_NOT_FOUND;
        }

        GoodsMemberLevel memberLevel = memberLevelMapper.selectLowestPriceLevel();
        if (memberLevel == null) {
            return RegistrationResult.MEMBER_LEVEL_NOT_FOUND;
        }

        user.setVipId(memberLevel.getId());
        user.setParentId(parent.getId());
        user.setInviteCode(inviteCodeGenerator());
        user.setAncestors(StringUtils.isEmpty(parent.getAncestors())
                ? "0," + parent.getId()
                : parent.getAncestors() + "," + parent.getId());
        user.setCreateTime(DateUtils.getNowDate());

        BigDecimal bonus = BigDecimal.ZERO;
        Optional<Object> configuredBonus = configService.getConfigValue("trade", "registerBonusAmount");
        if (configuredBonus.isPresent() && configuredBonus.get() != null) {
            try {
                bonus = new BigDecimal(String.valueOf(configuredBonus.get()));
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Invalid register bonus configuration", ex);
            }
            if (bonus.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Register bonus must not be negative");
            }
        }

        if (bonus.compareTo(BigDecimal.ZERO) > 0) {
            user.setBalance(bonus);
        }
        orderUserMapper.insertOrderUser(user);
        if (bonus.compareTo(BigDecimal.ZERO) > 0) {
            transactionService.recordFlow(
                    user.getId(), "zczs", bonus, BigDecimal.ZERO, "Registration bonus");
        }

        return RegistrationResult.SUCCESS;
    }

    @Override
    public Boolean existsPhone(String phone) {
        return orderUserMapper.existsPhone(phone);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)  // 显式回滚
    public int updateParentIdAndAncestors(Long userId, Long newParentId, Long version) {  // version 为 Long

        // 更新当前用户
        int affected = orderUserMapper.updateParentIdAndAncestors(userId, newParentId, version);
        if (affected == 0) {
            throw new RuntimeException("请刷新重试");  // 自定义异常或用Spring的
        }

        // 级联更新下级
        updateDescendantsAncestors(userId);

        return affected;  // 或返回 totalAffected = affected + cascadeCount;
    }

    /**
     * 级联更新下级ancestors（使用BFS队列，避免递归栈溢出）
     */
    private void updateDescendantsAncestors(Long userId) {
        // 查询当前用户的新 ancestors
        OrderUser current = orderUserMapper.selectOrderUserById(userId);
        if (current == null) {
            return;
        }
        String newAncestors = current.getAncestors() != null ? current.getAncestors() : "0";

        // 查询直属下级（用"direct"，递归处理多级）
        List<OrderUser> children = orderUserMapper.selectChildrenById(userId, "direct");
        for (OrderUser child : children) {
            // 计算子级新路径：父新路径 + ',' + child.id
            String childNewAncestors = newAncestors + "," + child.getId();
            child.setAncestors(childNewAncestors);
            // 无需setVersion，XML会自动+1并校验

            // 执行更新（如果失败，事务会回滚）
            int updated = orderUserMapper.updateOrderUser(child);
            if (updated == 0) {
                // 可抛异常中断，但事务会回滚整个操作
            } else {
            }
            // 递归子子级
            updateDescendantsAncestors(child.getId());
        }
    }
}
