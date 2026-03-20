package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.order.dao.OrderItemDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;


@RabbitListener(queues = {"hello.java.queue"})
@Service("orderItemService")
@Slf4j
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * 监听队列
     */

    /**
     * @RabbitHandler标注在方法上，重载区分不同的消息
     */
    @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity entity, Channel channel) throws Exception {
        byte[] body = message.getBody();
        MessageProperties messageProperties = message.getMessageProperties();
        System.out.println("接收到消息...内容：" + entity);
        try {
            //签收
            channel.basicAck(messageProperties.getDeliveryTag(), false);
            //拒签
//            channel.basicNack(messageProperties.getDeliveryTag(), false, true);
//            channel.basicReject(messageProperties.getDeliveryTag(), false);
        } catch (IOException e) {
            log.error("消息消费失败...", e);

        }
    }

    @RabbitHandler
    public void receiveMessage(OrderEntity orderEntity) {
        System.out.println("接收到消息...内容：" + orderEntity.getOrderSn());
    }

}