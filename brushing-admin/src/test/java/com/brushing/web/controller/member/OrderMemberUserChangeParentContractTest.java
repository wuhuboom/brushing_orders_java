package com.brushing.web.controller.member;

import com.brushing.common.constant.HttpStatus;
import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.spring.SpringUtils;
import com.brushing.member.service.IOrderMemberUserService;
import com.brushing.web.controller.member.dto.ChangeParentDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderMemberUserChangeParentContractTest {

    private static final String CONCURRENT_MESSAGE =
            "The member hierarchy is being changed concurrently. Refresh the list and try again";

    private IOrderMemberUserService memberUserService;
    private OrderMemberUserController controller;

    @BeforeAll
    static void configureMessages() {
        LocaleContextHolder.setDefaultLocale(Locale.ENGLISH);
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("member.user.parent.concurrent_conflict", Locale.ENGLISH,
                CONCURRENT_MESSAGE);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("messageSource", messages);
        new SpringUtils().postProcessBeanFactory(beanFactory);
    }

    @BeforeEach
    void setUpController() {
        memberUserService = mock(IOrderMemberUserService.class);
        controller = spy(new OrderMemberUserController());
        ReflectionTestUtils.setField(controller, "orderMemberUserService", memberUserService);
    }

    @Test
    void dedicatedEndpointRequiresDedicatedPermission() throws Exception {
        Method method = OrderMemberUserController.class
                .getMethod("changeParent", ChangeParentDto.class);

        assertEquals("@ss.hasPermi('member:member:changeParent')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/changeParent", method.getAnnotation(PutMapping.class).value()[0]);
    }

    @Test
    void requestRequiresMemberParentAndVersion() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertEquals(3, validator.validate(new ChangeParentDto()).size());
        }
    }

    @Test
    void retriesOnceOutsideTheTransactionAndSendsOneMessageAfterSuccess() {
        ChangeParentDto dto = validRequest();
        when(memberUserService.changeParent(88L, "100", 7L))
                .thenThrow(new CannotAcquireLockException("deadlock victim"))
                .thenReturn(1);
        doNothing().when(controller).sendUserInfoMessage(anyLong(), anyString());

        assertEquals(HttpStatus.SUCCESS, controller.changeParent(dto).get("code"));

        verify(memberUserService, times(2)).changeParent(88L, "100", 7L);
        verify(controller, times(1)).sendUserInfoMessage(88L, "1");
    }

    @Test
    void mapsTwoTransientLockFailuresToLocalizedConflictWithoutWebsocketMessage() {
        ChangeParentDto dto = validRequest();
        when(memberUserService.changeParent(88L, "100", 7L))
                .thenThrow(new CannotAcquireLockException("first attempt"))
                .thenThrow(new CannotAcquireLockException("second attempt"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.changeParent(dto));

        assertEquals(HttpStatus.CONFLICT, exception.getCode());
        assertEquals(CONCURRENT_MESSAGE, exception.getMessage());
        verify(memberUserService, times(2)).changeParent(88L, "100", 7L);
        verify(controller, never()).sendUserInfoMessage(anyLong(), anyString());
    }

    @Test
    void doesNotRetryOrdinaryBusinessFailure() {
        ChangeParentDto dto = validRequest();
        ServiceException businessFailure = new ServiceException("invalid parent");
        when(memberUserService.changeParent(88L, "100", 7L)).thenThrow(businessFailure);

        ServiceException actual = assertThrows(ServiceException.class,
                () -> controller.changeParent(dto));

        assertSame(businessFailure, actual);
        verify(memberUserService, times(1)).changeParent(88L, "100", 7L);
        verify(controller, never()).sendUserInfoMessage(anyLong(), anyString());
    }

    private static ChangeParentDto validRequest() {
        ChangeParentDto dto = new ChangeParentDto();
        dto.setMemberId(88L);
        dto.setParentIdentifier("100");
        dto.setVersion(7L);
        return dto;
    }
}
