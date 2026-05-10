package com.sujit.auth_service.dto;

public record UserDto(
        Long id,
        String username,
        String email,
        String role
) {
}
