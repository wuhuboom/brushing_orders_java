package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginUserDto {
    @NotBlank(message = "Username must not be blank")
    @Pattern(regexp = "[A-Za-z0-9_]{2,20}", message = "Username format is invalid")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Size(max = 64, message = "Password is too long")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
