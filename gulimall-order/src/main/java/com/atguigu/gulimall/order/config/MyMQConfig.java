package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMQConfig {

    @Bean
    public Queue orderDelayQueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        return new Queue("order.delay.queue",true,false,false,arguments);
    }
    @Bean
    public Queue orderReleaseOrderQueue(){
        return new Queue("order.release.order.queue",true,false,false);
    }
    @Bean
    public Queue orderSeckillOrderQueue(){
        return new Queue("order.seckill.order.queue",true,false,false);
    }



    @Bean
    public Exchange orderEventExchange(){
        Map<String, Object> arguments = new HashMap<>();
        return new TopicExchange("order-event-exchange",true,false,arguments);
    }
    @Bean
    public Binding orderCreateOrderBinding(){
        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE,"order-event-exchange","order.create.order",null);
    }
    @Bean
    public Binding orderReleaseOrderBinding(){
        return new Binding("order.release.order.queue", Binding.DestinationType.QUEUE,"order-event-exchange","order.release.order",null);
    }

    /**
     * 订单释放直接和库存释放进行绑定
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);
    }

    @Bean
    public Binding orderSeckillOrderBinding(){
        return new Binding("order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);
    }

}
