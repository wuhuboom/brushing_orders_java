package com.brushing.member.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 充值记录对象 order_topup
 *
 * @author brushing
 * @date 2025-08-04
 */
public class OrderTopup extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;
    private Long agentUserId;
    private String isReal;

    /**
     * 金额
     */
    private BigDecimal amout;
    /**
     * 实际金额
     */
    private BigDecimal realMoney;

    private String username;

    /**
     * 手机号
     */
    private String phone;


    private String code;

    /**
     * 类型
     */
    private String type;
    /**
     * 充值方式
     */
    private String payMethod;
    /**
     * 地址
     */
    private String address;
    /**
     * 状态 0成功 1待审核 2拒绝
     */
    private String status;
    /**
     * 审核时间
     */
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER)
    private Date auditTime;
    /**
     * 审核人
     */
    private String auditor;

    private String agentUsername;

    /**
     * 备注
     */
    private String remark;

    public String getAgentUsername() {
        return agentUsername;
    }

    public void setAgentUsername(String agentUsername) {
        this.agentUsername = agentUsername;
    }

    public Long getAgentUserId() {
        return agentUserId;
    }

    public void setAgentUserId(Long agentUserId) {
        this.agentUserId = agentUserId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setAmout(BigDecimal amout) {
        this.amout = amout;
    }

    public BigDecimal getAmout() {
        return amout;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(java.util.Date auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditor() {
        return auditor;
    }

    public void setAuditor(String auditor) {
        this.auditor = auditor;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("userId", getUserId())
                .append("amout", getAmout())
                .append("type", getType())
                .append("payMethod", getPayMethod())
                .append("address", getAddress())
                .append("status", getStatus())
                .append("auditTime", getAuditTime())
                .append("auditor", getAuditor())
                .append("remark", getRemark())
                .append("createTime", getCreateTime())
                .toString();
    }

    public String getIsReal() {
        return isReal;
    }

    public void setIsReal(String isReal) {
        this.isReal = isReal;
    }

    public BigDecimal getRealMoney() {
        return realMoney;
    }

    public void setRealMoney(BigDecimal realMoney) {
        this.realMoney = realMoney;
    }
}
