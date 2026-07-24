package com.order.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AvatarDto {

    @NotBlank(message = "Avatar must not be blank")
    @Size(max = 512, message = "Avatar URL is too long")
    @Pattern(
            regexp = "^(https://[^\\s]+|/profile/[^\\s]*)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Avatar URL is invalid")
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar == null ? null : avatar.trim();
    }
}
