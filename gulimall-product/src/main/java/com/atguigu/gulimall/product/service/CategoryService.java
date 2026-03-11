package com.atguigu.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author xmh
 * @email liu241023@qq.com
 * @date 2026-03-10 17:07:32
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);
    /**
     * 查询catelogId的完整路径
     * @param catelogId
     * @return
     */
    Long[] findCatelogPath(Long catelogId);
}

