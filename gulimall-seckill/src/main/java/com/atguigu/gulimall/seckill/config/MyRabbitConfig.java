package com.atguigu.gulimall.seckill.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     *
     */

   /* @PostConstruct//MyRabbitConfig对象创建完成以后，执行这个方法
    public void initRabbitTemplate() {
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            *//**
             *
             * @param correlationData  消息的唯一id
             * @param b                broker是否接收到消息
             * @param s                失败的原因
             *//*
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("correlationData：" + correlationData + "===>broker是否接收到消息：" + b + "===>失败的原因：" + s);
            }
        });

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            *//**
             *
             * @param message 投递失败的消息详细信息
             * @param i       回复的状态码
             * @param s       回复的文本内容
             * @param s1      当时这个消息发给哪个交换机
             * @param s2      当时这个消息用哪个路邮键
             *//*
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                System.out.println("Message:" + message + "\n===>replyCode:" + i + "\n===>replyText:" + s + "\n===>exchangeName:" + s1 + "\n===>routekey:" + s2);
            }
        });
    }*/

}
