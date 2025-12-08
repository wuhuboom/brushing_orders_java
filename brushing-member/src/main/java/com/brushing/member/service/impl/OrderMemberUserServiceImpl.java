package com.brushing.member.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.brushing.common.utils.InviteCodeGenerator;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.*;
import com.brushing.member.mapper.*;
import com.brushing.member.service.IOrderShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.service.IOrderMemberUserService;

/**
 * 会员用户Service业务层处理
 * 
 * @author brushing
 * @date 2025-07-30
 */
@Service
public class OrderMemberUserServiceImpl implements IOrderMemberUserService 
{
    @Autowired
    private OrderMemberUserMapper orderMemberUserMapper;

    @Autowired
    private OrderMemberLevelMapper levelMapper;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private WelfareConfigMapper welfareConfigMapper;

    @Autowired
    private OrderShopMapper shopMapper;

    /**
     * 查询会员用户
     * 
     * @param id 会员用户主键
     * @return 会员用户
     */
    @Override
    public OrderMemberUser selectOrderMemberUserById(Long id)
    {
        return orderMemberUserMapper.selectOrderMemberUserById(id);
    }

    @Override
    public OrderMemberUser findByUsername(String username) {
        return orderMemberUserMapper.findByUsername(username);
    }

    @Override
    public String register(OrderMemberUser user) {
        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserByInCode(user.getInviteCode());
        if (StringUtils.isNull(orderMemberUser)){
            return "500";
        }

        OrderMemberLevel orderMemberLevel = levelMapper.selectLowestPriceLevel();
        user.setLevelId(orderMemberLevel.getId());
        user.setParentId(orderMemberUser.getId());
        WelfareConfig welfareConfig = welfareConfigMapper.selectWelfareConfigByType("1");
        if (StringUtils.isNotNull(welfareConfig)&&welfareConfig.getStatus().equals("0")){
            user.setBalance(welfareConfig.getAmount());
        }
        user.setInviteCode(setCode());
        user.setAncestors(orderMemberUser.getAncestors()+","+orderMemberUser.getId());
        orderMemberUserMapper.insertOrderMemberUser(user);
        return "200";
    }

    @Override
    public Boolean existsPhone(String phone) {
        return orderMemberUserMapper.existsPhone(phone);
    }


    /**
     * 查询会员用户列表
     * 
     * @param orderMemberUser 会员用户
     * @return 会员用户
     */
    @Override
    public List<OrderMemberUser> selectOrderMemberUserList(OrderMemberUser orderMemberUser)
    {
        return orderMemberUserMapper.selectOrderMemberUserList(orderMemberUser);
    }

    @Override
    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope) {
        return orderMemberUserMapper.selectMembersByScope(userId,scope);
    }

    /**
     * 新增会员用户
     * 
     * @param orderMemberUser 会员用户
     * @return 结果
     */
    @Override
    public int insertOrderMemberUser(OrderMemberUser orderMemberUser)
    {
        orderMemberUser.setCreateTime(DateUtils.getNowDate());
        orderMemberUser.setInviteCode(setCode());
        orderMemberUser.setAncestors(setAncestors(orderMemberUser));
       if (StringUtils.isNull(orderMemberUser.getLevelId())){
           OrderMemberLevel orderMemberLevel = levelMapper.selectLowestPriceLevel();
           orderMemberUser.setLevelId(orderMemberLevel.getId());
       }
        return orderMemberUserMapper.insertOrderMemberUser(orderMemberUser);
    }

    public String setCode(){
        String code = InviteCodeGenerator.generateInviteCode();
        OrderMemberUser orderMemberUser = orderMemberUserMapper.selectOrderMemberUserByInCode(code);
        if (StringUtils.isNotNull(orderMemberUser)){
            setCode();
        }
        return code;
    }

    //设置祖级
    public String setAncestors(OrderMemberUser orderMemberUser){
        Long parentId = orderMemberUser.getParentId();
        if (StringUtils.isNull(parentId)){
            return "0";
        }
        if (parentId.equals("0")){
            return "0";
        }
        OrderMemberUser orderMemberUser1 = orderMemberUserMapper.selectOrderMemberUserById(parentId);
        if (StringUtils.isNull(orderMemberUser1)){
            return "0";
        }
        return orderMemberUser1.getAncestors()+ "," +orderMemberUser1.getId() ;
    }

    /**
     * 修改会员用户
     * 
     * @param orderMemberUser 会员用户
     * @return 结果
     */
    @Override
    public int updateOrderMemberUser(OrderMemberUser orderMemberUser)
    {

        if(StringUtils.isNotNull(orderMemberUser.getParentId())){
            OrderMemberUser oldUser = orderMemberUserMapper.selectOrderMemberUserById(orderMemberUser.getId());
            String oldAncestors = oldUser.getAncestors(); // 旧祖籍，比如 "0,1,2"
            Long newParentId = orderMemberUser.getParentId();    // 新parentId，比如 0 或 10
            String newAncestors;
            if (newParentId == 0L) {
                // 新的上级是0，说明是顶级用户，祖籍就直接是 "0"
                newAncestors = "0";
            } else {
                // 去掉旧祖籍最后一部分，加上新的parentId
                int lastCommaIndex = oldAncestors.lastIndexOf(",");
                if (lastCommaIndex != -1) {
                    newAncestors = oldAncestors.substring(0, lastCommaIndex + 1) + newParentId;
                } else {
                    newAncestors = String.valueOf(newParentId);
                }
            }
            orderMemberUser.setAncestors(newAncestors);
            // 调用更新子孙节点祖籍的方法
            orderMemberUserMapper.updateChildrenAncestors(oldAncestors, newAncestors);

        }


        return orderMemberUserMapper.updateOrderMemberUser(orderMemberUser);
    }

    /**
     * 批量删除会员用户
     * 
     * @param ids 需要删除的会员用户主键
     * @return 结果
     */
    @Override
    public int deleteOrderMemberUserByIds(Long[] ids)
    {
        return orderMemberUserMapper.deleteOrderMemberUserByIds(ids);
    }

    /**
     * 删除会员用户信息
     * 
     * @param id 会员用户主键
     * @return 结果
     */
    @Override
    public int deleteOrderMemberUserById(Long id)
    {
        return orderMemberUserMapper.deleteOrderMemberUserById(id);
    }

    @Override
    public DashboardData getDashboardData() {
        return dashboardMapper.getDashboardData();
    }

    @Override
    public void updateUserLevel(Long userId, BigDecimal amount) {
        OrderMemberUser user = orderMemberUserMapper.selectOrderMemberUserById(userId);
        try {
                // 获取用户当前的余额对应的等级
                OrderMemberLevel levelByBalance = levelMapper.findLevelByBalance(amount);
                if (levelByBalance == null) {
                    return;
                }
                // 比较查询到的等级与用户当前等级
                Long currentLevelId = user.getLevelId();
                Long newLevelId = levelByBalance.getId();
                if (currentLevelId != null && currentLevelId.equals(newLevelId)) {
                    return;
                }
                user.setLevelId(newLevelId);
                // 更新用户等级
                int i = orderMemberUserMapper.updateOrderMemberUser(user);
        } catch (Exception e) {
                // 记录错误，继续处理下一个用户
        }
    }

    @Override
    public Boolean checkUserBalance(OrderMemberUser user) {
        Integer dealCount = user.getDealCount() + 1;
        //当前等级
        int level = levelMapper.selectLevelById(user.getLevelId());
        //查询当前用户的
        OrderShop orderShop = shopMapper.selectOrderShopByAutoVip(dealCount);
        if (StringUtils.isNull(orderShop)){
            return false;
        }
        //对应的等级
        Integer vipLevel = orderShop.getVipLevel();
        OrderMemberLevel orderMemberLevel = levelMapper.selectLevelByRank(vipLevel);
        if (StringUtils.isNull(orderMemberLevel)){
            return false;
        }
        if (level == vipLevel || level>vipLevel){
            return true;
        }
        BigDecimal balance = user.getBalance();
        BigDecimal price = orderMemberLevel.getPrice();
        return balance.compareTo(price) >= 0;
    }
}
