package com.sujit.api_gateway.service;

import com.sujit.api_gateway.model.RateLimitPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    private ReactiveStringRedisTemplate redisTemplate;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);

        rateLimitService = new RateLimitService(redisTemplate);
        RateLimitPolicyCache.setCurrent(new RateLimitPolicy(100, 1000, 20, 100));
    }

    @Test
    void shouldAllowAdminRequestWithoutRateLimitCheck() {
        Mono<Boolean> result = rateLimitService.isAllowed("admin-user", "/products", "ADMIN", "PREMIUM");

        assertTrue(result.block());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldAllowWhenTokenBucketReturnsAllowed() {
        when(redisTemplate.execute(any(), any(), anyString(), anyString()))
                .thenReturn(Flux.just(1L));

        Mono<Boolean> result = rateLimitService.isAllowed("user-1", "/orders", "USER", "FREE");

        assertTrue(result.block());
    }

    @Test
    void shouldBlockWhenTokenBucketReturnsDenied() {
        when(redisTemplate.execute(any(), any(), anyString(), anyString()))
                .thenReturn(Flux.just(0L));

        Mono<Boolean> result = rateLimitService.isAllowed("user-1", "/orders", "USER", "FREE");

        assertFalse(result.block());
    }
}
