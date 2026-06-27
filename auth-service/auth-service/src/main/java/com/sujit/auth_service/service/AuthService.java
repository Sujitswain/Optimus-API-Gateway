package com.sujit.auth_service.service;

import com.sujit.auth_service.dto.AuthResponse;
import com.sujit.auth_service.dto.LoginRequest;
import com.sujit.auth_service.dto.RegisterRequest;
import com.sujit.auth_service.dto.UserDto;
import com.sujit.auth_service.entity.User;
import com.sujit.auth_service.exception.BadRequestException;
import com.sujit.auth_service.service.JwtBlacklistService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import static com.sujit.auth_service.constant.AuthServiceConstants.TOKEN_TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final JwtBlacklistService jwtBlacklistService;

    /**
     * Authenticates a user based on the provided login request, 
     * which can contain either an email or username along with a password.
     * @param request
     * @return
     */
    public AuthResponse login(LoginRequest request) {
        String credential = chooseLoginIdentifier(request);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(credential, request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for identifier: {}", credential);
            throw new BadRequestException("invalid email/username or password");
        }

        User user = userService.findByEmail(credential)
                .or(() -> userService.findByUsername(credential))
                .orElseThrow(() -> new BadRequestException("invalid email/username or password"));

        String refreshToken = redisRefreshTokenService.createRefreshToken(user);
        String accessToken = jwtService.generateAccessToken(user);
        log.info("User {} logged in successfully", user.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtService.getAccessTokenExpirationMs() / 1000,
                new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name())
        );
    }

    /**
     * Registers a new user and returns an authentication response containing 
     * the access token, refresh token, and user details.
     * @param request
     * @return
     */
    public AuthResponse register(RegisterRequest request) {
        User savedUser = userService.register(request);
        String refreshToken = redisRefreshTokenService.createRefreshToken(savedUser);
        String accessToken = jwtService.generateAccessToken(savedUser);
        log.info("User {} registered successfully", savedUser.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtService.getAccessTokenExpirationMs() / 1000,
                new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole().name())
        );
    }

    /**
     * Refreshes the access token using the provided refresh token. 
     * It validates the refresh token, checks for expiration, and 
     * generates a new access token if valid. 
     * The old refresh token is deleted and a new one is created.
     * @param refreshTokenValue
     * @return
     */
    public AuthResponse refreshToken(String refreshTokenValue) {
        User user = redisRefreshTokenService.resolveUserFromRefreshToken(refreshTokenValue);
        redisRefreshTokenService.deleteByToken(refreshTokenValue);

        String newRefreshToken = redisRefreshTokenService.createRefreshToken(user);
        String accessToken = jwtService.generateAccessToken(user);
        log.info("Access token refreshed for user: {}", user.getUsername());

        return new AuthResponse(
                accessToken,
                newRefreshToken,
                TOKEN_TYPE,
                jwtService.getAccessTokenExpirationMs() / 1000,
                null
        );
    }

    public void logout(String refreshTokenValue) {
        redisRefreshTokenService.deleteByToken(refreshTokenValue);
        log.info("User logged out and refresh token invalidated: {}", refreshTokenValue);
    }

    public void blacklistAccessToken(String accessToken) {
        try {
            long secondsLeft = jwtService.getTokenExpirationSeconds(accessToken);
            if (secondsLeft > 0) {
                jwtBlacklistService.blacklist(accessToken, java.time.Duration.ofSeconds(secondsLeft));
                log.info("Access token blacklisted for logout");
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid access token during logout, skipping blacklist", ex);
        }
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtService.getRefreshTokenExpirationMs() / 1000;
    }

    // Helper method to determine whether to use email or username for authentication
    private String chooseLoginIdentifier(LoginRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            return request.email().trim().toLowerCase();
        }
        if (request.username() != null && !request.username().isBlank()) {
            return request.username().trim();
        }
        throw new BadRequestException("email or username is required");
    }
}
