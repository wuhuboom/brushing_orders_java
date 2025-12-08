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

    public OrderMemberUser selectOrderMemberUser(Long id);


    /**
     * 通过
     * @param
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
     * 查询电话号码是否存在
     * @param phone 电话号码
     * @return
     */
    public Boolean  existsPhone(String phone);

    /**
     * 查询会员用户列表
     * 
     * @param orderMemberUser 会员用户
     * @return 会员用户集合
     */
    public List<OrderMemberUser> selectOrderMemberUserList(OrderMemberUser orderMemberUser);

    public List<OrderMemberUser> selectAllUser();

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


    public int updateUserAddress(OrderMemberUser orderMemberUser);

    /**
     * 重置今日提现次数 和 重置次数
     * @return
     */
    public int resetTodayCounts();

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

    int updateChildrenAncestors(@Param("oldAncestors") String oldAncestors,
                                @Param("newAncestors") String newAncestors);

    /**
     * 查选对应用户的直属下级或者所有下级
     * @param userId
     * @param scope 'direct' 或 'all'
     * @return
     */
    List<OrderMemberUser> selectMembersByScope(@Param("userId") Long userId,
                                         @Param("scope") String scope);


   public int updateUserLevel(@Param("id") Long id, @Param("levelId") Long levelId);



}
