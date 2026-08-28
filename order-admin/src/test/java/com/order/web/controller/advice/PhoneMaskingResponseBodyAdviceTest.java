package com.order.web.controller.advice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.core.domain.entity.SysUser;
import com.order.common.core.domain.model.LoginUser;
import com.order.common.core.page.TableDataInfo;
import com.order.system.service.ISystemAlignmentService;

class PhoneMaskingResponseBodyAdviceTest
{
    private final ISystemAlignmentService alignmentService = mock(ISystemAlignmentService.class);
    private final PhoneMaskingResponseBodyAdvice advice = new PhoneMaskingResponseBodyAdvice();

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(advice, "alignmentService", alignmentService);
        ReflectionTestUtils.setField(advice, "objectMapper", new ObjectMapper());
        authenticateOperator();
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adviceIsNotAppliedToManagementConsoleControllers()
    {
        ControllerAdvice annotation = PhoneMaskingResponseBodyAdvice.class
                .getAnnotation(ControllerAdvice.class);

        assertArrayEquals(
                new String[] {"com.order.api.controller"},
                annotation.basePackages());
    }

    @Test
    void customerFacingAdviceStillHonorsLegacyPhoneMaskingSetting()
    {
        when(alignmentService.shouldHidePhone(9L)).thenReturn(true);

        JsonNode result = assertInstanceOf(JsonNode.class, write(phoneTable()));

        assertEquals("+12****4455", result.path("rows").path(0).path("phoneNumber").asText());
        verify(alignmentService).shouldHidePhone(9L);
    }

    private void authenticateOperator()
    {
        SysUser user = new SysUser();
        LoginUser loginUser = new LoginUser(9L, null, user, Set.of("member:orderuser:list"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private Object write(TableDataInfo body)
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        return advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                null,
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(new MockHttpServletResponse()));
    }

    private TableDataInfo phoneTable()
    {
        return new TableDataInfo(List.of(Map.of("phoneNumber", "+12345674455")), 1L);
    }
}
