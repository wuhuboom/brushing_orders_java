package com.order.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public final class ConfigApiDtos {
    private ConfigApiDtos() {
    }

    public record LanguageOption(String code, String tag, String label, boolean h5Enabled) {
    }

    public record GlobalContentResponse(
            String lang,
            String protocolContent,
            String aboutContent,
            String certificateContent,
            String helpContent,
            String termsContent,
            String eventContent,
            String transactionDescription,
            String orderDescription,
            String usageDescription) {
    }

    public record WebsiteConfigResponse(
            String name,
            String currencyUnit,
            String copyright,
            String logo,
            int popUpLimit,
            String popUpImage,
            String backgroundImage,
            String h5BackgroundImage,
            String showLogo,
            String hideImage,
            List<String> imageShowTimeRange) {
    }

    public record CustomerServiceResponse(
            String id,
            String name,
            Long sortOrder,
            String image,
            String link) {
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
    }

    public record NoticeSummaryResponse(
            Long noticeId,
            String noticeTitle,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTime) {
    }

    public record NoticeDetailResponse(
            Long noticeId,
            String noticeTitle,
            String noticeContent,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createTime) {
    }

    public record TimeZoneResponse(String tzName) {
    }

    public record TradeConfigResponse(
            Object registerBonusAmount,
            Object minTradeBalance,
            Object memberWithdrawalStatus,
            Object minCreditScoreForWithdrawal,
            Object minWithdrawalAmount,
            Object maxWithdrawalAmount,
            Object withdrawalFeeRate,
            Object serviceTimeRange,
            Object tradeTimeRange,
            Object withdrawalTimeRange,
            Object orderExpireSeconds,
            Object startTaskDelayMs,
            Object submitTaskDelayMs,
            Object requiredTaskGroupsForWithdrawal,
            Object allowModifyWithdrawalAddress) {
    }

    public record PagedResult<T>(List<T> rows, long total) {
    }

    public record AvatarUploadResponse(String fileName, String url, String avatar) {
    }
}
