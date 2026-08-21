package com.sujit.api_gateway.controller;

import com.sujit.api_gateway.config.RateLimitRedisConfig;
import com.sujit.api_gateway.model.RateLimitPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAdminControllerIT {

    @Test
    void shouldUpdateRateLimitAndPublishRedisEvent() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RateLimitAdminController controller = new RateLimitAdminController(redisTemplate);

        RateLimitPolicy request = new RateLimitPolicy(250, 1500, 40, 200);
        when(redisTemplate.convertAndSend(eq(RateLimitRedisConfig.RATE_LIMIT_POLICY_CHANNEL), any(RateLimitPolicy.class)))
                .thenReturn(1L);

        ResponseEntity<RateLimitPolicy> response = controller.updateRateLimit(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(250, response.getBody().getFreePerMinute());
        assertEquals(1500, response.getBody().getPremiumPerMinute());
        assertEquals(40, response.getBody().getFreeCapacity());
        assertEquals(200, response.getBody().getPremiumCapacity());
        verify(redisTemplate).convertAndSend(eq(RateLimitRedisConfig.RATE_LIMIT_POLICY_CHANNEL), any(RateLimitPolicy.class));
    }
}
