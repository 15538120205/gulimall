package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Slf4j
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {

    }
    /**
     * 创建交换机
     */
    @Test
    public void createExchangeAndQueue() {
        amqpAdmin.declareExchange(new DirectExchange("hello.java.exchange",true,false));
        log.info("创建成功");
    }

    /**
     * 创建队列
     */
    @Test
    public void createQueue() {
        amqpAdmin.declareQueue(new Queue("hello.java.queue",true,false,false));
        log.info("创建成功");
    }
    /**
     * 创建绑定关系
     */
    @Test
    public void createBinding() {
        Binding binding = new Binding(
                "hello.java.queue",
                Binding.DestinationType.QUEUE,
                "hello.java.exchange",
                "hello.java",
                null
        );
        amqpAdmin.declareBinding(binding);
        log.info("创建成功");
    }
    /**
     * 发送消息
     */
    @Test
    public void sendMsg() {
        OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
        entity.setId(1L);
        entity.setCreateTime(new Date());
        entity.setName("hello world");
        // 发送消息是个对象,需要使用序列化机制
        rabbitTemplate.convertAndSend("hello.java.exchange","hello.java",entity,new CorrelationData(
                UUID.randomUUID().toString()
        ));
        //以json发送
//        rabbitTemplate.convertAndSend("hello.java.exchange","hello.java","hello world");
    }

    /**
     * 接收消息
     */
    @Test
    public void receiveMsg() {

    }

}
