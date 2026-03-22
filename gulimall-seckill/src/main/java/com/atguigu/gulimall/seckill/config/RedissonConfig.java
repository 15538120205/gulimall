package com.atguigu.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类：创建RedissonClient Bean，供Spring容器注入
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown") // 容器销毁时关闭RedissonClient
    public RedissonClient redissonClient() {
        // 1. 创建Redisson配置对象
        Config config = new Config();
        
        // 2. 配置Redis连接（替换为你的Redis地址和密码）
        // 单机版Redis（谷粒商城本地测试常用）
        config.useSingleServer()
              .setAddress("redis://192.168.56.10:6379") // Redis地址+端口
              .setPassword(null) // Redis密码（没有则设为null）
              .setDatabase(0);   // 使用的Redis数据库（默认0）
        
        // 若用Redis集群/哨兵，替换为对应配置（示例）：
        // config.useClusterServers()
        //       .addNodeAddress("redis://192.168.56.10:6379", "redis://192.168.56.11:6379");

        // 3. 创建RedissonClient实例并返回（Spring会管理这个Bean）
        return Redisson.create(config);
    }
}