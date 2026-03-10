package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author ljx
 * @email liu241023@qq.com
 * @date 2026-03-10 17:55:59
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
