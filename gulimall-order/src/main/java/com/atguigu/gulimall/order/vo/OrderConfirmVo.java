package com.atguigu.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author
 * @Description 订单确认页VO
 */

public class OrderConfirmVo {
    /**
     * 会员收获地址列表
     */
    @Getter
    @Setter
    List<MemberAddressVo> memberAddressVos;

    /**
     * 所有选中的购物项
     */
    @Getter
    @Setter
    List<OrderItemVo> items;

    /**
     * 优惠券（会员积分）
     */
    @Getter
    @Setter
    private Integer integration;



    /**
     * 防止重复提交的令牌
     */
    @Getter
    @Setter
    String orderToken;


    private BigDecimal total;//订单总额

    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal("0");
        if (items != null && items.size() > 0){
            for (OrderItemVo item : items) {
                total = total.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }
        return total;
    }


    private BigDecimal payPrice;//订单应付价格
    public BigDecimal getPayPrice() {
        return getTotal();
    }

    public Integer getCount() {
        Integer count = 0;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    @Getter
    @Setter
    Map<Long,Boolean> stocks;
    @Getter
    @Setter
    private BigDecimal weight;



}