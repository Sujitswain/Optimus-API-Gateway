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

    @Value("${jwt.refresh-token-replay-grace-ms:30000}")
    private long replayGraceMs;

    private final StringRedisTemplate redisTemplate;
    private final UserService userService;
    private String refreshTokenPrefix;
    private String refreshTokenReplayPrefix;

    @PostConstruct
    private void init() {
        refreshTokenPrefix = "refresh:token:";
        refreshTokenReplayPrefix = "refresh:token:replay:";
    }

    public String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        String key = refreshTokenPrefix + token;

        // A refresh token is rotated on use to reduce replay risk.
        // We keep the value in Redis only for the configured lifetime.
        // A browser tab that reuses the same old refresh token within a short grace window
        // can still be tolerated for a few seconds before the token is treated as fully consumed.
        redisTemplate.opsForValue().set(key, String.valueOf(user.getId()), Duration.ofMillis(refreshTokenExpirationMs));
        log.debug("Created refresh token {} for user {}", token, user.getId());
        return token;
    }

    public User resolveUserFromRefreshToken(String token) {
        String activeKey = refreshTokenPrefix + token;
        String userIdValue = redisTemplate.opsForValue().get(activeKey);

        if (userIdValue != null) {
            return resolveUserByUserId(userIdValue);
        }

        // Grace window: a recently rotated refresh token may still be retried by another tab
        // for a short period. This prevents a false logout during the replay race window.
        String replayKey = refreshTokenReplayPrefix + token;
        String replayUserIdValue = redisTemplate.opsForValue().get(replayKey);
        if (replayUserIdValue != null) {
            return resolveUserByUserId(replayUserIdValue);
        }

        throw new ResourceNotFoundException("refresh token not found or expired");
    }

    public void rotateToken(String oldToken, User user) {
        if (oldToken == null || oldToken.isBlank()) {
            return;
        }

        // Store the old token in the replay grace bucket for a few seconds.
        // This allows a duplicate browser-tab request to succeed once before the old token is fully discarded.
        String replayKey = refreshTokenReplayPrefix + oldToken;
        redisTemplate.opsForValue().set(replayKey, String.valueOf(user.getId()), Duration.ofMillis(replayGraceMs));

        // Remove the active token; a new one will replace it below.
        redisTemplate.delete(refreshTokenPrefix + oldToken);
    }

    public void deleteByToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.delete(refreshTokenPrefix + token);
        redisTemplate.delete(refreshTokenReplayPrefix + token);
    }

    private User resolveUserByUserId(String userIdValue) {
        try {
            long userId = Long.parseLong(userIdValue);
            return userService.getById(userId);
        } catch (NumberFormatException ex) {
            throw new ResourceNotFoundException("invalid refresh token payload");
        }
    }
}
