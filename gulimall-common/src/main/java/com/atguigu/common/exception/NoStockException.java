package com.atguigu.common.exception;

public class NoStockException extends RuntimeException{
    private Object skuId;
    public NoStockException(Object skuId){
        super("商品id:"+skuId+"没有足够的库存了");
    }

    public Object getSkuId() {
        return skuId;
    }

    public void setSkuId(Object skuId) {
        this.skuId = skuId;
    }
}
