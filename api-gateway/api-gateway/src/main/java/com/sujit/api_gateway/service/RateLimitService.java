package com.sujit.api_gateway.service;

import com.sujit.api_gateway.enums.Role;
import com.sujit.api_gateway.enums.Tier;
import com.sujit.api_gateway.model.RateLimitPolicy;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RateLimitService {

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local time = redis.call('TIME')
            local now = (tonumber(time[1]) * 1000) + math.floor(tonumber(time[2]) / 1000)
            local capacity = tonumber(ARGV[1])
            local refillPerSecond = tonumber(ARGV[2])
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local lastRefill = tonumber(redis.call('HGET', KEYS[1], 'lastRefillMillis'))

            if tokens == nil or lastRefill == nil then
                tokens = capacity
                lastRefill = now
            else
                local elapsedSeconds = math.max(0, (now - lastRefill) / 1000)
                tokens = math.min(capacity, tokens + (elapsedSeconds * refillPerSecond))
                lastRefill = now
            end

            local allowed = 0
            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            end

            redis.call('HSET', KEYS[1], 'tokens', tokens, 'lastRefillMillis', lastRefill)
            redis.call('PEXPIRE', KEYS[1], 60000)
            return allowed
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isAllowed(String userId, String path, String role, String tier) {
        if (userId == null) {
            return Mono.just(false);
        }

        if (Role.ADMIN.name().equalsIgnoreCase(role)) {
            return Mono.just(true);
        }

        RateLimitPolicy current = RateLimitPolicyCache.getCurrent();
        int refillPerMinute = Tier.PREMIUM.name().equalsIgnoreCase(tier)
                ? current.getPremiumPerMinute()
                : current.getFreePerMinute();
        int capacity = Tier.PREMIUM.name().equalsIgnoreCase(tier)
            ? current.getPremiumCapacity()
            : current.getFreeCapacity();

        String key = "rate_limit:" + userId + ":" + normalizePath(path);
        String refillPerSecond = Double.toString(refillPerMinute / 60.0);

        return redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(key),
                Integer.toString(capacity),
                refillPerSecond)
            .next()
            .map(result -> result == 1L);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }
        return path.replaceAll("/+$", "");
    }
}

/*
// REDIS FIXED-WINDOW IMPLEMENTATION (kept for reference)
// int limit = Tier.PREMIUM.name().equalsIgnoreCase(tier)
//         ? current.getPremiumPerMinute()
//         : current.getFreePerMinute();
// String key = "rate_limit:" + userId + ":" + normalizePath(path);
// return redisTemplate.opsForValue()
//         .increment(key)
//         .flatMap(count -> {
//             if (count == 1L) {
//                 return redisTemplate.expire(key, Duration.ofMinutes(1)).thenReturn(count);
//             }
//             return Mono.just(count);
//         })
//         .map(count -> count <= limit);

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
