package com.order.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 读取项目相关配置
 * 
 * @author order
 */
@Component
@ConfigurationProperties(prefix = "order")
public class OrderConfig
{
    /** 项目名称 */
    private String name;

    /** 版本 */
    private String version;

    /** Maven 构建时生成的版本信息，IDE 直接运行时可为空。 */
    @Autowired(required = false)
    private BuildProperties buildProperties;

    /** 版权年份 */
    private String copyrightYear;

    /** 上传路径 */
    private static String profile;

    /** 获取地址开关 */
    private static boolean addressEnabled;

    /** 验证码类型 */
    private static String captchaType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    @PostConstruct
    void resolveApplicationVersion()
    {
        version = resolveApplicationVersion(version, buildProperties);
    }

    static String resolveApplicationVersion(String configuredVersion, BuildProperties buildProperties)
    {
        if (StringUtils.hasText(configuredVersion) && !isUnresolvedMavenToken(configuredVersion))
        {
            return configuredVersion;
        }
        if (buildProperties != null && StringUtils.hasText(buildProperties.getVersion()))
        {
            return buildProperties.getVersion();
        }
        return "development";
    }

    private static boolean isUnresolvedMavenToken(String value)
    {
        String trimmed = value.trim();
        return trimmed.startsWith("@") && trimmed.endsWith("@");
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public static String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        OrderConfig.profile = profile;
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    public void setAddressEnabled(boolean addressEnabled)
    {
        OrderConfig.addressEnabled = addressEnabled;
    }

    public static String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        OrderConfig.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }
}
