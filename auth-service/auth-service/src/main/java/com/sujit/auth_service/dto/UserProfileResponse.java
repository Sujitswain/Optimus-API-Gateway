package com.sujit.auth_service.dto;

import com.sujit.auth_service.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private Role role;
    private Instant createdAt;
}
