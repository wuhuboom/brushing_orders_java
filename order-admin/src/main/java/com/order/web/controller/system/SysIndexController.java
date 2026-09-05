package com.order.web.controller.system;

import com.order.framework.init.GeoIpQueryQueryService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.order.common.config.OrderConfig;
import com.order.common.utils.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 首页
 *
 * @author order
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private OrderConfig orderConfig;



    @Resource
    GeoIpQueryQueryService ipQueryQueryService;

    @RequestMapping("/test/ip")
    public String testIp(String ip) {
        return ipQueryQueryService.queryByIp(ip);
    }

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        ZoneId systemZoneId = ZoneId.systemDefault();

        // 获取当前时间，使用系统默认时区
        ZonedDateTime zonedDateTime = ZonedDateTime.now(systemZoneId);

        // 定义格式化模板
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 格式化为字符串
        String formattedDate = zonedDateTime.format(formatter);

        return StringUtils.format("version：{},time-zone：{},time：{}", orderConfig.getVersion(), systemZoneId,formattedDate);
    }

    @GetMapping("/test/v")
    public String nodeV()
    {
        ZoneId systemZoneId = ZoneId.systemDefault();

        // 获取当前时间，使用系统默认时区
        ZonedDateTime zonedDateTime = ZonedDateTime.now(systemZoneId);

        // 定义格式化模板
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 格式化为字符串
        String formattedDate = zonedDateTime.format(formatter);

        return StringUtils.format("version：{},time-zone：{},time：{}", orderConfig.getVersion(), systemZoneId,formattedDate);
    }
}
