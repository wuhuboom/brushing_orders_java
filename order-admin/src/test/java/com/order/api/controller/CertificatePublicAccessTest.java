package com.order.api.controller;

import com.order.framework.front.FrontUserAuthInterceptor;
import com.order.framework.front.FrontWebConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CertificatePublicAccessTest {
    @Test
    void anonymousCertificateSettingsAreExcludedWithoutExposingMemberApis() {
        FrontWebConfig config = new FrontWebConfig();
        ReflectionTestUtils.setField(config, "userAuthInterceptor", mock(FrontUserAuthInterceptor.class));
        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);
        java.util.List<?> interceptors = ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
        assertNotNull(interceptors);
        assertEquals(1, interceptors.size());
        MappedInterceptor mapped = assertInstanceOf(MappedInterceptor.class, interceptors.get(0));
        for (Map.Entry<String, Boolean> entry : Map.of(
                "/api/config/certificate", false,
                "/api/config/content", false,
                "/api/config/content-relative", false,
                "/api/config/website", false,
                "/api/user/getInfo", true,
                "/api/order", true).entrySet()) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", entry.getKey());
            ServletRequestPathUtils.parseAndCache(request);
            assertEquals(entry.getValue(), mapped.matches(request), entry.getKey());
        }
    }
}
