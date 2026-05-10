package com.sujit.auth_service.service;

import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static com.sujit.auth_service.constant.AuthServiceConstants.ROLE_CLAIM;
import static com.sujit.auth_service.constant.AuthServiceConstants.USER_ID_CLAIM;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Unit Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private String jwtSecret;
    private long accessTokenExpirationMs;

    @BeforeEach
    void setUp() {
        jwtSecret = "asa@T#&@#^&7847587SAD23HSG&$#^%DH8496584";
        accessTokenExpirationMs = 900000;
        
        ReflectionTestUtils.setField(jwtService, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", accessTokenExpirationMs);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 2592000000L);
        
        jwtService.init();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .role(Role.FREE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token with correct claims")
    void testGenerateAccessToken() {
        // Act
        String token = jwtService.generateAccessToken(testUser);
        
        // Assert
        assertThat(token).isNotNull().isNotBlank();
        
        // Act
        Claims claims = jwtService.extractAllClaims(token);

        // Assert
        assertThat(claims.getSubject()).isEqualTo(testUser.getUsername());
        assertThat(claims.get(ROLE_CLAIM)).isEqualTo(Role.FREE.name());
        assertThat(claims.get(USER_ID_CLAIM)).isEqualTo(testUser.getId().intValue());
    }

    @Test
    @DisplayName("Should extract username from token")
    void testExtractUsername() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        
        // Act
        String username = jwtService.extractUsername(token);
        
        // Assert
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void testExtractExpiration() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        
        // Act
        Date expirationDate = jwtService.extractExpiration(token);
        
        // Assert
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate.getTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void testExtractAllClaims() {
        // Arrange
        String token = jwtService.generateAccessToken(testUser);
        
        // Act
        Claims claims = jwtService.extractAllClaims(token);
        
        // Assert
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUser.getUsername());
        assertThat(claims.get(ROLE_CLAIM)).isEqualTo(Role.FREE.name());
        assertThat(claims.get(USER_ID_CLAIM)).isEqualTo(testUser.getId().intValue());
    }

    @Test
    @DisplayName("Should return correct token expiration time")
    void testGetAccessTokenExpirationMs() {
        // Act
        long expirationMs = jwtService.getAccessTokenExpirationMs();
        
        // Assert
        assertThat(expirationMs).isEqualTo(accessTokenExpirationMs);
    }

    @Test
    @DisplayName("Should return correct refresh token expiration time")
    void testGetRefreshTokenExpirationMs() {
        // Act
        long expirationMs = jwtService.getRefreshTokenExpirationMs();
        
        // Assert
        assertThat(expirationMs).isEqualTo(2592000000L);
    }

    @Test
    @DisplayName("Should generate tokens with different content for different users")
    void testTokensAreDifferentForDifferentUsers() {
        // Arrange
        User anotherUser = User.builder()
                .id(2L)
                .username("anotheruser")
                .email("another@example.com")
                .password("password456")
                .role(Role.PREMIUM)
                .createdAt(Instant.now())
                .build();
        
        // Act
        String token1 = jwtService.generateAccessToken(testUser);
        String token2 = jwtService.generateAccessToken(anotherUser);
        
        // Assert
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractUsername(token1)).isEqualTo("testuser");
        assertThat(jwtService.extractUsername(token2)).isEqualTo("anotheruser");
    }

    @Test
    @DisplayName("Should throw exception when parsing invalid token")
    void testExtractClaimsWithInvalidToken() {
        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractAllClaims("invalid.token.here"))
                .isNotNull();
    }
}
