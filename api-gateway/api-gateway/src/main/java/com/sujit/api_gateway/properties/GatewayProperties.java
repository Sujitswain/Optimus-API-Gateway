package com.sujit.api_gateway.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@RefreshScope
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<String> publicPaths = new ArrayList<>();
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @Getter
    @Setter
    public static class RateLimitProperties {
        private int freePerMinute = 100;
        private int premiumPerMinute = 1000;
    }
}
