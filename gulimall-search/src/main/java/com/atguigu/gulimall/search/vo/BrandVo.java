package com.atguigu.gulimall.search.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class BrandVo {
    private Long brandId;
    @JSONField(name = "name")
    private String brandName;
}
