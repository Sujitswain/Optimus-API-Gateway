package com.sujit.auth_service.service;

import com.sujit.auth_service.dto.AuthResponse;
import com.sujit.auth_service.dto.LoginRequest;
import com.sujit.auth_service.dto.RegisterRequest;
import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.enums.Role;
import com.sujit.auth_service.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RedisRefreshTokenService redisRefreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private String refreshTokenValue;

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

        loginRequest = new LoginRequest("test@example.com", "testuser", "password123");
        registerRequest = new RegisterRequest("newuser", "new@example.com", "password123");
        refreshTokenValue = "refresh-token-value";
    }

    @Test
    @DisplayName("Should login successfully with email")
    void testLoginWithEmailSuccess() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "password123"));
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(redisRefreshTokenService.createRefreshToken(testUser)).thenReturn(refreshTokenValue);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user()).isNotNull();
        assertThat(response.user().username()).isEqualTo("testuser");

        verify(authenticationManager).authenticate(any());
        verify(jwtService).generateAccessToken(testUser);
        verify(redisRefreshTokenService).createRefreshToken(testUser);
    }

    @Test
    @DisplayName("Should login successfully with username")
    void testLoginWithUsernameSuccess() {
        // Arrange
        LoginRequest usernameLogin = new LoginRequest(null, "testuser", "password123");
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("testuser", "password123"));
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(redisRefreshTokenService.createRefreshToken(testUser)).thenReturn(refreshTokenValue);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.login(usernameLogin);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.user().username()).isEqualTo("testuser");

        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Should throw exception on invalid credentials")
    void testLoginWithInvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new AuthenticationException("Invalid credentials") {});

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("invalid email/username or password");

        verify(redisRefreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Should throw exception when no email or username provided")
    void testLoginWithNoCredentials() {
        // Arrange
        LoginRequest emptyLogin = new LoginRequest("", "", "password123");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(emptyLogin))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("email or username is required");
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterSuccess() {
        // Arrange
        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .email("new@example.com")
                .password("encodedPassword")
                .role(Role.FREE)
                .createdAt(Instant.now())
                .build();

        when(userService.register(registerRequest)).thenReturn(newUser);
        when(redisRefreshTokenService.createRefreshToken(newUser)).thenReturn(refreshTokenValue);
        when(jwtService.generateAccessToken(newUser)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        assertThat(response.user().username()).isEqualTo("newuser");

        verify(userService).register(registerRequest);
        verify(redisRefreshTokenService).createRefreshToken(newUser);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshTokenSuccess() {
        // Arrange
        String refreshTokenValue = "refresh-token-value";
        when(redisRefreshTokenService.resolveUserFromRefreshToken(refreshTokenValue)).thenReturn(testUser);
        doNothing().when(redisRefreshTokenService).deleteByToken(refreshTokenValue);
        when(redisRefreshTokenService.createRefreshToken(testUser)).thenReturn("new-refresh-token");
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.refreshToken(refreshTokenValue);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.user()).isNull(); // User is null on refresh

        verify(redisRefreshTokenService).deleteByToken(refreshTokenValue);
        verify(redisRefreshTokenService).createRefreshToken(testUser);
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void testRefreshTokenInvalid() {
        // Arrange
        String invalidToken = "invalid-token";
        when(redisRefreshTokenService.resolveUserFromRefreshToken(invalidToken)).thenThrow(new RuntimeException("Token not found"));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should include user info in login response")
    void testLoginResponseIncludesUserInfo() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "password123"));
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(redisRefreshTokenService.createRefreshToken(testUser)).thenReturn(refreshTokenValue);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().username()).isEqualTo("testuser");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        assertThat(response.user().role()).isEqualTo("FREE");
    }
}
