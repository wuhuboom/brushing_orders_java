package com.brushing.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.core.domain.BaseEntity;
import com.brushing.common.xss.Xss;

/**
 * 通知公告表 sys_notice
 * 
 * @author brushing
 */
public class SysNotice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公告ID */
    private Long noticeId;

    /** 公告标题 */
    private String noticeTitle;

    /** 公告类型（1通知 2公告） */
    private String noticeType;

    /** 公告内容 */
    private String noticeContent;

    /** 公告内容 - 中文 */
    private String contentZh;

    /** 公告内容 - 英文 */
    private String contentEn;

    /** 公告内容 - 日文 */
    private String contentJa;

    /** 公告内容 - 泰文 */
    private String contentTh;

    /** 公告内容 - 韩文 */
    private String contentKo;

    /** 公告内容 - 葡萄牙 */
    private String contentPor;

    /** 公告内容 - 繁体中文 */
    private String contentZhTw;

    /** 公告状态（0正常 1关闭） */
    private String status;

    /** 中文标题 */
    private String titleZh;

    /** 英文标题 */
    private String titleEn;

    /** 日文标题 */
    private String titleJa;

    /** 泰文标题 */
    private String titleTh;

    /** 韩文标题 */
    private String titleKo;

    /** 葡萄牙文标题 */
    private String titlePor;

    /** 繁体中文标题 */
    private String titleZhTw;

    public Long getNoticeId()
    {
        return noticeId;
    }

    public void setNoticeId(Long noticeId)
    {
        this.noticeId = noticeId;
    }

    public void setNoticeTitle(String noticeTitle)
    {
        this.noticeTitle = noticeTitle;
    }

    @Xss(message = "{notice.title.xss}")
    @NotBlank(message = "{notice.title.not_blank}")
    @Size(min = 0, max = 50, message = "{notice.title.length_invalid}")
    public String getNoticeTitle()
    {
        return noticeTitle;
    }

    public void setNoticeType(String noticeType)
    {
        this.noticeType = noticeType;
    }

    public String getNoticeType()
    {
        return noticeType;
    }

    public void setNoticeContent(String noticeContent)
    {
        this.noticeContent = noticeContent;
    }

    public String getNoticeContent()
    {
        return noticeContent;
    }

    public String getContentZh() {
        return contentZh;
    }

    public void setContentZh(String contentZh) {
        this.contentZh = contentZh;
    }

    public String getContentEn() {
        return contentEn;
    }

    public void setContentEn(String contentEn) {
        this.contentEn = contentEn;
    }

    public String getContentJa() {
        return contentJa;
    }

    public void setContentJa(String contentJa) {
        this.contentJa = contentJa;
    }

    public String getContentTh() {
        return contentTh;
    }

    public void setContentTh(String contentTh) {
        this.contentTh = contentTh;
    }

    public String getContentKo() {
        return contentKo;
    }

    public void setContentKo(String contentKo) {
        this.contentKo = contentKo;
    }

    public String getContentPor() {
        return contentPor;
    }

    public void setContentPor(String contentPor) {
        this.contentPor = contentPor;
    }

    public String getContentZhTw() {
        return contentZhTw;
    }

    public void setContentZhTw(String contentZhTw) {
        this.contentZhTw = contentZhTw;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setTitleZh(String titleZh) {
        this.titleZh = titleZh;
    }

    public String getTitleZh() {
        return titleZh;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleJa(String titleJa) {
        this.titleJa = titleJa;
    }

    public String getTitleJa() {
        return titleJa;
    }

    public void setTitleTh(String titleTh) {
        this.titleTh = titleTh;
    }

    public String getTitleTh() {
        return titleTh;
    }

    public void setTitleKo(String titleKo) {
        this.titleKo = titleKo;
    }

    public String getTitleKo() {
        return titleKo;
    }

    public void setTitlePor(String titlePor) {
        this.titlePor = titlePor;
    }

    public String getTitlePor() {
        return titlePor;
    }

    public void setTitleZhTw(String titleZhTw) {
        this.titleZhTw = titleZhTw;
    }

    public String getTitleZhTw() {
        return titleZhTw;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("noticeId", getNoticeId())
            .append("noticeTitle", getNoticeTitle())
            .append("noticeType", getNoticeType())
            .append("noticeContent", getNoticeContent())
            .append("contentZh", getContentZh())
            .append("contentEn", getContentEn())
            .append("contentJa", getContentJa())
            .append("contentTh", getContentTh())
            .append("contentKo", getContentKo())
            .append("contentPor", getContentPor())
            .append("contentZhTw", getContentZhTw())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
