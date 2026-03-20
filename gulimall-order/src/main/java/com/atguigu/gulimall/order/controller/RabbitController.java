package com.atguigu.gulimall.order.controller;


import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;



@RestController
@RequestMapping("/rabbit")
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/sendMessage")
    public R sendMessage(@RequestParam(value = "num", defaultValue = "10") Integer num) {
        for (int i = 0; i < num; i++) {
            if (i % 2 == 0) {
                OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
                orderReturnReasonEntity.setId(1L);
                orderReturnReasonEntity.setName("哈哈" + i);
                orderReturnReasonEntity.setSort(1);
                orderReturnReasonEntity.setStatus(0);
                orderReturnReasonEntity.setCreateTime(new Date());

                rabbitTemplate.convertAndSend("hello.java.exchange", "hello.java", orderReturnReasonEntity,new CorrelationData(
                        UUID.randomUUID().toString()
                ));
                System.out.println("OrderReturnReasonEntity消息发送成功");
            } else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello.java.exchange", "hello.java", orderEntity,new CorrelationData(
                        UUID.randomUUID().toString()
                ));
                System.out.println("OrderEntity消息发送成功");
            }
        }
        return R.ok();
    }
}