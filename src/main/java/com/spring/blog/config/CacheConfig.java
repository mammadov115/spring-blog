package com.spring.blog.config;

import com.spring.blog.dto.PostResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.type.TypeFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {

        // "post" cache: tək obyekt, defaultTyping-li generic serializer problemsizdir
        GenericJacksonJsonRedisSerializer genericSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build())
                .build();

        // "similar" cache: List<PostResponse> — konkret tip, polymorphic typing YOX (bug-a səbəb olan budur)
        JacksonJsonRedisSerializer<List<PostResponse>> similarSerializer =
                new JacksonJsonRedisSerializer<>(
                        TypeFactory.createDefaultInstance()
                                .constructCollectionType(List.class, PostResponse.class));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(genericSerializer));

        RedisCacheConfiguration similarConfig = defaultConfig
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(similarSerializer));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of("similar", similarConfig))
                .build();
    }
}
