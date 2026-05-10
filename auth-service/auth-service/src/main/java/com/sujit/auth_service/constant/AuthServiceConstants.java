package com.sujit.auth_service.constant;

public final class AuthServiceConstants {

    private AuthServiceConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_TYPE = "Bearer";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ROLE_CLAIM = "role";
    public static final String USER_ID_CLAIM = "userId";
    public static final String TOKEN_EXPIRED_ERROR = "TOKEN_EXPIRED";
    public static final String INVALID_TOKEN_ERROR = "INVALID_TOKEN";
    public static final String TOKEN_EXPIRED_MESSAGE = "Access token expired";
    public static final String TOKEN_INVALID_MESSAGE = "Invalid token";
}
