package com.sujit.auth_service.service;

import com.sujit.auth_service.entity.RefreshToken;
import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.exception.BadRequestException;
import com.sujit.auth_service.exception.ResourceNotFoundException;
import com.sujit.auth_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiry(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiry().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("refresh token expired");
        }
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("refresh token not found"));
    }

    public void deleteToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }

    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
