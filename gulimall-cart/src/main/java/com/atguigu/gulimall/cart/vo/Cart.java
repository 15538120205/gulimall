package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Description: 整体购物车
 *  这里不用@Data，自己生成getter和setter方法，主要为了数量、金额等属性自定义计算方法。
 *  例如Cart里的商品数量通过CartItem列表计算总数量。
 */
public class Cart {
 
    /**
     * 购物车子项信息
     */
    List<CartItem> items;
    /**
     * 商品的总数量
     */
    private Integer countNum;
    /**
     * 商品类型数量
     */
    private Integer countType;
    /**
     * 商品总价
     */
    private BigDecimal totalAmount;
    /**
     * 减免价格
     */
    private BigDecimal reduce = new BigDecimal("0");
 
//需要计算的属性，必须重写它的get方法，保证每次获取属性都会进行计算
    public List<CartItem> getItems() {
        return items;
    }
 
    public void setItems(List<CartItem> items) {
        this.items = items;
    }
 
    public Integer getCountNum() {
        int count = 0;
        if (items!=null && items.size()>0) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }
 
 
    public Integer getCountType() {
        int count = 0;
        if (items!=null && items.size()>0) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }
 
 
    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        // 1、计算购物项总价
        if (items!=null && items.size()>0) {
            for (CartItem item : items) {
                BigDecimal totalPrice = item.getTotalPrice();
                if (item.getCheck()) {
                    amount = amount.add(totalPrice);
                }
            }
        }
        // 2、减去优惠总价
        BigDecimal subtract = amount.subtract(getReduce());
        return subtract;
    }
 
 
    public BigDecimal getReduce() {
        return reduce;
    }
 
    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}