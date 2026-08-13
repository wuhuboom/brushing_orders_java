package com.brushing.set.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 全局配置（中英文内容）对象 order_global_config
 *
 * @author brushing
 * @date 2025-10-18
 */
public class OrderGlobalConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 注册协议（英文） */
    @Excel(name = "注册协议", readConverterExp = "英文")
    private String registerProtocolEn;

    /** 注册协议（非英文） */
    @Excel(name = "注册协议", readConverterExp = "中文")
    private String registerProtocolLocal;

    /** 关于我们（英文） */
    @Excel(name = "关于我们", readConverterExp = "英文")
    private String aboutUsEn;

    /** 关于我们（非英文） */
    @Excel(name = "关于我们", readConverterExp = "中文")
    private String aboutUsLocal;

    /** 证书（英文） */
    @Excel(name = "证书", readConverterExp = "英文")
    private String certificateEn;

    /** 证书（非英文） */
    @Excel(name = "证书", readConverterExp = "中文")
    private String certificateLocal;

    /** 常见问题（英文） */
    @Excel(name = "常见问题", readConverterExp = "英文")
    private String faqEn;

    /** 常见问题（非英文） */
    @Excel(name = "常见问题", readConverterExp = "中文")
    private String faqLocal;

    /** 最新事件（英文） */
    @Excel(name = "最新事件", readConverterExp = "英文")
    private String latestEventEn;

    /** 最新事件（非英文） */
    @Excel(name = "最新事件", readConverterExp = "中文")
    private String latestEventLocal;

    /** 条款条规（英文） */
    @Excel(name = "条款条规", readConverterExp = "英文")
    private String termsEn;

    /** 条款条规（非英文） */
    @Excel(name = "条款条规", readConverterExp = "中文")
    private String termsLocal;

    /** 收入指南（英文） */
    @Excel(name = "收入指南", readConverterExp = "英文")
    private String incomeGuideEn;

    /** 收入指南（非英文） */
    @Excel(name = "收入指南", readConverterExp = "中文")
    private String incomeGuideLocal;

    /** 关于我们 - 中文繁体 */
    @Excel(name = "关于我们 - 中文繁体")
    private String aboutUsZhTw;

    /** 注册协议 - 韩文 */
    @Excel(name = "注册协议 - 韩文")
    private String registrationAgreementKo;

    /** 关于我们 - 韩文 */
    @Excel(name = "关于我们 - 韩文")
    private String aboutUsKo;

    /** 注册协议 - 中文繁体 */
    @Excel(name = "注册协议 - 中文繁体")
    private String registrationAgreementZhTw;

    /** 条款条规 - 中文繁体 */
    @Excel(name = "条款条规 - 中文繁体")
    private String termsConditionsZhTw;

    /** 收入指南 - 泰文 */
    @Excel(name = "收入指南 - 泰文")
    private String incomeGuideTh;

    /** 证书 - 日文 */
    @Excel(name = "证书 - 日文")
    private String certificateJa;

    /** 证书 - 泰文 */
    @Excel(name = "证书 - 泰文")
    private String certificateTh;

    /** 最新事件 - 泰文 */
    @Excel(name = "最新事件 - 泰文")
    private String latestEventsTh;

    /** 条款条规 - 日文 */
    @Excel(name = "条款条规 - 日文")
    private String termsConditionsJa;

    /** 常见问题 - 日文 */
    @Excel(name = "常见问题 - 日文")
    private String faqJa;

    /** 收入指南 - 日文 */
    @Excel(name = "收入指南 - 日文")
    private String incomeGuideJa;

    /** 条款条规 - 泰文 */
    @Excel(name = "条款条规 - 泰文")
    private String termsConditionsTh;

    /** 证书 - 韩文 */
    @Excel(name = "证书 - 韩文")
    private String certificateKo;

    /** 最新事件 - 日文 */
    @Excel(name = "最新事件 - 日文")
    private String latestEventsJa;

    /** 常见问题 - 泰文 */
    @Excel(name = "常见问题 - 泰文")
    private String faqTh;

    /** 最新事件 - 韩文 */
    @Excel(name = "最新事件 - 韩文")
    private String latestEventsKo;

    /** 注册协议 - 日文 */
    @Excel(name = "注册协议 - 日文")
    private String registrationAgreementJa;

    /** 条款条规 - 韩文 */
    @Excel(name = "条款条规 - 韩文")
    private String termsConditionsKo;

    /** 注册协议 - 泰文 */
    @Excel(name = "注册协议 - 泰文")
    private String registrationAgreementTh;

    /** 常见问题 - 中文繁体 */
    @Excel(name = "常见问题 - 中文繁体")
    private String faqZhTw;

    /** 最新事件 - 中文繁体 */
    @Excel(name = "最新事件 - 中文繁体")
    private String latestEventsZhTw;

    /** 关于我们 - 泰文 */
    @Excel(name = "关于我们 - 泰文")
    private String aboutUsTh;

    /** 关于我们 - 日文 */
    @Excel(name = "关于我们 - 日文")
    private String aboutUsJa;

    /** 收入指南 - 韩文 */
    @Excel(name = "收入指南 - 韩文")
    private String incomeGuideKo;

    /** 证书 - 中文繁体 */
    @Excel(name = "证书 - 中文繁体")
    private String certificateZhTw;

    /** 常见问题 - 韩文 */
    @Excel(name = "常见问题 - 韩文")
    private String faqKo;

    /** 收入指南 - 中文繁体 */
    @Excel(name = "收入指南 - 中文繁体")
    private String incomeGuideZhTw;

    @Excel(name = "注册协议 - 葡萄牙")
    private String registrationAgreementPor;

    @Excel(name = "关于我们 - 葡萄牙")
    private String aboutUsPor;

    @Excel(name = "证书 - 葡萄牙")
    private String certificatePor;

    @Excel(name = "常见问题 - 葡萄牙")
    private String faqPor;

    @Excel(name = "最新事件 - 葡萄牙")
    private String latestEventsPor;

    @Excel(name = "条款条规 - 葡萄牙")
    private String termsConditionsPor;

    @Excel(name = "收入指南 - 葡萄牙")
    private String incomeGuidePor;

    @Excel(name = "注册协议 - 西班牙语")
    private String registrationAgreementEs;

    @Excel(name = "关于我们 - 西班牙语")
    private String aboutUsEs;

    @Excel(name = "证书 - 西班牙语")
    private String certificateEs;

    @Excel(name = "常见问题 - 西班牙语")
    private String faqEs;

    @Excel(name = "最新事件 - 西班牙语")
    private String latestEventsEs;

    @Excel(name = "条款条规 - 西班牙语")
    private String termsConditionsEs;

    @Excel(name = "收入指南 - 西班牙语")
    private String incomeGuideEs;


    public String getRegistrationAgreementEs() {
        return registrationAgreementEs;
    }

    public void setRegistrationAgreementEs(String registrationAgreementEs) {
        this.registrationAgreementEs = registrationAgreementEs;
    }

    public String getAboutUsEs() {
        return aboutUsEs;
    }

    public void setAboutUsEs(String aboutUsEs) {
        this.aboutUsEs = aboutUsEs;
    }

    public String getCertificateEs() {
        return certificateEs;
    }

    public void setCertificateEs(String certificateEs) {
        this.certificateEs = certificateEs;
    }

    public String getFaqEs() {
        return faqEs;
    }

    public void setFaqEs(String faqEs) {
        this.faqEs = faqEs;
    }

    public String getLatestEventsEs() {
        return latestEventsEs;
    }

    public void setLatestEventsEs(String latestEventsEs) {
        this.latestEventsEs = latestEventsEs;
    }

    public String getTermsConditionsEs() {
        return termsConditionsEs;
    }

    public void setTermsConditionsEs(String termsConditionsEs) {
        this.termsConditionsEs = termsConditionsEs;
    }

    public String getIncomeGuideEs() {
        return incomeGuideEs;
    }

    public void setIncomeGuideEs(String incomeGuideEs) {
        this.incomeGuideEs = incomeGuideEs;
    }

    public String getRegistrationAgreementPor() {
        return registrationAgreementPor;
    }

    public void setRegistrationAgreementPor(String registrationAgreementPor) {
        this.registrationAgreementPor = registrationAgreementPor;
    }

    public String getAboutUsPor() {
        return aboutUsPor;
    }

    public void setAboutUsPor(String aboutUsPor) {
        this.aboutUsPor = aboutUsPor;
    }

    public String getCertificatePor() {
        return certificatePor;
    }

    public void setCertificatePor(String certificatePor) {
        this.certificatePor = certificatePor;
    }

    public String getFaqPor() {
        return faqPor;
    }

    public void setFaqPor(String faqPor) {
        this.faqPor = faqPor;
    }

    public String getLatestEventsPor() {
        return latestEventsPor;
    }

    public void setLatestEventsPor(String latestEventsPor) {
        this.latestEventsPor = latestEventsPor;
    }

    public String getTermsConditionsPor() {
        return termsConditionsPor;
    }

    public void setTermsConditionsPor(String termsConditionsPor) {
        this.termsConditionsPor = termsConditionsPor;
    }

    public String getIncomeGuidePor() {
        return incomeGuidePor;
    }

    public void setIncomeGuidePor(String incomeGuidePor) {
        this.incomeGuidePor = incomeGuidePor;
    }

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

    public void setAboutUsZhTw(String aboutUsZhTw)
    {
        this.aboutUsZhTw = aboutUsZhTw;
    }

    public String getAboutUsZhTw()
    {
        return aboutUsZhTw;
    }

    public void setRegistrationAgreementKo(String registrationAgreementKo)
    {
        this.registrationAgreementKo = registrationAgreementKo;
    }

    public String getRegistrationAgreementKo()
    {
        return registrationAgreementKo;
    }

    public void setAboutUsKo(String aboutUsKo)
    {
        this.aboutUsKo = aboutUsKo;
    }

    public String getAboutUsKo()
    {
        return aboutUsKo;
    }

    public void setRegistrationAgreementZhTw(String registrationAgreementZhTw)
    {
        this.registrationAgreementZhTw = registrationAgreementZhTw;
    }

    public String getRegistrationAgreementZhTw()
    {
        return registrationAgreementZhTw;
    }

    public void setTermsConditionsZhTw(String termsConditionsZhTw)
    {
        this.termsConditionsZhTw = termsConditionsZhTw;
    }

    public String getTermsConditionsZhTw()
    {
        return termsConditionsZhTw;
    }

    public void setIncomeGuideTh(String incomeGuideTh)
    {
        this.incomeGuideTh = incomeGuideTh;
    }

    public String getIncomeGuideTh()
    {
        return incomeGuideTh;
    }

    public void setCertificateJa(String certificateJa)
    {
        this.certificateJa = certificateJa;
    }

    public String getCertificateJa()
    {
        return certificateJa;
    }

    public void setCertificateTh(String certificateTh)
    {
        this.certificateTh = certificateTh;
    }

    public String getCertificateTh()
    {
        return certificateTh;
    }

    public void setLatestEventsTh(String latestEventsTh)
    {
        this.latestEventsTh = latestEventsTh;
    }

    public String getLatestEventsTh()
    {
        return latestEventsTh;
    }

    public void setTermsConditionsJa(String termsConditionsJa)
    {
        this.termsConditionsJa = termsConditionsJa;
    }

    public String getTermsConditionsJa()
    {
        return termsConditionsJa;
    }

    public void setFaqJa(String faqJa)
    {
        this.faqJa = faqJa;
    }

    public String getFaqJa()
    {
        return faqJa;
    }

    public void setIncomeGuideJa(String incomeGuideJa)
    {
        this.incomeGuideJa = incomeGuideJa;
    }

    public String getIncomeGuideJa()
    {
        return incomeGuideJa;
    }

    public void setTermsConditionsTh(String termsConditionsTh)
    {
        this.termsConditionsTh = termsConditionsTh;
    }

    public String getTermsConditionsTh()
    {
        return termsConditionsTh;
    }

    public void setCertificateKo(String certificateKo)
    {
        this.certificateKo = certificateKo;
    }

    public String getCertificateKo()
    {
        return certificateKo;
    }

    public void setLatestEventsJa(String latestEventsJa)
    {
        this.latestEventsJa = latestEventsJa;
    }

    public String getLatestEventsJa()
    {
        return latestEventsJa;
    }

    public void setFaqTh(String faqTh)
    {
        this.faqTh = faqTh;
    }

    public String getFaqTh()
    {
        return faqTh;
    }

    public void setLatestEventsKo(String latestEventsKo)
    {
        this.latestEventsKo = latestEventsKo;
    }

    public String getLatestEventsKo()
    {
        return latestEventsKo;
    }

    public void setRegistrationAgreementJa(String registrationAgreementJa)
    {
        this.registrationAgreementJa = registrationAgreementJa;
    }

    public String getRegistrationAgreementJa()
    {
        return registrationAgreementJa;
    }

    public void setTermsConditionsKo(String termsConditionsKo)
    {
        this.termsConditionsKo = termsConditionsKo;
    }

    public String getTermsConditionsKo()
    {
        return termsConditionsKo;
    }

    public void setRegistrationAgreementTh(String registrationAgreementTh)
    {
        this.registrationAgreementTh = registrationAgreementTh;
    }

    public String getRegistrationAgreementTh()
    {
        return registrationAgreementTh;
    }

    public void setFaqZhTw(String faqZhTw)
    {
        this.faqZhTw = faqZhTw;
    }

    public String getFaqZhTw()
    {
        return faqZhTw;
    }

    public void setLatestEventsZhTw(String latestEventsZhTw)
    {
        this.latestEventsZhTw = latestEventsZhTw;
    }

    public String getLatestEventsZhTw()
    {
        return latestEventsZhTw;
    }

    public void setAboutUsTh(String aboutUsTh)
    {
        this.aboutUsTh = aboutUsTh;
    }

    public String getAboutUsTh()
    {
        return aboutUsTh;
    }

    public void setAboutUsJa(String aboutUsJa)
    {
        this.aboutUsJa = aboutUsJa;
    }

    public String getAboutUsJa()
    {
        return aboutUsJa;
    }

    public void setIncomeGuideKo(String incomeGuideKo)
    {
        this.incomeGuideKo = incomeGuideKo;
    }

    public String getIncomeGuideKo()
    {
        return incomeGuideKo;
    }

    public void setCertificateZhTw(String certificateZhTw)
    {
        this.certificateZhTw = certificateZhTw;
    }

    public String getCertificateZhTw()
    {
        return certificateZhTw;
    }

    public void setFaqKo(String faqKo)
    {
        this.faqKo = faqKo;
    }

    public String getFaqKo()
    {
        return faqKo;
    }

    public void setIncomeGuideZhTw(String incomeGuideZhTw)
    {
        this.incomeGuideZhTw = incomeGuideZhTw;
    }

    public String getIncomeGuideZhTw()
    {
        return incomeGuideZhTw;
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
                .append("aboutUsZhTw", getAboutUsZhTw())
                .append("registrationAgreementKo", getRegistrationAgreementKo())
                .append("aboutUsKo", getAboutUsKo())
                .append("registrationAgreementZhTw", getRegistrationAgreementZhTw())
                .append("termsConditionsZhTw", getTermsConditionsZhTw())
                .append("incomeGuideTh", getIncomeGuideTh())
                .append("certificateJa", getCertificateJa())
                .append("certificateTh", getCertificateTh())
                .append("latestEventsTh", getLatestEventsTh())
                .append("termsConditionsJa", getTermsConditionsJa())
                .append("faqJa", getFaqJa())
                .append("incomeGuideJa", getIncomeGuideJa())
                .append("termsConditionsTh", getTermsConditionsTh())
                .append("certificateKo", getCertificateKo())
                .append("latestEventsJa", getLatestEventsJa())
                .append("faqTh", getFaqTh())
                .append("latestEventsKo", getLatestEventsKo())
                .append("registrationAgreementJa", getRegistrationAgreementJa())
                .append("termsConditionsKo", getTermsConditionsKo())
                .append("registrationAgreementTh", getRegistrationAgreementTh())
                .append("faqZhTw", getFaqZhTw())
                .append("latestEventsZhTw", getLatestEventsZhTw())
                .append("aboutUsTh", getAboutUsTh())
                .append("aboutUsJa", getAboutUsJa())
                .append("incomeGuideKo", getIncomeGuideKo())
                .append("certificateZhTw", getCertificateZhTw())
                .append("faqKo", getFaqKo())
                .append("incomeGuideZhTw", getIncomeGuideZhTw())
                .toString();
    }
}
