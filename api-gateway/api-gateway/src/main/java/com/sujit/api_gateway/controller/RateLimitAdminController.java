package com.sujit.api_gateway.controller;

import com.sujit.api_gateway.config.RateLimitRedisConfig;
import com.sujit.api_gateway.model.RateLimitPolicy;
import com.sujit.api_gateway.service.RateLimitPolicyCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class RateLimitAdminController {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitAdminController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<RateLimitPolicy> getRateLimit() {
        return ResponseEntity.ok(RateLimitPolicyCache.getCurrent());
    }

    @PostMapping("/rate-limit")
    public ResponseEntity<RateLimitPolicy> updateRateLimit(@RequestBody RateLimitPolicy request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getFreePerMinute() < 1 || request.getPremiumPerMinute() < 1
            || request.getFreeCapacity() < 1 || request.getPremiumCapacity() < 1) {
            return ResponseEntity.badRequest().build();
        }

        RateLimitPolicyCache.setCurrent(request);
        redisTemplate.convertAndSend(RateLimitRedisConfig.RATE_LIMIT_POLICY_CHANNEL, request);
        return ResponseEntity.ok(request);
    }
}
