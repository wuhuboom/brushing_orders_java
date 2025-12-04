package com.order.web.core.config;

import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.order.common.config.OrderConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.List;

/**
 * Swagger2的接口配置
 * 
 * @author order
 */
@Configuration
public class SwaggerConfig
{
    /** 系统基础配置 */
    @Autowired
    private OrderConfig orderConfig;
    
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        // 设置认证的请求头
                        .addSecuritySchemes("BearerAuth", securityScheme()))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .info(getApiInfo());
    }

    @Bean
    public SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");
    }
    
    /**
     * 添加摘要信息
     */
    public Info getApiInfo()
    {
        return new Info()
            // 设置标题
            .title("标题：DataCenter管理系统_接口文档")
            // 描述
            .description("描述")
            // 作者信息
            .contact(new Contact().name(orderConfig.getName()))
            // 版本
            .version("版本号:" + orderConfig.getVersion());
    }
}
