package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author ljx
 * @email liu241023@qq.com
 * @date 2026-03-10 17:47:12
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
