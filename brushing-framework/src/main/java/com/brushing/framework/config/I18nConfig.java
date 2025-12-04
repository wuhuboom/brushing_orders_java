package com.brushing.framework.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import jakarta.validation.Validator;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.context.i18n.LocaleContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;


/**
 * 资源文件配置加载
 * 
 * @author brushing
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer
{
    private static final Logger log = LoggerFactory.getLogger(I18nConfig.class);
    @Bean
    public LocaleResolver localeResolver()
    {
        CookieLocaleResolver clr = new CookieLocaleResolver();
        // 默认语言
        clr.setDefaultLocale(Locale.ENGLISH);
        clr.setCookieName("LOCALE");
        // 保证 cookie 在整个应用路径上有效
        clr.setCookiePath("/");
        // 允许前端脚本读取（调试时使用），生产环境可改回 true
        clr.setCookieHttpOnly(false);
        // 30 days
        clr.setCookieMaxAge(60 * 60 * 24 * 30);
        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");  // 确保 paramName 是 'lang'
        // 允许所有 HTTP 方法，前端可以使用 GET/POST/AJAX 等任意方式切换
        // interceptor.setHttpMethods("GET");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(localeChangeInterceptor());
        // 每次请求将解析到的 Locale 设置到 LocaleContextHolder，确保 MessageUtils 使用正确的 locale
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String cookieHeader = request.getHeader("Cookie");
               // log.debug("Incoming request cookies: {}", cookieHeader);
                LocaleResolver localeResolver = (LocaleResolver) request.getAttribute(org.springframework.web.servlet.DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
                if (localeResolver == null) {
                    localeResolver = org.springframework.web.servlet.support.RequestContextUtils.getLocaleResolver(request);
                }
                // 先检查请求参数或自定义头，优先使用它们来切换语言（适用于跨域且无法使用 cookie 的情况）
                String langParam = request.getParameter("lang");
                String xLang = request.getHeader("X-Lang");
                String langToUse = (langParam != null && !langParam.isEmpty()) ? langParam : (xLang != null && !xLang.isEmpty() ? xLang : null);
                if (langToUse != null) {
                    String normalized = langToUse.replace('-', '_');
                    String[] parts = normalized.split("_");
                    Locale parsed;
                    if (parts.length == 1) {
                        parsed = new Locale(parts[0]);
                    } else {
                        parsed = new Locale(parts[0], parts[1]);
                    }
                  //  log.debug("Locale parsed from request param/header: {} -> {}", langToUse, parsed);
                    LocaleContextHolder.setLocale(parsed);
                    if (localeResolver != null) {
                        try {
                            localeResolver.setLocale(request, response, parsed);
                        } catch (Exception ex) {
                            log.debug("LocaleResolver.setLocale threw: {}", ex.getMessage());
                        }
                    }
                    return true;
                }
                if (localeResolver != null) {
                //    log.debug("Using LocaleResolver implementation: {}", localeResolver.getClass().getName());
                    Locale locale = localeResolver.resolveLocale(request);
               //     log.debug("Resolved locale in interceptor: {}", locale);
                    LocaleContextHolder.setLocale(locale);
                } else {
                 //   log.warn("No LocaleResolver found for request");
                }
                return true;
            }

            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
                // no-op
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
                LocaleContextHolder.resetLocaleContext();
            }
        });
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:i18n/messages"); // 确保文件位于 resources/i18n/messages_*.properties
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }

    @Bean
    public Validator validator(MessageSource messageSource) {
        LocalValidatorFactoryBean lvfb = new LocalValidatorFactoryBean();
        lvfb.setValidationMessageSource(messageSource);
        return lvfb;
    }
}
