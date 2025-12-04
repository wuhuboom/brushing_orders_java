package com.order.member.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.order.common.annotation.Excel;
import com.order.common.core.domain.BaseEntity;

/**
 * 登录日志对象 order_login_log
 * 
 * @author order
 * @date 2025-11-10
 */
public class OrderLoginLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增ID，唯一标识 */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** IP地址 */
    @Excel(name = "IP地址")
    private String ip;

    /** 地址 */
    @Excel(name = "地址")
    private String address;

    /** 参数 */
    @Excel(name = "参数")
    private String loginParams;

    /** 用户名（来自 order_user） */
    private String username;

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

    public void setIp(String ip) 
    {
        this.ip = ip;
    }

    public String getIp() 
    {
        return ip;
    }

    public void setAddress(String address) 
    {
        this.address = address;
    }

    public String getAddress() 
    {
        return address;
    }

    public void setLoginParams(String loginParams) 
    {
        this.loginParams = loginParams;
    }

    public String getLoginParams() 
    {
        return loginParams;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("ip", getIp())
            .append("address", getAddress())
            .append("username", getUsername())
            .append("createTime", getCreateTime())
            .append("loginParams", getLoginParams())
            .toString();
    }
}
