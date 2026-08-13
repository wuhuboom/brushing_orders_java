package com.brushing.api.controller.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TopUpVo {

    /** 金额 */
    @NotNull(message = "amoutIsNull")
    @DecimalMin(value = "1.00", message = "amout1Min")
    private BigDecimal amout;
    /** 支付方式 */
    private String payMethod;
    /** 地址 */
    private String address;

    public BigDecimal getAmout() {
        return amout;
    }

    public void setAmout(BigDecimal amout) {
        this.amout = amout;
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
}
