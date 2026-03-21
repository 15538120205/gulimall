package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
/**
 * @author
 * @Description 商品信息VO
 */
@Data
public class OrderItemVo {

    private Long skuId;

    /**
     * 是否被选中
     */
    private Boolean check;

    private String title;

    /**
     * 默认图片
     */
    private String image;

    /**
     * 商品套餐属性
     */
    private List<String> skuAttr;

    /**
     * 商品单价
     */
    private BigDecimal price;

    /**
     * 商品数量
     */
    private Integer count;

    /**
     * 总价
     */
    private BigDecimal totalPrice;

//    /**
//     * 是否有货
//     */
//    private Boolean hasStock;

    /**
     * 重量
     */
    private BigDecimal weight;


}