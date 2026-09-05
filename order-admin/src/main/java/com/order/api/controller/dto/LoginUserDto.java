package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginUserDto {
    @NotBlank(message = "Username must not be blank")
    // Login must also accept existing usernames created by administrators or legacy systems.
    @Size(max = 100, message = "Username is too long")
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
