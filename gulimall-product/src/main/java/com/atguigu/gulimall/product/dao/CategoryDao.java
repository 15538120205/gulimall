package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author xmh
 * @email liu241023@qq.com
 * @date 2026-03-10 17:07:32
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
