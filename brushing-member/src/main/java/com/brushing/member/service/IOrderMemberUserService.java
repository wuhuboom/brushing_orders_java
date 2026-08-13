package com.brushing.member.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.brushing.member.domain.DashboardData;
import com.brushing.member.domain.OrderMemberUser;
import com.brushing.member.domain.vo.MemberHierarchyStatVo;
import com.brushing.member.domain.vo.MemberLevelAmountVo;
import com.brushing.member.domain.vo.RebateStatVo;
import com.brushing.member.domain.vo.TopLevelUserStatVo;
import org.apache.ibatis.annotations.Param;
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

    public String register(OrderMemberUser user);

    public Boolean  existsPhone(String phone);
    /**
     * 查询会员用户列表
     * 
     * @param orderMemberUser 会员用户
     * @return 会员用户集合
     */
    public List<OrderMemberUser> selectOrderMemberUserList(OrderMemberUser orderMemberUser);

    /**
     * Globally sort filtered members by a transaction/hierarchy aggregate,
     * page the sorted IDs, and return the normal member-list row contract.
     */
    List<OrderMemberUser> selectOrderMemberUserListByAggregateSort(
            OrderMemberUser orderMemberUser, String sortKey, String sortDirection);

    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope);

    // 支持按下级用户名模糊匹配过滤的重载
    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope, String subUsername);
    // 支持同时按下级用户名与下级电话号码过滤（电话号码支持4位尾号匹配）
    public List<OrderMemberUser> selectMembersByScope(Long userId, String scope, String subUsername, String subPhone);

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
     * Change a member's parent and rebuild the materialized ancestor path for
     * that member and every descendant.
     *
     * @param memberId member being moved
     * @param parentIdentifier new parent ID/invite code, or {@code "0"} for root
     * @param version expected member version
     * @return affected root-member row count
     */
    int changeParent(Long memberId, String parentIdentifier, Long version);

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

    public DashboardData getDashboardData();

    public void updateUserLevel(Long userId, BigDecimal amount);

    public Boolean checkUserBalance(OrderMemberUser orderMemberUser);

    List<MemberHierarchyStatVo> getHierarchyStats(String username, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 只通过用户名查询（会自动计算今日/昨日区间）
     */
    RebateStatVo getRebateStats(String username);

    /**
     * 查询指定用户名的前3层下级用户，返回每个用户的用户名、层级(1/2/3)和充值/提现总额
     */
    List<MemberLevelAmountVo> getFirstThreeLevelsByUsername(String username);

    /**
     * 查询顶级用户统计（parent_id = 0），支持按用户名和时间范围过滤
     */
    List<TopLevelUserStatVo> getTopLevelStats(String username, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定用户的下级用户的统计信息
     */
    List<TopLevelUserStatVo> getSubStatsByUserId(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
