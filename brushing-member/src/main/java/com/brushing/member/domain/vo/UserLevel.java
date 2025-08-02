package com.brushing.member.domain.vo;

import com.brushing.common.annotation.Excel;

public class UserLevel {

    /** 主键ID */
    private Long id;

    /** 会员图标 */
    @Excel(name = "会员图标")
    private String icon;

    /** 中文名称 */
    @Excel(name = "中文名称")
    private String nameZh;

    /** 英文名称 */
    @Excel(name = "英文名称")
    private String nameEn;

    private Integer orderCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getNameZh() {
        return nameZh;
    }

    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }
}
