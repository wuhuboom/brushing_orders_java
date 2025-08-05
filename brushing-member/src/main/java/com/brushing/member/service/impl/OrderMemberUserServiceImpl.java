package com.brushing.member.service.impl;

import java.util.List;

import com.brushing.common.utils.InviteCodeGenerator;
import com.brushing.common.utils.DateUtils;
import com.brushing.common.utils.StringUtils;
import com.brushing.member.domain.OrderMemberLevel;
import com.brushing.member.mapper.OrderMemberLevelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.brushing.member.mapper.OrderMemberUserMapper;
import com.brushing.member.domain.OrderMemberUser;
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
        user.setInviteCode(setCode());
        user.setAncestors(orderMemberUser.getAncestors()+","+orderMemberUser.getId());
        orderMemberUserMapper.insertOrderMemberUser(user);
        return "200";
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
        OrderMemberLevel orderMemberLevel = levelMapper.selectLowestPriceLevel();
        orderMemberUser.setLevelId(orderMemberLevel.getId());
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
}
