package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderMemberUser;
import org.apache.ibatis.annotations.Param;

/**
 * 会员用户Mapper接口
 * 
 * @author brushing
 * @date 2025-07-30
 */
public interface OrderMemberUserMapper 
{
    /**
     * 查询会员用户
     * 
     * @param id 会员用户主键
     * @return 会员用户
     */
    public OrderMemberUser selectOrderMemberUserById(Long id);


    /**
     * 通过
     * @param id
     * @return
     */
    public OrderMemberUser selectOrderMemberUserByInCode(String inviteCode);

    /**
     * 通过用户名查询
     * @param inviteCode
     * @return
     */
    public OrderMemberUser findByUsername(String inviteCode);

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
     * 删除会员用户
     * 
     * @param id 会员用户主键
     * @return 结果
     */
    public int deleteOrderMemberUserById(Long id);

    /**
     * 批量删除会员用户
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderMemberUserByIds(Long[] ids);

    /**
     * 查询直属下级
     * @param userId  对应用户id
     * @param isDirect true查询直属下级 false 查询所有
     * @return
     */
    List<OrderMemberUser> selectSubUsers(@Param("userId") Long userId,
                                         @Param("isDirect") boolean isDirect);



}
