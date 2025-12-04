package com.order.member.service;

import java.util.List;
import com.order.member.domain.OrderUser;

/**
 * 订单用户Service接口
 * 
 * @author order
 * @date 2025-10-21
 */
public interface IOrderUserService 
{
    /**
     * 查询订单用户
     * 
     * @param id 订单用户主键
     * @return 订单用户
     */
    public OrderUser selectOrderUserById(Long id);

    /**
     * 查询订单用户列表
     * 
     * @param orderUser 订单用户
     * @return 订单用户集合
     */
    public List<OrderUser> selectOrderUserList(OrderUser orderUser);

    /**
     * 新增订单用户
     * 
     * @param orderUser 订单用户
     * @return 结果
     */
    public int insertOrderUser(OrderUser orderUser);

    /**
     * 修改订单用户
     * 
     * @param orderUser 订单用户
     * @return 结果
     */
    public int updateOrderUser(OrderUser orderUser);

    /**
     * 批量删除订单用户
     * 
     * @param ids 需要删除的订单用户主键集合
     * @return 结果
     */
    public int deleteOrderUserByIds(Long[] ids);

    /**
     * 删除订单用户信息
     * 
     * @param id 订单用户主键
     * @return 结果
     */
    public int deleteOrderUserById(Long id);

    public OrderUser selectOrderUserByName(String username);

    public OrderUser selectOrderUserByInviteCode(String inviteCode);


    /**
     * 通过用户ID修改上级ID，并同步更新ancestors
     * @param paramMap 参数Map：userId（用户ID）、newParentId（新上级ID）、version（版本）
     * @return 影响行数
     */
    int updateParentIdAndAncestors(Long userId,
                                   Long newParentId,
                                   Long version);

    /**
     * 通过用户ID和scope查询下级用户（direct: 直属下级；all: 所有下级；默认: 直属下级）
     * @param userId 用户ID
     * @param scope 查询范围（direct/all）
     * @return 下级用户列表
     */
    List<OrderUser> selectChildrenById(Long userId,String scope);

    public String register(OrderUser user);

}
