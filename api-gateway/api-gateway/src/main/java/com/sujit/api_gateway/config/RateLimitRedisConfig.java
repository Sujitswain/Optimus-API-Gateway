package com.sujit.api_gateway.config;

import com.sujit.api_gateway.service.RateLimitPolicyRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RateLimitRedisConfig {

    public static final String RATE_LIMIT_POLICY_CHANNEL = "gateway.rate-limit.policy.updated";

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // .enableUnsafeDefaultTyping() includes the specific package path
        // (like com.sujit.api_gateway.dto.RateLimitPolicy) inside the JSON data,
        // every application that reads or writes to this key must have that exact
        // same class in the exact same package path.
        // else can use .typePropertyName("@type") and in the class use @JsonTypeName("RateLimitPolicy")
        RedisSerializer<Object> jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        // A check that all connection credentials and settings
        // initialize before marking the template as ready.
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RateLimitPolicyRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(RATE_LIMIT_POLICY_CHANNEL));
        return container;
    }
}
