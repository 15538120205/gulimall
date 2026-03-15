package com.atguigu.common.to.es;

import lombok.Data;
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

}
