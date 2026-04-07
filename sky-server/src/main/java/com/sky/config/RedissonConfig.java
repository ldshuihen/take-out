package com.sky.config;

/**
 * 文件名：RedissonConfig
 * 作者：24141
 * 创建日期：2026/4/7
 * 描述：
 */
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单机 Redis，集群用 useClusterServers()
        config.useSingleServer().setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }
}