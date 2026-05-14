package com.sujit.api_gateway.config;

import com.sujit.api_gateway.filter.JwtRateLimitFilter;
import com.sujit.api_gateway.properties.GatewayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewaySecurityConfig {

    private final JwtRateLimitFilter jwtRateLimitFilter;

    public GatewaySecurityConfig(JwtRateLimitFilter jwtRateLimitFilter) {
        this.jwtRateLimitFilter = jwtRateLimitFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**", "/actuator/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtRateLimitFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }
}

/*
// SERVLET-BASED IMPLEMENTATION (commented out for reference)
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration
// @EnableWebSecurity
// @EnableConfigurationProperties(GatewayProperties.class)
// public class GatewaySecurityConfig {
//
//     private final JwtRateLimitFilter jwtRateLimitFilter;
//
//     public GatewaySecurityConfig(JwtRateLimitFilter jwtRateLimitFilter) {
//         this.jwtRateLimitFilter = jwtRateLimitFilter;
//     }
//
//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http
//                 .csrf(csrf -> csrf.disable())
//                 .authorizeHttpRequests(authz -> authz
//                         .requestMatchers("/api/auth/**", "/actuator/**").permitAll()
//                         .anyRequest().authenticated()
//                 )
//                 .addFilterBefore(jwtRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
//                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//
//         return http.build();
//     }
//
//     @Bean
//     public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
//         return new StringRedisTemplate(factory);
//     }
// }
*/
