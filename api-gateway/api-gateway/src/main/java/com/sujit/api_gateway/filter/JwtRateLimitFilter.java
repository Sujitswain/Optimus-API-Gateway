package com.sujit.api_gateway.filter;

import com.sujit.api_gateway.properties.GatewayProperties;
import com.sujit.api_gateway.service.JwtBlacklistService;
import com.sujit.api_gateway.service.JwtService;
import com.sujit.api_gateway.service.RateLimitService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.sujit.api_gateway.constants.GatewayApplicationConstants.BEARER_PREFIX;

@Component
public class JwtRateLimitFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtRateLimitFilter.class);

    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;
    private final RateLimitService rateLimitService;
    private final GatewayProperties gatewayProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtRateLimitFilter(JwtService jwtService,
                              JwtBlacklistService jwtBlacklistService,
                              RateLimitService rateLimitService,
                              GatewayProperties gatewayProperties) {
        this.jwtService = jwtService;
        this.jwtBlacklistService = jwtBlacklistService;
        this.rateLimitService = rateLimitService;
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        return resolveToken(exchange.getRequest())
                .flatMap(token -> validateAndForward(exchange, chain, token))
                .switchIfEmpty(unauthorized(exchange))
                .onErrorResume(throwable -> {
                    log.warn("JWT validation failed for request {}: {}", path, throwable.getMessage());
                    return unauthorized(exchange);
                });
    }

    private Mono<Void> validateAndForward(ServerWebExchange exchange, WebFilterChain chain, String token) {
        Claims claims = jwtService.validateToken(token);
        return jwtBlacklistService.isBlacklisted(token)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("JWT token is blacklisted for path {}", exchange.getRequest().getPath().value());
                        return unauthorized(exchange);
                    }

                    String userId = jwtService.resolveUserId(claims);
                    String role = jwtService.resolveRole(claims);
                    String path = exchange.getRequest().getPath().value();

                    return rateLimitService.isAllowed(userId, path, role)
                            .flatMap(allowed -> allowed
                                    ? authenticateAndContinue(exchange, chain, claims, token)
                                    : tooManyRequests(exchange));
                });
    }

    private Mono<Void> authenticateAndContinue(ServerWebExchange exchange,
                                               WebFilterChain chain,
                                               Claims claims,
                                               String token) {
        Authentication authentication = jwtService.createAuthentication(claims, token);
        exchange.getAttributes().put("jwtClaims", claims);
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private boolean isPublicPath(String path) {
        return gatewayProperties.getPublicPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<String> resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }
        return Mono.just(header.substring(BEARER_PREFIX.length()).trim());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
}

/*
// SERVLET-BASED IMPLEMENTATION
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.web.filter.OncePerRequestFilter;
// import java.io.IOException;

// @Component
// public class JwtRateLimitFilter extends OncePerRequestFilter implements Ordered {
//     private static final Logger log = LoggerFactory.getLogger(JwtRateLimitFilter.class);
//     private static final String BEARER_PREFIX = "Bearer ";
//
//     private final JwtService jwtService;
//     private final JwtBlacklistService jwtBlacklistService;
//     private final RateLimitService rateLimitService;
//     private final GatewayProperties gatewayProperties;
//     private final AntPathMatcher pathMatcher = new AntPathMatcher();
//
//     public JwtRateLimitFilter(JwtService jwtService,
//                               JwtBlacklistService jwtBlacklistService,
//                               RateLimitService rateLimitService,
//                               GatewayProperties gatewayProperties) {
//         this.jwtService = jwtService;
//         this.jwtBlacklistService = jwtBlacklistService;
//         this.rateLimitService = rateLimitService;
//         this.gatewayProperties = gatewayProperties;
//     }
//
//     @Override
//     public int getOrder() {
//         return Ordered.HIGHEST_PRECEDENCE;
//     }
//
//     @Override
//     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//             throws ServletException, IOException {
//
//         String path = request.getRequestURI();
//         if (isPublicPath(path)) {
//             filterChain.doFilter(request, response);
//             return;
//         }
//
//         String token = resolveToken(request);
//         if (token == null) {
//             unauthorized(response);
//             return;
//         }
//
//         try {
//             validateAndForward(request, response, filterChain, token);
//         } catch (Exception e) {
//             log.warn("JWT validation failed for request {}: {}", path, e.getMessage());
//             unauthorized(response);
//         }
//     }
//
//     private void validateAndForward(HttpServletRequest request, HttpServletResponse response,
//                                     FilterChain filterChain, String token) throws IOException, ServletException {
//
//         Claims claims = jwtService.validateToken(token);
//
//         if (jwtBlacklistService.isBlacklisted(token)) {
//             log.warn("JWT token is blacklisted for path {}", request.getRequestURI());
//             unauthorized(response);
//             return;
//         }
//
//         String userId = jwtService.resolveUserId(claims);
//         String role = jwtService.resolveRole(claims);
//         String path = request.getRequestURI();
//
//         if (!rateLimitService.isAllowed(userId, path, role)) {
//             tooManyRequests(response);
//             return;
//         }
//
//         Authentication authentication = jwtService.createAuthentication(claims, token);
//         SecurityContextHolder.getContext().setAuthentication(authentication);
//         request.setAttribute("jwtClaims", claims);
//
//         filterChain.doFilter(request, response);
//     }
//
//     private boolean isPublicPath(String path) {
//         return gatewayProperties.getPublicPaths().stream()
//                 .anyMatch(pattern -> pathMatcher.match(pattern, path));
//     }
//
//     private String resolveToken(HttpServletRequest request) {
//         String header = request.getHeader(HttpHeaders.AUTHORIZATION);
//         if (header == null || !header.startsWith(BEARER_PREFIX)) {
//             return null;
//         }
//         return header.substring(BEARER_PREFIX.length()).trim();
//     }
//
//     private void unauthorized(HttpServletResponse response) throws IOException {
//         response.setStatus(HttpStatus.UNAUTHORIZED.value());
//     }
//
//     private void tooManyRequests(HttpServletResponse response) throws IOException {
//         response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
//     }
// }
*/
