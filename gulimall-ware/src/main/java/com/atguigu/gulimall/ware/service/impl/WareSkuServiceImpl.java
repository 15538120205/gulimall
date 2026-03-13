package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;


@Service("wareSkuService")
@Slf4j
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private ProductFeignService productFeignService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)){
            wrapper.eq("sku_id",skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)){
            wrapper.eq("ware_id",wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断是否有库存记录

        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId);
        List<WareSkuEntity> entities = this.baseMapper.selectList(queryWrapper);
        if (entities == null || entities.size() == 0){
            //新增库存记录
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // 查询sku名字
            /*try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                String skuName = (String)   data.get("skuName");
                if (info.getCode() == 0){
                    wareSkuEntity.setSkuName(skuName);
                }
            }catch (Exception e){

            }*/
            String skuName = null;
            try {
                R info = productFeignService.info(skuId);
                // 先判断调用是否成功，再获取数据（修正逻辑顺序）
                if (info != null && info.getCode() == 0) {
                    Object skuInfoObj = info.get("skuInfo");
                    if (skuInfoObj != null && skuInfoObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) skuInfoObj;
                        skuName = (String) data.get("skuName");
                        // 去除首尾空格，避免空字符串
                        skuName = trimToNull(skuName);
                    }
                }
            } catch (Exception e) {
                // 优化3：记录异常日志，不吞掉异常
                log.error("调用商品服务获取skuName失败，skuId={}", skuId, e.toString());
                // 降级策略：skuName 设为 null，不影响库存新增
                skuName = null;
            }
            wareSkuEntity.setSkuName(skuName); // 即使为空也不影响核心逻辑
            this.baseMapper.insert(wareSkuEntity);
        }else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }
    }
    private String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim(); // 去除首尾空格
        return trimmed.isEmpty() ? null : trimmed;
    }

}