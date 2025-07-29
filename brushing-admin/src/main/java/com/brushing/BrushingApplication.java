package com.brushing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author brushing
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class BrushingApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(BrushingApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  管理平台启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
