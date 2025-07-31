package com.brushing.set.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.brushing.common.annotation.Excel;
import com.brushing.common.core.domain.BaseEntity;

/**
 * 系统邮箱配置对象 order_email_config
 * 
 * @author brushing
 * @date 2025-07-31
 */
public class OrderEmailConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 邮件服务器地址，例如 smtp.qq.com */
    @Excel(name = "邮件服务器地址，例如 smtp.qq.com")
    private String host;

    /** 邮箱用户名（通常为邮箱地址） */
    @Excel(name = "邮箱用户名", readConverterExp = "通=常为邮箱地址")
    private String username;

    /** 邮箱密码或授权码 */
    @Excel(name = "邮箱密码或授权码")
    private String password;

    /** 发件人邮箱地址 */
    @Excel(name = "发件人邮箱地址")
    private String fromAddress;

    /** 发件人名称（显示名称） */
    @Excel(name = "发件人名称", readConverterExp = "显=示名称")
    private String fromName;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setHost(String host) 
    {
        this.host = host;
    }

    public String getHost() 
    {
        return host;
    }

    public void setUsername(String username) 
    {
        this.username = username;
    }

    public String getUsername() 
    {
        return username;
    }

    public void setPassword(String password) 
    {
        this.password = password;
    }

    public String getPassword() 
    {
        return password;
    }

    public void setFromAddress(String fromAddress) 
    {
        this.fromAddress = fromAddress;
    }

    public String getFromAddress() 
    {
        return fromAddress;
    }

    public void setFromName(String fromName) 
    {
        this.fromName = fromName;
    }

    public String getFromName() 
    {
        return fromName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("host", getHost())
            .append("username", getUsername())
            .append("password", getPassword())
            .append("fromAddress", getFromAddress())
            .append("fromName", getFromName())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
