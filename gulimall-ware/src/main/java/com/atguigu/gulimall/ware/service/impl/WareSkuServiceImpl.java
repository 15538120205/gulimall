package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("wareSkuService")
@Slf4j
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private WareOrderTaskService orderTaskService;
    @Autowired
    private WareOrderTaskDetailService orderTaskDetailService;
    @Autowired
    private OrderFeignService orderFeignService;


    /**
     * 解锁库存
     *
     * @param
     */
    public void unlockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        this.baseMapper.unlockStock(skuId, wareId, num);
        //改变库存工作单状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        entity.setLockStatus(2);
        orderTaskDetailService.updateById(entity);

    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
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
        if (entities == null || entities.size() == 0) {
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
        } else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map(id -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            vo.setSkuId(id);
            Long count = this.baseMapper.getSkuStock(id);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        orderTaskService.save(taskEntity);
        // 1.找到所有商品在哪个仓库有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setNum(item.getCount());
            stock.setSkuId(skuId);
            // 查询这个商品在哪个仓库有库存
            stock.setWareId(this.baseMapper.listWareIdHasStock(skuId));
            return stock;
        }).collect(Collectors.toList());

        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(
                            null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1
                    );
                    orderTaskDetailService.save(taskDetailEntity);
                    //锁定成功,发消息给MQ
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, detailTo);
                    lockedTo.setDetail(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                } else {
                    //当前仓库锁失败,尝试下一个仓库
                }
            }
            if (!skuStocked) {
                // 当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        // 锁定成功
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {

        System.out.println("收到解锁库存消息");
        Long id = to.getId();//锁库存工作单的id
        StockDetailTo detail = to.getDetail();//锁库存工作单的详情
        //解锁
        Long detailId = detail.getId();
        //获取锁库存工作单的详情
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null) {
            //解锁
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            //查询订单状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                //判断订单状态
                if (data == null || data.getStatus() == 4) {
                    //订单状态为已取消状态或订单不存在需要解锁库存
                    if(byId.getLockStatus() == 1) {
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                //消息拒绝重新放到队列,让别人继续消费解锁
                throw new RuntimeException("远程服务失败");
            }
        } else {
            //无需解锁
        }

    }
    /**
     * 防止订单服务卡顿，导致订单状态消息一直改不了，库存消息优先到期，查订单状态新建状态，什么都不做。
     * 导致卡顿的订单永远不会解锁库存
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查询最新的库存解锁状态,防止重复解锁库存
        WareOrderTaskEntity taskEntity = orderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = taskEntity.getId();
        List<WareOrderTaskDetailEntity> entities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().
                eq("task_id", id).
                eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unlockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }

    @Data
    class SkuWareHasStock {
        private Integer num;
        private Long skuId;
        private List<Long> wareId;
    }

    private String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim(); // 去除首尾空格
        return trimmed.isEmpty() ? null : trimmed;
    }

}