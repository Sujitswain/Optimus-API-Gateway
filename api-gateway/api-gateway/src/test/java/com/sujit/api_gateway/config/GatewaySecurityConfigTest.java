package com.sujit.api_gateway.config;

import com.sujit.api_gateway.filter.JwtRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class GatewaySecurityConfigTest {

    @Test
    void shouldBuildSecurityFilterChain() {
        JwtRateLimitFilter jwtRateLimitFilter = mock(JwtRateLimitFilter.class);
        GatewaySecurityConfig config = new GatewaySecurityConfig(jwtRateLimitFilter);

        ServerHttpSecurity http = ServerHttpSecurity.http();
        SecurityWebFilterChain filterChain = config.securityWebFilterChain(http);

        assertNotNull(filterChain);
    }
}
