package com.order.api.controller.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditPasswordDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void newPasswordAcceptsSixCharacters() {
        assertTrue(validator.validate(requestWithNewPassword("123456")).isEmpty());
    }

    @Test
    void newPasswordRejectsFewerThanSixCharacters() {
        assertFalse(validator.validate(requestWithNewPassword("12345")).isEmpty());
    }

    private EditPasswordDto requestWithNewPassword(String newPassword) {
        EditPasswordDto request = new EditPasswordDto();
        request.setOldPassword("old-password");
        request.setNewPassword(newPassword);
        return request;
    }
}
