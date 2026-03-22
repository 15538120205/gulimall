package com.atguigu.gulimall.seckill;

import org.junit.jupiter.api.Test;

//@SpringBootTest
class GulimallSeckillApplicationTests {
    String key ="_"+"001";
    @Test
    void contextLoads() {
        if ("61ac515_001".endsWith(key)){
            System.out.println( "true");
        }else {
            System.out.println("false");
        }
    }



}
