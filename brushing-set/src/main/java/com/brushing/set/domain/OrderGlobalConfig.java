package com.brushing.set.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 全局配置（中英文内容）对象 order_global_config
 * 
 * @author brushing
 * @date 2025-07-31
 */
public class OrderGlobalConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 注册协议（英文） */
    @Excel(name = "注册协议", readConverterExp = "英=文")
    private String registerProtocolEn;

    /** 注册协议（非英文） */
    @Excel(name = "注册协议", readConverterExp = "非=英文")
    private String registerProtocolLocal;

    /** 关于我们（英文） */
    @Excel(name = "关于我们", readConverterExp = "英=文")
    private String aboutUsEn;

    /** 关于我们（非英文） */
    @Excel(name = "关于我们", readConverterExp = "非=英文")
    private String aboutUsLocal;

    /** 证书（英文） */
    @Excel(name = "证书", readConverterExp = "英=文")
    private String certificateEn;

    /** 证书（非英文） */
    @Excel(name = "证书", readConverterExp = "非=英文")
    private String certificateLocal;

    /** 常见问题（英文） */
    @Excel(name = "常见问题", readConverterExp = "英=文")
    private String faqEn;

    /** 常见问题（非英文） */
    @Excel(name = "常见问题", readConverterExp = "非=英文")
    private String faqLocal;

    /** 最新事件（英文） */
    @Excel(name = "最新事件", readConverterExp = "英=文")
    private String latestEventEn;

    /** 最新事件（非英文） */
    @Excel(name = "最新事件", readConverterExp = "非=英文")
    private String latestEventLocal;

    /** 条款条规（英文） */
    @Excel(name = "条款条规", readConverterExp = "英=文")
    private String termsEn;

    /** 条款条规（非英文） */
    @Excel(name = "条款条规", readConverterExp = "非=英文")
    private String termsLocal;

    /** 收入指南（英文） */
    @Excel(name = "收入指南", readConverterExp = "英=文")
    private String incomeGuideEn;

    /** 收入指南（非英文） */
    @Excel(name = "收入指南", readConverterExp = "非=英文")
    private String incomeGuideLocal;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRegisterProtocolEn(String registerProtocolEn) 
    {
        this.registerProtocolEn = registerProtocolEn;
    }

    public String getRegisterProtocolEn() 
    {
        return registerProtocolEn;
    }

    public void setRegisterProtocolLocal(String registerProtocolLocal) 
    {
        this.registerProtocolLocal = registerProtocolLocal;
    }

    public String getRegisterProtocolLocal() 
    {
        return registerProtocolLocal;
    }

    public void setAboutUsEn(String aboutUsEn) 
    {
        this.aboutUsEn = aboutUsEn;
    }

    public String getAboutUsEn() 
    {
        return aboutUsEn;
    }

    public void setAboutUsLocal(String aboutUsLocal) 
    {
        this.aboutUsLocal = aboutUsLocal;
    }

    public String getAboutUsLocal() 
    {
        return aboutUsLocal;
    }

    public void setCertificateEn(String certificateEn) 
    {
        this.certificateEn = certificateEn;
    }

    public String getCertificateEn() 
    {
        return certificateEn;
    }

    public void setCertificateLocal(String certificateLocal) 
    {
        this.certificateLocal = certificateLocal;
    }

    public String getCertificateLocal() 
    {
        return certificateLocal;
    }

    public void setFaqEn(String faqEn) 
    {
        this.faqEn = faqEn;
    }

    public String getFaqEn() 
    {
        return faqEn;
    }

    public void setFaqLocal(String faqLocal) 
    {
        this.faqLocal = faqLocal;
    }

    public String getFaqLocal() 
    {
        return faqLocal;
    }

    public void setLatestEventEn(String latestEventEn) 
    {
        this.latestEventEn = latestEventEn;
    }

    public String getLatestEventEn() 
    {
        return latestEventEn;
    }

    public void setLatestEventLocal(String latestEventLocal) 
    {
        this.latestEventLocal = latestEventLocal;
    }

    public String getLatestEventLocal() 
    {
        return latestEventLocal;
    }

    public void setTermsEn(String termsEn) 
    {
        this.termsEn = termsEn;
    }

    public String getTermsEn() 
    {
        return termsEn;
    }

    public void setTermsLocal(String termsLocal) 
    {
        this.termsLocal = termsLocal;
    }

    public String getTermsLocal() 
    {
        return termsLocal;
    }

    public void setIncomeGuideEn(String incomeGuideEn) 
    {
        this.incomeGuideEn = incomeGuideEn;
    }

    public String getIncomeGuideEn() 
    {
        return incomeGuideEn;
    }

    public void setIncomeGuideLocal(String incomeGuideLocal) 
    {
        this.incomeGuideLocal = incomeGuideLocal;
    }

    public String getIncomeGuideLocal() 
    {
        return incomeGuideLocal;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("registerProtocolEn", getRegisterProtocolEn())
            .append("registerProtocolLocal", getRegisterProtocolLocal())
            .append("aboutUsEn", getAboutUsEn())
            .append("aboutUsLocal", getAboutUsLocal())
            .append("certificateEn", getCertificateEn())
            .append("certificateLocal", getCertificateLocal())
            .append("faqEn", getFaqEn())
            .append("faqLocal", getFaqLocal())
            .append("latestEventEn", getLatestEventEn())
            .append("latestEventLocal", getLatestEventLocal())
            .append("termsEn", getTermsEn())
            .append("termsLocal", getTermsLocal())
            .append("incomeGuideEn", getIncomeGuideEn())
            .append("incomeGuideLocal", getIncomeGuideLocal())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
