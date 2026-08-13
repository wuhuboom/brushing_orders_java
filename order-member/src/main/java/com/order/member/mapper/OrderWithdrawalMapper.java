package com.order.member.mapper;

import java.util.List;
import com.order.member.domain.OrderWithdrawal;
import com.order.member.domain.WithdrawalDailyUsage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import org.apache.ibatis.annotations.Param;

/**
 * 提现Mapper接口
 * 
 * @author order
 * @date 2025-11-11
 */
public interface OrderWithdrawalMapper 
{
    /**
     * 查询提现
     * 
     * @param id 提现主键
     * @return 提现
     */
    public OrderWithdrawal selectOrderWithdrawalById(Long id);

    /**
     * 查询提现列表
     * 
     * @param orderWithdrawal 提现
     * @return 提现集合
     */
    public List<OrderWithdrawal> selectOrderWithdrawalList(OrderWithdrawal orderWithdrawal);
    public List<OrderWithdrawal> selectOrderWithdrawalByUserId(Long userId);

    /**
     * 新增提现
     * 
     * @param orderWithdrawal 提现
     * @return 结果
     */
    public int insertOrderWithdrawal(OrderWithdrawal orderWithdrawal);

    /**
     * 修改提现
     * 
     * @param orderWithdrawal 提现
     * @return 结果
     */
    public int updateOrderWithdrawal(OrderWithdrawal orderWithdrawal);

    /**
     * 删除提现
     * 
     * @param id 提现主键
     * @return 结果
     */
    public int deleteOrderWithdrawalById(Long id);

    /**
     * 批量删除提现
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderWithdrawalByIds(Long[] ids);

    OrderWithdrawal selectByUserAndRequestId(@Param("userId") Long userId,
                                             @Param("requestId") String requestId);

    OrderWithdrawal selectForUpdate(Long id);

    int existsPendingByUserId(Long userId);

    int existsPendingByAccountId(Long accountId);

    WithdrawalDailyUsage selectDailyUsage(@Param("userId") Long userId,
                                          @Param("start") Date start,
                                          @Param("end") Date end);

    int transitionStatus(@Param("id") Long id,
                         @Param("fromStatus") String fromStatus,
                         @Param("toStatus") String toStatus,
                         @Param("remarks") String remarks,
                         @Param("updateBy") String updateBy);

    int updateAccountSnapshot(@Param("id") Long id,
                              @Param("accountSnapshotEncrypted") String accountSnapshotEncrypted,
                              @Param("accountMask") String accountMask,
                              @Param("updateBy") String updateBy);

    int ensureDailyQuota(@Param("businessDate") LocalDate businessDate,
                         @Param("legacyStart") Date legacyStart,
                         @Param("legacyEnd") Date legacyEnd);

    int reserveDailyQuota(@Param("businessDate") LocalDate businessDate,
                          @Param("amount") BigDecimal amount,
                          @Param("limit") BigDecimal limit);

    int releaseDailyQuota(@Param("businessDate") LocalDate businessDate,
                          @Param("amount") BigDecimal amount);

    List<OrderWithdrawal> selectPublicByUser(@Param("userId") Long userId,
                                             @Param("status") String status);
}
