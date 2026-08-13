package com.order.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;
import com.order.common.i18n.Translations;

/**
 * 站内信对象 order_site_message
 * 
 * @author order
 * @date 2025-11-10
 */
public class OrderSiteMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 会员列表 */
    @Excel(name = "会员列表")
    private String memberList;

    /** 会员用户名列表 */
    private String memberNames;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private Integer isEnabled;

    /** 内容 */
    @Excel(name = "内容")
    private String content;

    /** 多语言翻译ID */
    private Long translationsId;

    /** 多语言翻译 */
    private Translations translations;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setMemberList(String memberList) 
    {
        this.memberList = memberList;
    }

    public String getMemberList() 
    {
        return memberList;
    }

    public String getMemberNames()
    {
        return memberNames;
    }

    public void setMemberNames(String memberNames)
    {
        this.memberNames = memberNames;
    }

    public void setIsEnabled(Integer isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public Integer getIsEnabled() 
    {
        return isEnabled;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public Long getTranslationsId()
    {
        return translationsId;
    }

    public void setTranslationsId(Long translationsId)
    {
        this.translationsId = translationsId;
    }

    public Translations getTranslations()
    {
        return translations;
    }

    public void setTranslations(Translations translations)
    {
        this.translations = translations;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("title", getTitle())
            .append("memberList", getMemberList())
            .append("memberNames", getMemberNames())
            .append("isEnabled", getIsEnabled())
            .append("createTime", getCreateTime())
            .append("content", getContent())
            .append("translationsId", getTranslationsId())
            .toString();
    }
}
