package com.sujit.api_gateway.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateLimitPolicy {
    private int freePerMinute;
    private int premiumPerMinute;
    private int freeCapacity;
    private int premiumCapacity;

    public RateLimitPolicy() {
        this(100, 1000, 20, 100);
    }

    public RateLimitPolicy(int freePerMinute, int premiumPerMinute) {
        this(freePerMinute, premiumPerMinute, 20, 100);
    }

    public RateLimitPolicy(int freePerMinute, int premiumPerMinute,
                           int freeCapacity, int premiumCapacity) {
        this.freePerMinute = freePerMinute;
        this.premiumPerMinute = premiumPerMinute;
        this.freeCapacity = freeCapacity;
        this.premiumCapacity = premiumCapacity;
    }
}
