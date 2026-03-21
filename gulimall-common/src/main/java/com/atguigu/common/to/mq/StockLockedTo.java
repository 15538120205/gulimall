package com.atguigu.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    /**
     * 库存工作单的id
     */
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockDetailTo getDetail() {
        return detail;
    }

    public void setDetail(StockDetailTo detail) {
        this.detail = detail;
    }

    /**
     * 工作单详情类
     */
    private StockDetailTo detail;
}
 