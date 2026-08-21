package com.sujit.api_gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sujit.api_gateway.model.RateLimitPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicyRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RateLimitPolicyRedisSubscriber.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            RateLimitPolicy policy = objectMapper.readValue(message.getBody(), RateLimitPolicy.class);
            RateLimitPolicyCache.setCurrent(policy);
                log.info("Rate limit updated via Redis: free={}/{} premium={}/{}",
                    policy.getFreePerMinute(), policy.getFreeCapacity(),
                    policy.getPremiumPerMinute(), policy.getPremiumCapacity());
        } catch (Exception e) {
            log.warn("Failed to update rate limit from Redis message: {}", e.getMessage());
        }
    }
}
