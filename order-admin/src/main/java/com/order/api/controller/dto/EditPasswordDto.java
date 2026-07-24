package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditPasswordDto {
    @NotBlank(message = "Old password must not be blank")
    @Size(max = 64, message = "Old password is too long")
    private String oldPassword;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 8, max = 64, message = "Password length must be between 8 and 64 characters")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
