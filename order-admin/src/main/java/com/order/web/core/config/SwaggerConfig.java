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
 * H5 OpenAPI 基础配置。
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
     * 添加摘要信息。
     */
    public Info getApiInfo()
    {
        return new Info()
            .title("H5 前台接口文档")
            .description("""
                    仅包含 `/api/**` 下的 H5 接口，不包含后台管理端接口。

                    响应中的 `code` 是业务状态码：`200` 表示成功，其他值表示具体业务失败原因；
                    REST 风格接口还会使用对应的 HTTP 状态码，兼容接口可能始终返回 HTTP 200，
                    调用方应始终以响应体中的 `code` 为准。
                    """)
            .contact(new Contact().name(orderConfig.getName()))
            .version("版本号：" + orderConfig.getVersion());
    }
}
