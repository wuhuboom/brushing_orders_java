package com.order.api.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * Public DTOs for /api/account. Domain objects are intentionally not exposed.
 */
public final class AccountApiDtos {
    private AccountApiDtos() {
    }

    public record WithdrawalTypeResponse(Long id, String typeName, String type) {
    }

    public record WithdrawalAccountRequest(
            @NotBlank @Size(max = 20) String withdrawalTypeId,
            Boolean isDefault,
            @Size(max = 100) String bankName,
            @Size(max = 50) String depositType,
            @Size(max = 50) String branchCode,
            @Size(max = 100) String branchName,
            @Size(max = 100) String bankAccount,
            @Size(max = 100) String accountHolder,
            @Size(max = 100) String accountName,
            @Size(max = 100) String walletName,
            @Size(max = 255) String walletAddress) {
    }

    public record WithdrawalAccountResponse(
            Long id,
            String type,
            String withdrawalTypeId,
            String withdrawalTypeName,
            boolean isDefault,
            String bankName,
            String depositType,
            String branchCode,
            String branchName,
            String bankAccount,
            String accountHolder,
            String accountName,
            String walletName,
            String walletAddress,
            Date createTime,
            Date updateTime) {
    }

    public record WithdrawalRequest(
            @NotNull UUID requestId,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 18, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 128) String tradePassword,
            @NotNull Long withdrawalAccountId) {
    }

    public record WithdrawalResponse(
            Long withdrawalId,
            String orderNumber,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal netAmount,
            String status,
            Date createdAt) {
    }

    public record WithdrawalHistoryResponse(
            Long id,
            String orderNumber,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal netAmount,
            String status,
            String remarks,
            String account,
            Date createTime) {
    }

    public record DepositResponse(
            Long id,
            BigDecimal amount,
            BigDecimal giftAmount,
            BigDecimal receivedAmount,
            String status,
            String transactionType,
            String orderNumber,
            String remark,
            Date createTime) {
    }

    public record TransactionResponse(
            Long id,
            String serialCode,
            String transactionType,
            BigDecimal transactionAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String transactionCode,
            String remark,
            Date createdTime) {
    }

    public record ReviewStatusRequest(
            @NotBlank String status,
            @Size(max = 255) String remarks) {
    }

    /**
     * Full payout details are exposed only by the separately permissioned admin endpoint.
     */
    public record SensitiveWithdrawalAccountResponse(
            Long withdrawalId,
            String type,
            String withdrawalType,
            String bankName,
            String depositType,
            String branchCode,
            String branchName,
            String bankAccount,
            String accountHolder,
            String accountName,
            String walletName,
            String walletAddress) {
    }
}
