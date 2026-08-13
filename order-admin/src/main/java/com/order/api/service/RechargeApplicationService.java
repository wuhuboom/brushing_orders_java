package com.order.api.service;

import com.order.member.domain.GoodsRechargeRecord;
import com.order.member.domain.OrderUser;
import com.order.member.mapper.GoodsRechargeRecordMapper;
import com.order.member.mapper.OrderUserMapper;
import com.order.member.service.ITransactionService;
import com.order.member.service.SiteMessageNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.order.api.service.AccountErrorCodes.REVIEW_CONFLICT;

@Service
public class RechargeApplicationService {
    private final GoodsRechargeRecordMapper rechargeMapper;
    private final OrderUserMapper userMapper;
    private final ITransactionService transactionService;
    private final SiteMessageNotificationService siteMessageNotificationService;

    public RechargeApplicationService(
            GoodsRechargeRecordMapper rechargeMapper,
            OrderUserMapper userMapper,
            ITransactionService transactionService,
            SiteMessageNotificationService siteMessageNotificationService) {
        this.rechargeMapper = rechargeMapper;
        this.userMapper = userMapper;
        this.transactionService = transactionService;
        this.siteMessageNotificationService = siteMessageNotificationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long rechargeId, String targetStatus, String remark) {
        review(rechargeId, targetStatus, remark, "system");
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long rechargeId, String targetStatus, String remark, String updateBy) {
        if (!"2".equals(targetStatus) && !"3".equals(targetStatus)) {
            throw AccountApiException.badRequest(
                    REVIEW_CONFLICT,
                    "Status must be approved(2) or rejected(3)");
        }
        GoodsRechargeRecord recharge = rechargeMapper.selectForUpdate(rechargeId);
        if (recharge == null) {
            throw AccountApiException.notFound(REVIEW_CONFLICT, "Recharge record not found");
        }
        if (!"1".equals(recharge.getStatus())) {
            throw AccountApiException.conflict(
                    REVIEW_CONFLICT,
                    "Only pending recharge records can be reviewed");
        }

        if ("2".equals(targetStatus)) {
            creditRecharge(recharge);
        }
        if (rechargeMapper.transitionStatus(
                rechargeId,
                "1",
                targetStatus,
                trimToNull(remark),
                trimToNull(updateBy)) != 1) {
            throw AccountApiException.conflict(
                    REVIEW_CONFLICT,
                    "Recharge record was reviewed by another request");
        }
    }

    private void creditRecharge(GoodsRechargeRecord recharge) {
        OrderUser user = userMapper.selectWithdrawalUserByIdForUpdate(recharge.getUserId());
        if (user == null) {
            throw new IllegalStateException("Recharge user does not exist");
        }
        BigDecimal credited = positive(recharge.getReceivedAmount());
        BigDecimal amount = positive(recharge.getAmount());
        BigDecimal gift = nonNegative(recharge.getGiftAmount());
        if (credited == null) {
            credited = amount == null ? null : amount.add(gift);
        }
        if (credited == null || credited.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Recharge amount must be positive");
        }

        BigDecimal before = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        String reference = "recharge:" + recharge.getOrderNumber();
        if (amount != null && amount.add(gift).compareTo(credited) == 0) {
            transactionService.recordFlowWithTransactionCodeWithoutNotification(
                    user.getId(),
                    defaultType(recharge.getTransactionType()),
                    amount,
                    before,
                    recharge.getOrderNumber(),
                    reference);
            if (gift.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.recordFlowWithTransactionCodeWithoutNotification(
                        user.getId(),
                        "zs",
                        gift,
                        before.add(amount),
                        recharge.getOrderNumber(),
                        reference);
            }
        } else {
            transactionService.recordFlowWithTransactionCodeWithoutNotification(
                    user.getId(),
                    defaultType(recharge.getTransactionType()),
                    credited,
                    before,
                    recharge.getOrderNumber(),
                    reference);
        }
        if (userMapper.creditBalance(user.getId(), credited) != 1) {
            throw new IllegalStateException("Unable to credit recharge balance");
        }
        BigDecimal after = before.add(credited);
        BigDecimal depositAmount = amount == null ? credited : amount;
        siteMessageNotificationService.createForTransaction(
                user.getId(), "ck", depositAmount, before, after);
        if (gift.compareTo(BigDecimal.ZERO) > 0) {
            siteMessageNotificationService.createForTransaction(
                    user.getId(), "bonus", gift, before.add(depositAmount), after);
        }
    }

    private BigDecimal positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO
                : value;
    }

    private String defaultType(String value) {
        return value == null || value.isBlank() ? "ck" : value;
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
