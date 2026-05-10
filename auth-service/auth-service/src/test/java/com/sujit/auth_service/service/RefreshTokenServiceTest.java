package com.sujit.auth_service.service;

import com.sujit.auth_service.entity.RefreshToken;
import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.enums.Role;
import com.sujit.auth_service.exception.BadRequestException;
import com.sujit.auth_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Refresh Token Service Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.FREE)
                .createdAt(Instant.now())
                .build();

        testToken = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .user(testUser)
                .expiry(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();
    }

    @Test
    @DisplayName("Should create refresh token successfully")
    void testCreateRefreshTokenSuccess() {
        // Arrange
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testToken);

        // Act
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser);

        // Assert
        assertThat(createdToken).isNotNull();
        assertThat(createdToken.getUser()).isEqualTo(testUser);
        assertThat(createdToken.getToken()).isNotBlank();
        assertThat(createdToken.getExpiry()).isAfter(Instant.now());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should find refresh token by token string")
    void testFindByTokenSuccess() {
        // Arrange
        when(refreshTokenRepository.findByToken(testToken.getToken()))
                .thenReturn(Optional.of(testToken));

        // Act
        RefreshToken foundToken = refreshTokenService.findByToken(testToken.getToken());

        // Assert
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getToken()).isEqualTo(testToken.getToken());

        verify(refreshTokenRepository).findByToken(testToken.getToken());
    }

    @Test
    @DisplayName("Should throw exception when refresh token not found")
    void testFindByTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.findByToken("nonexistent"))
                .isInstanceOf(RuntimeException.class);

        verify(refreshTokenRepository).findByToken("nonexistent");
    }

    @Test
    @DisplayName("Should verify non-expired token successfully")
    void testVerifyExpirationNotExpired() {
        // Arrange
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .user(testUser)
                .expiry(Instant.now().plusSeconds(86400))
                .build();

        // // Act & Assert
        assertThatCode(() -> refreshTokenService.verifyExpiration(validToken))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void testVerifyExpirationExpired() {
        // Arrange
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .user(testUser)
                .expiry(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                .build();

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(expiredToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("refresh token expired");
    }

    @Test
    @DisplayName("Should delete refresh token successfully")
    void testDeleteTokenSuccess() {
        // Act
        refreshTokenService.deleteToken(testToken);

        // Assert
        verify(refreshTokenRepository).delete(testToken);
    }

    @Test
    @DisplayName("Should generate unique tokens for different users")
    void testUniqueTokensForDifferentUsers() {
        // Arrange
        User anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .email("another@example.com")
                .password("encodedPassword")
                .role(Role.PREMIUM)
                .createdAt(Instant.now())
                .build();

        RefreshToken token1 = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .user(testUser)
                .expiry(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .id(2L)
                .token(UUID.randomUUID().toString())
                .user(anotherUser)
                .expiry(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .build();

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(token1)
                .thenReturn(token2);

        // Act
        RefreshToken created1 = refreshTokenService.createRefreshToken(testUser);
        RefreshToken created2 = refreshTokenService.createRefreshToken(anotherUser);

        // Assert
        assertThat(created1.getToken()).isNotEqualTo(created2.getToken());
        assertThat(created1.getUser().getId()).isNotEqualTo(created2.getUser().getId());
    }
}
