package com.brushing.set.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 网站设置对象 order_site_config
 * 
 * @author brushing
 * @date 2025-07-31
 */
public class OrderSiteConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 网站名称 */
    @Excel(name = "网站名称")
    private String siteName;

    /** 客服链接 */
    @Excel(name = "客服链接")
    private String customerServiceUrl;


    private String emailAddress;

    /** 弹框内容 */
    @Excel(name = "弹框内容")
    private String popupMessage;

    /** 版权声明 */
    @Excel(name = "版权声明")
    private String copyrightInfo;

    /** 注册赠送金额 */
    @Excel(name = "注册赠送金额")
    private BigDecimal registerBonusAmount;

    /** 维护图片URL */
    @Excel(name = "维护图片URL")
    private String maintenanceImage;

    /** 网站LOGO URL */
    @Excel(name = "网站LOGO URL")
    private String siteLogo;

    /** 浏览器图标URL */
    @Excel(name = "浏览器图标URL")
    private String favicon;

    /** 货币类型，如CNY/USD/JPY */
    @Excel(name = "货币类型，如CNY/USD/JPY")
    private String currencyType;

    /** 同IP可注册人数限制 */
    @Excel(name = "同IP可注册人数限制")
    private Long maxRegisterPerIp;

    /** IP黑名单（逗号分隔） */
    @Excel(name = "IP黑名单", readConverterExp = "逗=号分隔")
    private String ipBlacklist;

    /** 是否开启注册 */
    @Excel(name = "是否开启注册")
    private String registerEnabled;

    /** 是否开启邮箱验证 */
    @Excel(name = "是否开启邮箱验证")
    private String emailVerificationEnabled;

    private String  totpEnabled;

    private String  levelStatus;
    private String  seriesStatus;

    public String getLevelStatus() {
        return levelStatus;
    }

    public void setLevelStatus(String levelStatus) {
        this.levelStatus = levelStatus;
    }

    public String getSeriesStatus() {
        return seriesStatus;
    }

    public void setSeriesStatus(String seriesStatus) {
        this.seriesStatus = seriesStatus;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(String totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSiteName(String siteName) 
    {
        this.siteName = siteName;
    }

    public String getSiteName() 
    {
        return siteName;
    }

    public void setCustomerServiceUrl(String customerServiceUrl) 
    {
        this.customerServiceUrl = customerServiceUrl;
    }

    public String getCustomerServiceUrl() 
    {
        return customerServiceUrl;
    }

    public void setPopupMessage(String popupMessage) 
    {
        this.popupMessage = popupMessage;
    }

    public String getPopupMessage() 
    {
        return popupMessage;
    }

    public void setCopyrightInfo(String copyrightInfo) 
    {
        this.copyrightInfo = copyrightInfo;
    }

    public String getCopyrightInfo() 
    {
        return copyrightInfo;
    }

    public void setRegisterBonusAmount(BigDecimal registerBonusAmount) 
    {
        this.registerBonusAmount = registerBonusAmount;
    }

    public BigDecimal getRegisterBonusAmount() 
    {
        return registerBonusAmount;
    }

    public void setMaintenanceImage(String maintenanceImage) 
    {
        this.maintenanceImage = maintenanceImage;
    }

    public String getMaintenanceImage() 
    {
        return maintenanceImage;
    }

    public void setSiteLogo(String siteLogo) 
    {
        this.siteLogo = siteLogo;
    }

    public String getSiteLogo() 
    {
        return siteLogo;
    }

    public void setFavicon(String favicon) 
    {
        this.favicon = favicon;
    }

    public String getFavicon() 
    {
        return favicon;
    }

    public void setCurrencyType(String currencyType) 
    {
        this.currencyType = currencyType;
    }

    public String getCurrencyType() 
    {
        return currencyType;
    }

    public void setMaxRegisterPerIp(Long maxRegisterPerIp) 
    {
        this.maxRegisterPerIp = maxRegisterPerIp;
    }

    public Long getMaxRegisterPerIp() 
    {
        return maxRegisterPerIp;
    }

    public void setIpBlacklist(String ipBlacklist) 
    {
        this.ipBlacklist = ipBlacklist;
    }

    public String getIpBlacklist() 
    {
        return ipBlacklist;
    }

    public void setRegisterEnabled(String registerEnabled) 
    {
        this.registerEnabled = registerEnabled;
    }

    public String getRegisterEnabled() 
    {
        return registerEnabled;
    }

    public void setEmailVerificationEnabled(String emailVerificationEnabled) 
    {
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    public String getEmailVerificationEnabled() 
    {
        return emailVerificationEnabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("siteName", getSiteName())
            .append("customerServiceUrl", getCustomerServiceUrl())
            .append("popupMessage", getPopupMessage())
            .append("copyrightInfo", getCopyrightInfo())
            .append("registerBonusAmount", getRegisterBonusAmount())
            .append("maintenanceImage", getMaintenanceImage())
            .append("siteLogo", getSiteLogo())
            .append("favicon", getFavicon())
            .append("currencyType", getCurrencyType())
            .append("maxRegisterPerIp", getMaxRegisterPerIp())
            .append("ipBlacklist", getIpBlacklist())
            .append("registerEnabled", getRegisterEnabled())
            .append("emailVerificationEnabled", getEmailVerificationEnabled())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
