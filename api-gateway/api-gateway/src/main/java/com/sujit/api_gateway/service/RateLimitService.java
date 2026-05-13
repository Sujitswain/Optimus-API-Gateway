package com.sujit.api_gateway.service;

import com.sujit.api_gateway.properties.GatewayProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final GatewayProperties gatewayProperties;

    public RateLimitService(StringRedisTemplate redisTemplate, GatewayProperties gatewayProperties) {
        this.redisTemplate = redisTemplate;
        this.gatewayProperties = gatewayProperties;
    }

    public boolean isAllowed(String userId, String path, String role) {
        if (userId == null) {
            return false;
        }

        int limit = "PREMIUM".equalsIgnoreCase(role)
                ? gatewayProperties.getPremiumPerMinute()
                : gatewayProperties.getFreePerMinute();

        String key = "rate_limit:" + userId + ":" + normalizePath(path);

        // Using virtual threads - synchronous Redis operations
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        return count <= limit;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }
        return path.replaceAll("/+$", "");
    }
}

/*
// ORIGINAL REACTIVE IMPLEMENTATION (commented out for reference)
// import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
// import reactor.core.publisher.Mono;

// @Service
// public class RateLimitService {
//     private final ReactiveStringRedisTemplate redisTemplate;
//     private final GatewayProperties gatewayProperties;

//     public RateLimitService(ReactiveStringRedisTemplate redisTemplate, GatewayProperties gatewayProperties) {
//         this.redisTemplate = redisTemplate;
//         this.gatewayProperties = gatewayProperties;
//     }

//     public Mono<Boolean> isAllowed(String userId, String path, String role) {
//         if (userId == null) {
//             return Mono.just(false);
//         }

//         int limit = "PREMIUM".equalsIgnoreCase(role)
//                 ? gatewayProperties.getPremiumPerMinute()
//                 : gatewayProperties.getFreePerMinute();

//         String key = "rate_limit:" + userId + ":" + normalizePath(path);

//         return redisTemplate.opsForValue()
//                 .increment(key)
//                 .flatMap(count -> {
//                     if (count == 1L) {
//                         return redisTemplate.expire(key, Duration.ofMinutes(1))
//                                 .thenReturn(count);
//                     }
//                     return Mono.just(count);
//                 })
//                 .map(count -> count <= limit);
//     }
// }
*/
