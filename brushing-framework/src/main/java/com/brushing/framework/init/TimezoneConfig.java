package com.brushing.framework.init;


import com.brushing.system.domain.SysTimeZone;
import com.brushing.system.mapper.SysTimeZoneMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration  // 作为配置类，不依赖 DailyTask
public class
TimezoneConfig {

    private static final Logger logger = LoggerFactory.getLogger(TimezoneConfig.class);

    @Autowired
    private SysTimeZoneMapper timeZoneMapper;

    @Autowired
    private Environment env;  // 用于动态设置属性（可选）

    private String dynamicTimezone = "UTC";  // 默认

    @PostConstruct
    public void initDynamicTimezone() {
        try {
            SysTimeZone active = timeZoneMapper.getActive();
            if (active != null) {
                this.dynamicTimezone = active.getTzName();
                logger.info("初始化动态时区: {}", dynamicTimezone);
                // 可选：动态设置到 Environment（但简单起见，直接用 getter）
            } else {
                logger.warn("未找到活跃时区，使用默认 UTC");
            }
        } catch (Exception e) {
            logger.error("查询时区失败，使用默认 UTC", e);
        }
    }

    public String getDynamicTimezone() {
        return dynamicTimezone;
    }
}
