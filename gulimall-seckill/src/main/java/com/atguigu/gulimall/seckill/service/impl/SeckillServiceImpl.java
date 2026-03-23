package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private static final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private static final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    private static final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1.扫描最近三天需要参与秒杀的商品
        R r = couponFeignService.getLast3DaySession();
        if (r.getCode() == 0) {
            List<SeckillSessionsWithSkus> sessionData = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {});
            // 2.保存到Redis中
            // 2.1 Redis存储结构
            // hset(k1,field1,value1)
            // hset(k1,field2,value2)
            // hset(k1,field3,value3)
            // hset(k1,field4,value4)
            // 2.2 缓存活动信息
            // 2.3 缓存活动关联的商品
            saveSessionInfos(sessionData);
            saveSessionSkuInfos(sessionData);

        }
    }

    /**
     * 服务降级调用的方法
     * @param e
     * @return
     */
    public List<SecKillSkuRedisTo> blockHandler(BlockException e){
        log.error("getCurrentSeckillSkus.....服务降级");
        return null;
    }
    //基于注解的资源降级
    @SentinelResource(value = "getCurrentSeckillSkus", blockHandler = "blockHandler" )
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        try (Entry entry = SphU.entry("seckillSkus")) {
        // 1.查询当前时间所属的秒杀场次
        long currentTime = System.currentTimeMillis();// 当前时间
        // 查询所有秒杀场次的key
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");// keys seckill:sessions:*
        for (String key : keys) {
            //seckill:sessions:1594396764000_1594453242000
            String replace = key.replace(SESSION_CACHE_PREFIX, "");// 截取时间，去掉前缀
            String[] time = replace.split("_");
            long startTime = Long.parseLong(time[0]);// 开始时间
            long endTime = Long.parseLong(time[1]);// 截止时间
            // 判断是否处于该场次
            if (currentTime >= startTime && currentTime <= endTime) {
                // 2.查询当前场次信息（查询结果List< sessionId_skuId > ）
                List<String> sessionIdSkuIds = redisTemplate.opsForList().range(key, -100, 100);// 获取list范围内100条数据
                // 获取商品信息
                BoundHashOperations<String, String, String> skuOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                assert sessionIdSkuIds != null;
                // 根据List< sessionId_skuId >从Map中批量获取商品信息
                List<String> skus = skuOps.multiGet(sessionIdSkuIds);
                if (!CollectionUtils.isEmpty(skus)) {
                    // 将商品信息反序列成对象
                    List<SecKillSkuRedisTo> skuInfos = skus.stream().map(sku -> {
                        SecKillSkuRedisTo skuInfo = JSON.parseObject(sku.toString(), SecKillSkuRedisTo.class);
                        // redisTo.setRandomCode(null);当前秒杀开始需要随机码
                        return skuInfo;
                    }).collect(Collectors.toList());
                    return skuInfos;
                }
                // 3.匹配场次成功，退出循环
                break;
            }
        }
        } catch (BlockException e) {
            //TODO 资源限流处理
            log.error("资源被限流{}", e.getMessage());
        }
        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 1.匹配查询当前商品的秒杀信息
        BoundHashOperations<String, String, String> skuOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        // 获取所有商品的key：sessionId_
        Set<String> keys = skuOps.keys();
        if (!CollectionUtils.isEmpty(keys)) {
            String lastIndex = "_" + skuId;
            for (String key : keys) {
                if (key.endsWith(lastIndex)) {
                    // 商品id匹配成功
                    String jsonString = skuOps.get(key);
                    // 进行序列化
                    SecKillSkuRedisTo skuInfo = JSON.parseObject(jsonString, SecKillSkuRedisTo.class);
                    Long currentTime = System.currentTimeMillis();
                    Long endTime = skuInfo.getEndTime();
                    if (currentTime <= endTime) {
                        // 当前时间小于截止时间
                        Long startTime = skuInfo.getStartTime();
                        if (currentTime >= startTime) {
                            // 返回当前正处于秒杀的商品信息
                            return skuInfo;
                        }
                        // 返回预告信息，不返回随机码
                        skuInfo.setRandomCode(null);// 随机码
                        return skuInfo;
                    }
                }
            }
        }
        return null;
    }
    /**
     * 秒杀商品
     * 1.校验登录状态
     * 2.校验秒杀时间
     * 3.校验随机码、场次、商品对应关系
     * 4.校验信号量扣减，校验购物数量是否限购
     * 5.校验是否重复秒杀（幂等性）【秒杀成功SETNX占位  userId_sessionId_skuId】
     * 6.扣减信号量
     * 7.发送消息，创建订单号和订单信息
     * 8.订单模块消费消息，生成订单
     * @param killId    sessionId_skuid
     * @param key   随机码
     * @param num   商品件数
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        // 1.校验登录状态
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        if (memberRespVo == null) {
            return null;
        }
        //获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        if (StringUtils.isEmpty(s)) {
            return null;
        }else {
            SecKillSkuRedisTo skuInfo = JSON.parseObject(s, SecKillSkuRedisTo.class);
            Long startTime = skuInfo.getStartTime();
            Long endTime = skuInfo.getEndTime();
            Long currentTime = System.currentTimeMillis();
            long ttl = endTime - currentTime;
            // 校验秒杀时间
            if (currentTime >= startTime && currentTime <= endTime) {
                // 校验随机码、商品id
                String randomCode = skuInfo.getRandomCode();
                String skuId = skuInfo.getPromotionSessionId()+"_"+skuInfo.getSkuId();
                if (key.equals(randomCode) && killId.equals(skuId)) {
                    //验证购物数量是否限购
                    if (num <= skuInfo.getSeckillLimit().intValue()){
                        //验证是否购买过了
                        String redisKey = memberRespVo.getId()+"_"+skuId;
                        //自动过期
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            //占位成功,没买过
                            //减去秒杀信号量
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            try {
                                boolean b = semaphore.tryAcquire(num,100, TimeUnit.MILLISECONDS);
                                //秒杀成功
                                if (b) {
                                    //快速下单,发送给MQ
                                    String orderSn = IdWorker.getTimeId();
                                    SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                    seckillOrderTo.setOrderSn(orderSn);
                                    seckillOrderTo.setPromotionSessionId(skuInfo.getPromotionSessionId());
                                    seckillOrderTo.setSkuId(skuInfo.getSkuId());
                                    seckillOrderTo.setNum(new BigDecimal(num));
                                    seckillOrderTo.setMemberId(memberRespVo.getId());
                                    seckillOrderTo.setSeckillPrice(skuInfo.getSeckillPrice());

                                    rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",seckillOrderTo);

                                    return orderSn;
                                }

                            } catch (InterruptedException e) {
                                return null;
                            }
                        }else {
                            //买过了
                            return null;
                        }


                    }

                }else {
                    return null;
                }


            }else {
                return null;
            }


        }
        return null;
    }


    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        if (sessions != null)
        sessions.stream().forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX+startTime + "_" + endTime;
            Boolean hasKey = redisTemplate.hasKey(key);
            // 缓存活动信息
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId()+"_"+item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,collect);
            }
        });
    }
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.stream().forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(relationSku -> {
                String token = UUID.randomUUID().toString().replace("-", "");
                if (!ops.hasKey(relationSku.getPromotionSessionId().toString()+"_"+relationSku.getSkuId().toString())){
                    // 缓存每个商品的秒杀信息
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    // sku基本信息
                    R r = productFeignService.getSkuInfo(relationSku.getSkuId());
                    if (r.getCode() == 0){
                        SkuInfoVo info = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }
                    // sku秒杀信息
                    BeanUtils.copyProperties(relationSku,redisTo);
                    //秒杀的时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    //随机码

                    redisTo.setRandomCode(token);

                    String s = JSON.toJSONString(redisTo);
                    ops.put(relationSku.getPromotionSessionId().toString()+"_"+relationSku.getSkuId().toString(),s);


                    //引入分布式的信号量  限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(relationSku.getSeckillCount().intValue());
                }

            });
        });
    }
}
