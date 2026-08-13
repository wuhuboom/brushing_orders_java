package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderUser;
import org.apache.ibatis.annotations.Param;

/**
 * 订单用户Mapper接口
 * 
 * @author order
 * @date 2025-10-21
 */
public interface OrderUserMapper 
{
    /**
     * 查询订单用户
     * 
     * @param id 订单用户主键
     * @return 订单用户
     */
    public OrderUser selectOrderUserById(Long id);

    public OrderUser selectOrderUserByName(String username);

    public OrderUser selectOrderUserByInviteCode(String inviteCode);

    /**
     * Front-end authentication lookup. This query must stay small and must not
     * include the reporting CTEs used by the administration screens.
     */
    OrderUser selectAuthUserByName(String username);

    OrderUser selectAuthUserById(Long id);

    /**
     * Public profile projection used by /api/user/login and /api/user/getInfo.
     */
    OrderUser selectUserProfileById(Long id);

    OrderUser selectWithdrawalUserById(Long id);

    OrderUser selectWithdrawalUserByIdForUpdate(Long id);

    int debitBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    int creditBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    Long lockUserById(Long userId);

    OrderUser selectOrderTaskUserById(Long id);

    OrderUser selectOrderBalanceById(Long id);

    int reserveOrderFunds(
            @Param("userId") Long userId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("progressDelta") long progressDelta,
            @Param("allowNegative") boolean allowNegative);

    int settleOrderFunds(
            @Param("userId") Long userId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("rebate") java.math.BigDecimal rebate);

    int settleLinkedOrderGroup(
            @Param("userId") Long userId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("rebate") java.math.BigDecimal rebate);

    int releaseCancelledOrderFunds(
            @Param("userId") Long userId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("progressDelta") long progressDelta);

    /**
     * Minimal referral lookup used during registration.
     */
    OrderUser selectReferralByInviteCode(String inviteCode);

    Boolean existsInviteCode(String inviteCode);

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

    int updateAvatarById(@Param("userId") Long userId,
                         @Param("avatar") String avatar);

    int updatePasswordById(@Param("userId") Long userId,
                           @Param("currentPassword") String currentPassword,
                           @Param("newPassword") String newPassword);

    int updateTradePasswordById(@Param("userId") Long userId,
                                @Param("currentPassword") String currentPassword,
                                @Param("newPassword") String newPassword);

    int resetTradePasswordByAdmin(@Param("userId") Long userId,
                                  @Param("tradePassword") String tradePassword);

    int updateWithdrawalPasswordFailCount(@Param("userId") Long userId,
                                          @Param("failCount") Integer failCount);

    int incrementWithdrawalPasswordFailCount(@Param("userId") Long userId);

    OrderUser selectTradePasswordStateByIdForUpdate(@Param("userId") Long userId);

    public int resetTodayRestBatch();

    /**
     * 删除订单用户
     * 
     * @param id 订单用户主键
     * @return 结果
     */
    public int deleteOrderUserById(Long id);

    /**
     * 批量删除订单用户
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderUserByIds(Long[] ids);


    /**
     * 通过用户ID修改上级ID，并同步更新ancestors
     * @param paramMap 参数Map：userId（用户ID）、newParentId（新上级ID）、version（版本）
     * @return 影响行数
     */
    int updateParentIdAndAncestors(@Param("userId") Long userId,
                                   @Param("newParentId") Long newParentId,
                                   @Param("version") Long version);

    /**
     * 通过用户ID和scope查询下级用户（direct: 直属下级；all: 所有下级；默认: 直属下级）
     * @param userId 用户ID
     * @param scope 查询范围（direct/all）
     * @return 下级用户列表
     */
    List<OrderUser> selectChildrenById(@Param("userId") Long userId, @Param("scope") String scope);

    /**
     * 查询电话号码是否存在
     * @param phoneNumber 电话号码
     * @return
     */
    public Boolean  existsPhone(String phoneNumber);
}
