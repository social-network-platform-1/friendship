package com.social.friendship.config;

import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class RedisConfig {

//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory("localhost", 6379);
//    }

    @Bean("statusRedis")
    public RedisTemplate<String, FriendshipStatus> statusRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, FriendshipStatus> template =
                new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(
                new StringRedisSerializer()
        );

        template.setValueSerializer(
                new JacksonJsonRedisSerializer<>(
                        FriendshipStatus.class
                )
        );

        return template;
    }
    @Bean("responseRedis")
    public RedisTemplate<String, List<FriendResponse>> responseRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, List<FriendResponse>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(RedisSerializer.string());

        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, FriendResponse.class);

        JacksonJsonRedisSerializer<List<FriendResponse>> serializer =
                new JacksonJsonRedisSerializer<>(objectMapper, listType);

        template.setValueSerializer(serializer);

        return template;
    }}
