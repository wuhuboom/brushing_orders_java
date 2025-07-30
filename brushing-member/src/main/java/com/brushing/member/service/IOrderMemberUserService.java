package com.brushing.member.service;

import java.util.List;
import com.brushing.member.domain.OrderMemberUser;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 会员用户Service接口
 * 
 * @author brushing
 * @date 2025-07-30
 */
public interface IOrderMemberUserService 
{
    /**
     * 查询会员用户
     * 
     * @param id 会员用户主键
     * @return 会员用户
     */
    public OrderMemberUser selectOrderMemberUserById(Long id);

    public OrderMemberUser findByUsername(String username);

    /**
     * 查询会员用户列表
     * 
     * @param orderMemberUser 会员用户
     * @return 会员用户集合
     */
    public List<OrderMemberUser> selectOrderMemberUserList(OrderMemberUser orderMemberUser);

    /**
     * 新增会员用户
     * 
     * @param orderMemberUser 会员用户
     * @return 结果
     */
    public int insertOrderMemberUser(OrderMemberUser orderMemberUser);

    /**
     * 修改会员用户
     * 
     * @param orderMemberUser 会员用户
     * @return 结果
     */
    public int updateOrderMemberUser(OrderMemberUser orderMemberUser);

    /**
     * 批量删除会员用户
     * 
     * @param ids 需要删除的会员用户主键集合
     * @return 结果
     */
    public int deleteOrderMemberUserByIds(Long[] ids);

    /**
     * 删除会员用户信息
     * 
     * @param id 会员用户主键
     * @return 结果
     */
    public int deleteOrderMemberUserById(Long id);
}
