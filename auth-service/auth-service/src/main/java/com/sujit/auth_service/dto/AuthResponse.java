package com.sujit.auth_service.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserDto user
) {
}
