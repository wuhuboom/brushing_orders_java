package com.brushing.member.mapper;

import java.util.List;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.vo.MemberHierarchyStatVo;
import com.brushing.member.domain.vo.RebateStatVo;
import java.time.LocalDateTime;

import com.brushing.member.domain.vo.TopLevelUserStatVo;
import com.brushing.member.domain.vo.MemberLevelAmountVo;
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

    /** Return one globally aggregate-sorted Page of member IDs. */
    List<Long> selectOrderMemberUserAggregateSortedIds(OrderMemberUser orderMemberUser);

    /** Load the lightweight list projection for one bounded ID batch. */
    List<OrderMemberUser> selectOrderMemberUserListByIds(@Param("userIds") List<Long> userIds);

    /** Aggregate transaction-derived member-list fields for one bounded ID batch. */
    List<OrderMemberUser> selectMemberListFinancialAggregates(@Param("userIds") List<Long> userIds);

    /** Aggregate direct and all-descendant counts for one bounded ID batch. */
    List<OrderMemberUser> selectMemberListSubordinateAggregates(@Param("userIds") List<Long> userIds);

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

    /** Lightweight hierarchy reads used by parent changes. */
    OrderMemberUser selectMemberHierarchyById(@Param("id") Long id);

    OrderMemberUser selectMemberHierarchyByInviteCode(@Param("inviteCode") String inviteCode);

    List<OrderMemberUser> selectMemberHierarchiesForUpdate(@Param("ids") List<Long> ids);

    int changeMemberParent(@Param("memberId") Long memberId,
                           @Param("parentId") Long parentId,
                           @Param("ancestors") String ancestors,
                           @Param("version") Long version);

    /**
     * Lock direct children of one bounded parent frontier. Callers repeat this
     * query level by level to lock a complete branch without relying on locks
     * taken against a materialized recursive CTE.
     */
    List<OrderMemberUser> selectMemberChildrenForUpdate(@Param("parentIds") List<Long> parentIds);

    int updateMemberAncestorsBatch(@Param("members") List<OrderMemberUser> members);


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

    /**
     * 查选对应用户的直属下级或者所有下级
     * @param userId
     * @param scope 'direct' 或 'all'
     * @return
     */
    List<OrderMemberUser> selectMembersByScope(@Param("userId") Long userId,
                                         @Param("scope") String scope,
                                         @Param("subUsername") String subUsername,
                                         @Param("subPhone") String subPhone);


   public int updateUserLevel(@Param("id") Long id, @Param("levelId") Long levelId);

    /**
     * 查询层级统计
     * @param username 用户名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return
     */
    List<MemberHierarchyStatVo> selectHierarchyStats(@Param("username") String username,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 通过用户名查询该用户及其所有下级的返利与交易统计（只需要 username，SQL 在 mapper XML 中自动计算今日/昨日区间）
     * @param username 用户名
     * @return RebateStatVo 包含 inviteCode、今日/昨日/累计返利、下级人数、交易人数等
     */
    RebateStatVo selectRebateStats(@Param("username") String username);

    /**
     * 查询所有顶级用户（parent_id = 0）及其下线统计信息
     * @param username 可选，按用户名过滤
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return
     */
    List<TopLevelUserStatVo> selectTopLevelStats(@Param("username") String username,
                                                 @Param("startTime") java.time.LocalDateTime startTime,
                                                 @Param("endTime") java.time.LocalDateTime endTime);

    /**
     * 查询指定用户的下级用户的统计信息
     *
     * @param userId 用户的ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 查询结果
     */
    List<TopLevelUserStatVo> selectSubStatsByUserId(@Param("userId") Long userId,
                                                 @Param("startTime") java.time.LocalDateTime startTime,
                                                 @Param("endTime") java.time.LocalDateTime endTime);

    // 新增：查询指定用户名的前3层下级（每个用户返回层级1/2/3，及充值/提现总额）
    List<MemberLevelAmountVo> selectFirstThreeLevelsByUsername(@Param("username") String username);

}
