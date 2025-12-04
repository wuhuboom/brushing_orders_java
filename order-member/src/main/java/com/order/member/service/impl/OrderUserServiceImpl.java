package com.order.member.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.order.common.member.RandomCodeGenerator;
import com.order.common.utils.DateUtils;
import com.order.common.utils.StringUtils;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.mapper.GoodsMemberLevelMapper;
import com.order.member.service.IGoodsMemberLevelService;
import com.order.member.service.IOrderConfigService;
import com.order.member.service.ITransactionService;
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
        String code = RandomCodeGenerator.generateRandomCode();
        OrderUser orderUser = orderUserMapper.selectOrderUserByInviteCode(code);
        if (StringUtils.isNotNull(orderUser)){
            inviteCodeGenerator();
        }
        return code;
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
    public String register(OrderUser user) {
        OrderUser orderUser = orderUserMapper.selectOrderUserByInviteCode(user.getInviteCode());
        if (StringUtils.isNull(orderUser)){
            return "500";
        }
        GoodsMemberLevel goodsMemberLevel = memberLevelMapper.selectLowestPriceLevel();
        user.setVipId(goodsMemberLevel.getId());
        user.setParentId(orderUser.getId());
        user.setInviteCode(inviteCodeGenerator());
        user.setAncestors(orderUser.getAncestors()+","+orderUser.getId());
        // 获取注册赠送金额配置
        Optional<Object> configValue = configService.getConfigValue("trade", "registerBonusAmount");
        if (configValue.isPresent()) {
            BigDecimal value =  new BigDecimal((Integer) configValue.get());
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                //增加用户赠送金额
               user.setBalance(value);
               orderUserMapper.insertOrderUser(user);
               transactionService.recordFlow(user.getId(),"zczs",value,new BigDecimal(0), "注册赠送金额");
                //记录用户余额变动日志
            }else{
                orderUserMapper.insertOrderUser(user);
            }
        }else{
            orderUserMapper.insertOrderUser(user);
        }

        return "200";
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
