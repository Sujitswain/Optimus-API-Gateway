package com.sujit.api_gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

import static com.sujit.api_gateway.constants.GatewayApplicationConstants.ROLE_CLAIM;
import static com.sujit.api_gateway.constants.GatewayApplicationConstants.USER_ID_CLAIM;

@Service
@RefreshScope
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key signingKey;

    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String resolveUsername(Claims claims) {
        return claims.getSubject();
    }

    public String resolveRole(Claims claims) {
        Object roleValue = claims.get(ROLE_CLAIM);
        return roleValue != null ? roleValue.toString() : "anonymous";
    }

    public String resolveUserId(Claims claims) {
        Object idValue = claims.get(USER_ID_CLAIM);
        return idValue != null ? idValue.toString() : null;
    }

    public Authentication createAuthentication(Claims claims, String token) {
        String role = resolveRole(claims);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        return new UsernamePasswordAuthenticationToken(resolveUsername(claims), token, authorities);
    }
}
