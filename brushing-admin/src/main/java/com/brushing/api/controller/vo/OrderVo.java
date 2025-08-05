package com.brushing.api.controller.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Date;


public class OrderVo {

    private Long id;

    private String orderNo;

    private String goodsName;

    private String coverUrl;

    private BigDecimal price;

    private BigDecimal commission;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;



    public OrderVo(Long id, String orderNo, String goodsName, String coverUrl, BigDecimal price, BigDecimal commission,Date createTime) {
        this.id = id;
        this.orderNo = orderNo;
        this.goodsName = goodsName;
        this.coverUrl = coverUrl;
        this.price = price;
        this.commission = commission;
        this.createTime=createTime;
    }


    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
