package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public class RegisterDto {

    @NotBlank(message = "Username must not be blank")
    @Pattern(regexp = "[A-Za-z0-9_]{2,20}", message = "Username format is invalid")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 64, message = "Password length must be between 8 and 64 characters")
    private String password;

    @NotBlank(message = "Trade password must not be blank")
    @Size(min = 6, max = 18, message = "Trade password length must be between 6 and 18 characters")
    private String tradePassword;

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "\\+[1-9]\\d{7,14}", message = "Phone number must use E.164 format")
    private String phoneNumber;

    // 0: male, 1: female, 2: unknown
    @NotBlank(message = "Sex must not be blank")
    @Pattern(regexp = "[012]", message = "Sex must be 0, 1 or 2")
    private String gender;

    @NotBlank(message = "Invite code must not be blank")
    @Pattern(regexp = "[A-Z0-9]{1,32}", message = "Invite code format is invalid")
    private String inviteCode;

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

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender == null ? null : gender.trim();
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode == null ? null : inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
