package com.brushing.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.brushing.common.config.BrushingConfig;
import com.brushing.common.utils.StringUtils;

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
        return StringUtils.format("Welcome to the {} Admin Framework. Current version: v{}. Please access it through the frontend URL", brushingConfig.getName(), brushingConfig.getVersion());
    }
}
