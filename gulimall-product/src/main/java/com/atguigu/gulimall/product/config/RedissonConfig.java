package com.atguigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        //设置单节点模式，设置redis地址。ssl安全连接redission://192.168.56.10:6379
        config.useSingleServer().setAddress("redis://192.168.56.10:6379");
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}