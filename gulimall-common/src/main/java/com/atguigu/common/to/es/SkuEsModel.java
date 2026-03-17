package com.atguigu.common.to.es;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;


@Data
public class SkuEsModel {
    /**
     * SKU Elasticsearch 数据模型
     * 用于存储商品 SKU 信息到 Elasticsearch 搜索引擎中，支持搜索功能
     */

    /**
     * 商品 SKU ID
     */
    private Long skuId;

    /**
     * 商品 SPU ID
     */
    private Long spuId;

    /**
     * 商品标题
     */
    private String skuTitle;

    /**
     * 商品价格
     */
    private BigDecimal skuPrice;

    /**
     * 商品图片 URL
     */
    private String skuImg;

    /**
     * 销量计数
     */
    private Long saleCount;

    /**
     * 库存状态标识
     */
    private boolean hasStock;

    /**
     * 热度评分，用于排序和推荐
     */
    private Long hotScore;

    /**
     * 品牌 ID
     */
    private Long brandId;

    /**
     * 分类 ID
     */
    private Long catalogId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 品牌图片 URL
     */
    private String brandImg;

    /**
     * 分类名称
     */
    private String catalogName;

    /**
     * 商品属性列表
     */
    private List<Attr> attrs;

    /**
     * 商品属性内部类
     * 用于存储商品的规格参数信息
     */
    @Data
    public static class Attr {
        /**
         * 属性 ID
         */
        private Long attrId;

        /**
         * 属性名称
         */
        private String attrName;

        /**
         * 属性值
         */
        private String attrValue;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSpuId() {
        return spuId;
    }

    public void setSpuId(Long spuId) {
        this.spuId = spuId;
    }

    public String getSkuTitle() {
        return skuTitle;
    }

    public void setSkuTitle(String skuTitle) {
        this.skuTitle = skuTitle;
    }

    public BigDecimal getSkuPrice() {
        return skuPrice;
    }

    public void setSkuPrice(BigDecimal skuPrice) {
        this.skuPrice = skuPrice;
    }

    public String getSkuImg() {
        return skuImg;
    }

    public void setSkuImg(String skuImg) {
        this.skuImg = skuImg;
    }

    public Long getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(Long saleCount) {
        this.saleCount = saleCount;
    }

    public boolean isHasStock() {
        return hasStock;
    }

    public void setHasStock(boolean hasStock) {
        this.hasStock = hasStock;
    }

    public Long getHotScore() {
        return hotScore;
    }

    public void setHotScore(Long hotScore) {
        this.hotScore = hotScore;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Long getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(Long catalogId) {
        this.catalogId = catalogId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBrandImg() {
        return brandImg;
    }

    public void setBrandImg(String brandImg) {
        this.brandImg = brandImg;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public List<Attr> getAttrs() {
        return attrs;
    }

    public void setAttrs(List<Attr> attrs) {
        this.attrs = attrs;
    }
}
