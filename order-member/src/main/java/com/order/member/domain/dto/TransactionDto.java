package com.order.member.domain.dto;

import java.math.BigDecimal;

/**
 * 交易 DTO，用于接收前端传入的交易信息
 */
public class TransactionDto {

    /** 用户ID */
    private Long userId;

    /** 操作类型：0 加，1 减 */
    private Integer operationType;

    /** 交易类型（业务类型字符串，可选） */
    private String transactionType;

    /** 金额 */
    private BigDecimal amount;

    /** 赠送类型：0 比例，1 固定金额 */
    private Integer giftType;

    /** 赠送比例（当 giftType=0 时有效，例：0.1 表示 10%） */
    private BigDecimal giftRatio;

    /** 赠送金额（当 giftType=1 时有效） */
    private BigDecimal giftAmount;

    /** 备注 */
    private String remark;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getOperationType() {
        return operationType;
    }

    public void setOperationType(Integer operationType) {
        this.operationType = operationType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getGiftType() {
        return giftType;
    }

    public void setGiftType(Integer giftType) {
        this.giftType = giftType;
    }

    public BigDecimal getGiftRatio() {
        return giftRatio;
    }

    public void setGiftRatio(BigDecimal giftRatio) {
        this.giftRatio = giftRatio;
    }

    public BigDecimal getGiftAmount() {
        return giftAmount;
    }

    public void setGiftAmount(BigDecimal giftAmount) {
        this.giftAmount = giftAmount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
