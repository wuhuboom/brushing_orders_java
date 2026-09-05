package com.order.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.order.member.domain.GoodsMemberLevel;
import com.order.member.domain.OrderUser;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Explicit public projection for the front-end member API.
 *
 * <p>Do not replace this type with {@link OrderUser}; that domain object also
 * contains password hashes, administration flags and audit data.</p>
 */
public record UserProfileResponse(
        Long id,
        String username,
        String phoneNumber,
        String avatar,
        String gender,
        String email,
        @JsonFormat(shape = JsonFormat.Shape.NUMBER) Date birthday,
        Long vipId,
        Long parentId,
        String parentUsername,
        String parentInviteCode,
        String inviteCode,
        BigDecimal balance,
        BigDecimal frozenBalance,
        BigDecimal baseSalary,
        Long taskProgress,
        Long reputationScore,
        BigDecimal todayCommission,
        BigDecimal luckyBonus,
        BigDecimal workLimit,
        String isEnabled,
        String allowInvite,
        String isFrozen,
        String isBanned,
        String isWithdrawalNotification,
        String productMatching,
        String accountStatus,
        String transactionStatus,
        String withdrawalStatus,
        String depositBlockWithdrawal,
        String assistWithdrawalStatus,
        MemberLevelResponse memberLevel) {

    public static UserProfileResponse from(OrderUser user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getAvatar(),
                user.getGender(),
                user.getEmail(),
                user.getBirthday(),
                user.getVipId(),
                user.getParentId(),
                user.getParentUsername(),
                user.getParentInviteCode(),
                user.getInviteCode(),
                user.getBalance(),
                user.getFrozenBalance(),
                user.getBaseSalary(),
                user.getTaskProgress(),
                user.getReputationScore(),
                user.getTodayCommission(),
                user.getLuckyBonus() == null ? BigDecimal.ZERO : user.getLuckyBonus(),
                user.getWorkLimit(),
                user.getIsEnabled(),
                user.getAllowInvite(),
                user.getIsFrozen(),
                user.getIsBanned(),
                user.getIsWithdrawalNotification(),
                user.getProductMatching(),
                user.getAccountStatus(),
                user.getTransactionStatus(),
                user.getWithdrawalStatus(),
                user.getDepositBlockWithdrawal(),
                user.getAssistWithdrawalStatus(),
                MemberLevelResponse.from(user.getMemberLevel()));
    }

    public record MemberLevelResponse(
            Long id,
            String name,
            Long level,
            String icon,
            BigDecimal price,
            BigDecimal minBalance,
            Long inviteCount,
            Long orderCountPerDay,
            BigDecimal minCommissionRate,
            BigDecimal maxCommissionRate,
            BigDecimal minContinuousCommissionRate,
            BigDecimal maxContinuousCommissionRate,
            Long taskCountPerDay,
            Long withdrawCountPerDay,
            BigDecimal withdrawFeeRate,
            BigDecimal minWithdrawAmount,
            BigDecimal withdrawLimitPerDay,
            BigDecimal minWithdraw,
            BigDecimal maxWithdraw,
            String description,
            String productMatchEnabled,
            BigDecimal productMatchMin,
            BigDecimal productMatchMax) {

        static MemberLevelResponse from(GoodsMemberLevel level) {
            if (level == null || level.getId() == null) {
                return null;
            }
            return new MemberLevelResponse(
                    level.getId(),
                    level.getName(),
                    level.getLevel(),
                    level.getIcon(),
                    level.getPrice(),
                    level.getMinBalance(),
                    level.getInviteCount(),
                    level.getOrderCountPerDay(),
                    level.getMinCommissionRate(),
                    level.getMaxCommissionRate(),
                    level.getMinContinuousCommissionRate(),
                    level.getMaxContinuousCommissionRate(),
                    level.getTaskCountPerDay(),
                    level.getWithdrawCountPerDay(),
                    level.getWithdrawFeeRate(),
                    level.getMinWithdrawAmount(),
                    level.getWithdrawLimitPerDay(),
                    level.getMinWithdraw(),
                    level.getMaxWithdraw(),
                    level.getDescription(),
                    level.getProductMatchEnabled(),
                    level.getProductMatchMin(),
                    level.getProductMatchMax());
        }
    }
}
