package com.sujit.api_gateway.service;

import com.sujit.api_gateway.enums.Role;
import com.sujit.api_gateway.properties.GatewayProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RateLimitService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final GatewayProperties gatewayProperties;

    public RateLimitService(ReactiveStringRedisTemplate redisTemplate, GatewayProperties gatewayProperties) {
        this.redisTemplate = redisTemplate;
        this.gatewayProperties = gatewayProperties;
    }

    public Mono<Boolean> isAllowed(String userId, String path, String role) {
        if (userId == null) {
            return Mono.just(false);
        }

        int limit = Role.PREMIUM.name().equalsIgnoreCase(role)
                ? gatewayProperties.getRateLimit().getPremiumPerMinute()
                : gatewayProperties.getRateLimit().getFreePerMinute();

        String key = "rate_limit:" + userId + ":" + normalizePath(path);

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    // Set expiration only on first increment to avoid resetting TTL on every request
                    // then after 1 minute, the key will expire and count will reset automatically
                    // so, you can make another 100 requests in the next minute without hitting the limit
                    if (count == 1L) {
                        return redisTemplate.expire(key, Duration.ofMinutes(1))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .map(count -> count <= limit);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }
        return path.replaceAll("/+$", "");
    }
}

/*
// SERVLET/THREAD-BASED IMPLEMENTATION
// import org.springframework.data.redis.core.StringRedisTemplate;
//
// @Service
// public class RateLimitService {
//     private final StringRedisTemplate redisTemplate;
//     private final GatewayProperties gatewayProperties;
//
//     public RateLimitService(StringRedisTemplate redisTemplate, GatewayProperties gatewayProperties) {
//         this.redisTemplate = redisTemplate;
//         this.gatewayProperties = gatewayProperties;
//     }
//
//     public boolean isAllowed(String userId, String path, String role) {
//         if (userId == null) {
//             return false;
//         }
//
//         int limit = "PREMIUM".equalsIgnoreCase(role)
//                 ? gatewayProperties.getPremiumPerMinute()
//                 : gatewayProperties.getFreePerMinute();
//
//         String key = "rate_limit:" + userId + ":" + normalizePath(path);
//
//         Long count = redisTemplate.opsForValue().increment(key);
//         if (count == 1L) {
//             redisTemplate.expire(key, Duration.ofMinutes(1));
//         }
//
//         return count <= limit;
//     }
// }
*/
