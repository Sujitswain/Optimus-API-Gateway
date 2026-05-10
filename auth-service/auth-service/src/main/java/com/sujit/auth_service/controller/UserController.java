package com.sujit.auth_service.controller;

import com.sujit.auth_service.dto.UserProfileResponse;
import com.sujit.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = Objects.requireNonNullElse(authentication.getName(), "anonymous");

        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(new UserProfileResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole(),
                        user.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
