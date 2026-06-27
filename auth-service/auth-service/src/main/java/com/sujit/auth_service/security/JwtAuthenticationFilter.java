package com.sujit.auth_service.security;

import com.sujit.auth_service.constant.AuthServiceConstants;
import com.sujit.auth_service.service.JwtBlacklistService;
import com.sujit.auth_service.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("Processing authentication for request: {}", request.getRequestURI());
        String header = request.getHeader(AuthServiceConstants.AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(AuthServiceConstants.BEARER_PREFIX)) {
            String token = header.substring(AuthServiceConstants.BEARER_PREFIX.length());
            try {
                if (jwtBlacklistService.isBlacklisted(token)) {
                    sendErrorResponse(response, AuthServiceConstants.INVALID_TOKEN_ERROR, "JWT token has been revoked");
                    return;
                }
                jwtService.extractAllClaims(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    String username = jwtService.extractUsername(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("Successfully authenticated user: {} for request: {}", username, request.getRequestURI());
                }
            } catch (ExpiredJwtException ex) {
                log.warn("Expired JWT token for request: {}", request.getRequestURI());
                sendErrorResponse(response, AuthServiceConstants.TOKEN_EXPIRED_ERROR, AuthServiceConstants.TOKEN_EXPIRED_MESSAGE);
                return;
            } catch (JwtException ex) {
                log.warn("Invalid JWT token for request: {}", request.getRequestURI());
                sendErrorResponse(response, AuthServiceConstants.INVALID_TOKEN_ERROR, AuthServiceConstants.TOKEN_INVALID_MESSAGE);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\", \"message\": \"%s\"}", error, message));
    }
}
