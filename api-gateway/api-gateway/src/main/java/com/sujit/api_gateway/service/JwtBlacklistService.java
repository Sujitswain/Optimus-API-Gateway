package com.sujit.api_gateway.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class JwtBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtBlacklistService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isBlacklisted(String token) {
        return redisTemplate.opsForValue()
                .get(BLACKLIST_PREFIX + token)
                .map(value -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> blacklist(String token, Duration ttl) {
        return redisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + token, "blacklisted", ttl);
    }
}

/*
// SERVLET/THREAD-BASED IMPLEMENTATION
// import org.springframework.data.redis.core.StringRedisTemplate;
//
// @Service
// public class JwtBlacklistService {
//
//     private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
//     private final StringRedisTemplate redisTemplate;
//
//     public JwtBlacklistService(StringRedisTemplate redisTemplate) {
//         this.redisTemplate = redisTemplate;
//     }
//
//     public boolean isBlacklisted(String token) {
//         return redisTemplate.opsForValue().get(BLACKLIST_PREFIX + token) != null;
//     }
//
//     public boolean blacklist(String token, Duration ttl) {
//         redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "blacklisted");
//         return redisTemplate.expire(BLACKLIST_PREFIX + token, ttl);
//     }
// }
*/
