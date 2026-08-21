package com.sujit.api_gateway.service;

import com.sujit.api_gateway.model.RateLimitPolicy;
import com.sujit.api_gateway.properties.GatewayProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class RateLimitPolicyCache {

    private static final AtomicReference<RateLimitPolicy> CURRENT = new AtomicReference<>();

    private final GatewayProperties gatewayProperties;

    public RateLimitPolicyCache(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    @PostConstruct
    public void init() {
        CURRENT.set(new RateLimitPolicy(
                gatewayProperties.getRateLimit().getFreePerMinute(),
            gatewayProperties.getRateLimit().getPremiumPerMinute(),
            gatewayProperties.getRateLimit().getFreeCapacity(),
            gatewayProperties.getRateLimit().getPremiumCapacity()
        ));
    }

    public static RateLimitPolicy getCurrent() {
        RateLimitPolicy current = CURRENT.get();
        if (current == null) {
            return new RateLimitPolicy(100, 1000, 20, 100);
        }
        return current;
    }

    public static void setCurrent(RateLimitPolicy policy) {
        if (policy != null) {
            CURRENT.set(policy);
        }
    }
}
