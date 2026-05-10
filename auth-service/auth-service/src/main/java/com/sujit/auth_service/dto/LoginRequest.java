package com.sujit.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        String email,
        String username,
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 255)
        String password
) {
}
