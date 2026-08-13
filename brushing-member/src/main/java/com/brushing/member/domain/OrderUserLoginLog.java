package com.brushing.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 会员登录日志对象 order_user_login_log
 *
 * @author brushing
 * @date 2026-01-11
 */
public class OrderUserLoginLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 用户名（关联 order_member_user.username） */
    private String username;

    /** IP */
    @Excel(name = "IP")
    private String ip;

    /** 地点 */
    @Excel(name = "地点")
    private String location;

    /** 登录域名 */
    @Excel(name = "登录域名")
    private String loginDomain;

    /** 浏览器 */
    @Excel(name = "浏览器")
    private String browser;

    /** 操作系统 */
    @Excel(name = "操作系统")
    private String os;

    /** 登录状态 */
    @Excel(name = "登录状态")
    private String status;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public String getIp()
    {
        return ip;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLoginDomain(String loginDomain)
    {
        this.loginDomain = loginDomain;
    }

    public String getLoginDomain()
    {
        return loginDomain;
    }

    public void setBrowser(String browser)
    {
        this.browser = browser;
    }

    public String getBrowser()
    {
        return browser;
    }

    public void setOs(String os)
    {
        this.os = os;
    }

    public String getOs()
    {
        return os;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("username", getUsername())
            .append("ip", getIp())
            .append("location", getLocation())
            .append("loginDomain", getLoginDomain())
            .append("browser", getBrowser())
            .append("os", getOs())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
