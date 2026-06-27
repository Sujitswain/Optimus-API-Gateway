package com.sujit.auth_service.service;

import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenService {

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private final StringRedisTemplate redisTemplate;
    private final UserService userService;
    private String refreshTokenPrefix;

    @PostConstruct
    private void init() {
        refreshTokenPrefix = "refresh:token:";
    }

    public String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        String key = refreshTokenPrefix + token;

        redisTemplate.opsForValue().set(key, String.valueOf(user.getId()), Duration.ofMillis(refreshTokenExpirationMs));
        log.debug("Created refresh token {} for user {}", token, user.getId());
        return token;
    }

    public User resolveUserFromRefreshToken(String token) {
        String key = refreshTokenPrefix + token;
        String userIdValue = redisTemplate.opsForValue().get(key);

        if (userIdValue == null) {
            throw new ResourceNotFoundException("refresh token not found or expired");
        }

        try {
            long userId = Long.parseLong(userIdValue);
            return userService.getById(userId);
        } catch (NumberFormatException ex) {
            throw new ResourceNotFoundException("invalid refresh token payload");
        }
    }

    public void deleteByToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete(refreshTokenPrefix + token);
    }
}
