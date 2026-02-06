package com.zrp.mockpay.dbcore.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // Redis 주소 연결 (redis://호스트:포트)
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort);
        
        return Redisson.create(config);
    }
}