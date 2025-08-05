package com.brushing.member.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 账户变动对象 order_account_change
 * 
 * @author brushing
 * @date 2025-08-02
 */
public class OrderAccountChange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private String id;

    /** 变动编号 */
    @Excel(name = "变动编号")
    private String changeNo;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 变动类型 */
    @Excel(name = "变动类型")
    private String type;

    /** 变动前余额 */
    @Excel(name = "变动前余额")
    private BigDecimal beforeAmount;

    /** 变动金额 */
    @Excel(name = "变动金额")
    private BigDecimal changeAmount;

    /** 变动后余额 */
    @Excel(name = "变动后余额")
    private BigDecimal afterAmount;

    /** 变动描述 */
    @Excel(name = "变动描述")
    private String description;

    public void setId(String id) 
    {
        this.id = id;
    }

    public String getId() 
    {
        return id;
    }

    public void setChangeNo(String changeNo) 
    {
        this.changeNo = changeNo;
    }

    public String getChangeNo() 
    {
        return changeNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setBeforeAmount(BigDecimal beforeAmount) 
    {
        this.beforeAmount = beforeAmount;
    }

    public BigDecimal getBeforeAmount() 
    {
        return beforeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) 
    {
        this.changeAmount = changeAmount;
    }

    public BigDecimal getChangeAmount() 
    {
        return changeAmount;
    }

    public void setAfterAmount(BigDecimal afterAmount) 
    {
        this.afterAmount = afterAmount;
    }

    public BigDecimal getAfterAmount() 
    {
        return afterAmount;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("changeNo", getChangeNo())
            .append("userId", getUserId())
            .append("type", getType())
            .append("beforeAmount", getBeforeAmount())
            .append("changeAmount", getChangeAmount())
            .append("afterAmount", getAfterAmount())
            .append("description", getDescription())
            .append("createTime", getCreateTime())
            .toString();
    }
}
