package com.order.api.service;

import com.order.api.controller.dto.UserProfileResponse;

public record UserLoginResult(String token, UserProfileResponse user) {
}
