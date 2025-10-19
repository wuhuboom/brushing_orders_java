package com.brushing.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.brushing.common.config.BrushingConfig;
import com.brushing.common.utils.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 首页
 *
 * @author brushing
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private BrushingConfig brushingConfig;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("Welcome to the {} Admin Framework. Current version: v{}. Please access it through the frontend URL", brushingConfig.getName(), "1.0.1");
    }

    @RequestMapping("/test/v")
    public String nodeV()
    {
        ZoneId systemZoneId = ZoneId.systemDefault();

        // 获取当前时间，使用系统默认时区
        ZonedDateTime zonedDateTime = ZonedDateTime.now(systemZoneId);

        // 定义格式化模板
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 格式化为字符串
        String formattedDate = zonedDateTime.format(formatter);

        return StringUtils.format("version1.1.9,time-zone：{},time：{}", systemZoneId,formattedDate);
    }
}
