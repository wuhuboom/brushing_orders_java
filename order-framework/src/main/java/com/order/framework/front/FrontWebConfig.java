package com.order.framework.front;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontWebConfig implements WebMvcConfigurer {

    @Autowired
    private FrontUserAuthInterceptor userAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/**") // 所有 /api/ 路径
                .excludePathPatterns("/api/user/login",
                        "/api/user/register",
                        "/api/user/logout",
                        "/api/config/languages",
                        "/api/config/error-messages",
                        "/api/config/content",
                        "/api/config/customer-services",
                        "/api/config/time-zone",
                        "/api/config/getCustomerService",
                        "/api/config/getGlobalConfig",
                        "/api/config/getZoneActive",
                        "/api/config/getConfigByLang",
                        "/api/config/getCustomerServiceByLang" );
    }
}
