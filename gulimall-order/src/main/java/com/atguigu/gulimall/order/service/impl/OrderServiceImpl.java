package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private MemberFeignService memberFeignService;
    @Autowired
    private CartFeignService cartFeignService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    private WmsFeignService wmsFeignService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {

        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        System.out.println("memberRespVo:" + memberRespVo.getId());
        //获取当前线程请求头(解决Feign异步调用丢失请求头问题)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //每个线程都共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //远程调用会员服务查询用户收货地址
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setMemberAddressVos(address);
        }, executor);
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //每个线程都共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //获取当前用户所有选中的购物项
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            System.out.println("items:" + items);
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            //远程查询sku的库存
            R hasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);
        //TODO 查询商品重量
        confirmVo.setWeight(new BigDecimal("0.95"));

        confirmVo.setIntegration(memberRespVo.getIntegration());
        //订单总额/应付总额自动计算
        //TODO 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, java.util.concurrent.TimeUnit.MINUTES);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        confirmVoThreadLocal.set(vo);
        //1.验证令牌
        String orderToken = vo.getOrderToken();
//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespVo.getId());
//        if (orderToken != null && orderToken.equals(redisToken)){
        response.setCode(0);
        //令牌验证成功
        //脚本执行的返回结果： 0-代表令牌校验失败, 1-代表令牌成功删除即成功
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        //Arg1：用DefaultRedisScript的构造器封装脚本和返回值类型； Arg2：用于存放Redis中token的key；Arg3：用于比较的token，即浏览器存储的token
        //令牌的验证和删除必须保证原子性
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()),
                orderToken);
        if (result == 0L) {
            response.setCode(1);
            //令牌验证失败
            return response;
        } else {
            //令牌验证成功
            //下单,创建订单
            OrderCreateTo order = createOrder();
            //验价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            //页面提交的价格
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //金额对比成功
                //保存订单/保存订单项数据
                saveOrder(order);
                //锁定库存.只要有异常,回滚订单数据
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    //锁定成功
                    response.setOrder(order.getOrder());
                    //订单创建成功,发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return response;
                } else {
                    //锁定失败
                    String msg =(String) r.get("msg");
                    throw new NoStockException(msg);
//                    response.setCode(3);
//                    return response;
                }
            } else {
                response.setCode(2);
                return response;
            }
        }

//        }else {
//        }
//        return response;

    }
    /**
     * 订单自动关闭
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //获取当前订单信息
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == 0) {
            OrderEntity update = new OrderEntity();
            //需要关闭订单
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            //发送消息给MQ
            try {
                //TODO 保证消息一定发送,每个消息做日志记录,数据库保存详细信息,定期扫描,重新发送
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (AmqpException e) {
                //出现异常,重试发送
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderSn);
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        payVo.setSubject(orderItemEntities.get(0).getSkuName());
        payVo.setBody(orderItemEntities.get(0).getSkuAttrsVals());
        payVo.setTotal_amount(orderEntity.getTotalAmount().setScale(2, BigDecimal.ROUND_UP).toString());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );
        List<OrderEntity> order_sn = page.getRecords().stream().map(item -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", item.getOrderSn()));
            item.setItemsEntities(itemEntities);
            return item;
        }).collect(Collectors.toList());
        page.setRecords(order_sn);
        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setTotalAmount(new BigDecimal(vo.getBuyer_pay_amount()));
        infoEntity.setSubject(vo.getBody());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCreateTime(new Date());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);
        //修改订单状态
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            this.update(new UpdateWrapper<OrderEntity>().set("status", OrderStatusEnum.PAYED.getCode()).eq("order_sn", vo.getOut_trade_no()));
        }
        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrderTo) {

        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal decimal = seckillOrderTo.getSeckillPrice().multiply(seckillOrderTo.getNum());
        orderEntity.setPayAmount(decimal);
        orderEntity.setTotalAmount(decimal);
        this.save(orderEntity);
        //保存订单项信息
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderItemEntity.setSkuId(seckillOrderTo.getSkuId());
        orderItemEntity.setRealAmount(decimal);
        orderItemEntity.setSkuQuantity(seckillOrderTo.getNum().intValue());
        orderItemService.save(orderItemEntity);



    }


    /**
     * 保存订单数据
     *
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        //保存订单
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        //保存订单项数据
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 创建订单
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //创建订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单
        OrderEntity orderEntity = buildOrder(orderSn);
        //创建订单项数据
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        //验价格,计算价格相关
        computePrice(orderEntity, itemEntities);
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);
        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        //订单价格
        //订单总额
        BigDecimal coupon = new BigDecimal("0");
        BigDecimal promotion = new BigDecimal("0");
        BigDecimal integration = new BigDecimal("0");
        BigDecimal total = new BigDecimal("0");
        Integer giftGrowth = 0;
        Integer giftIntegration = 0;
        for (OrderItemEntity itemEntity : itemEntities) {
            BigDecimal realAmount = itemEntity.getRealAmount();
            BigDecimal promotionAmount = itemEntity.getPromotionAmount();
            BigDecimal integrationAmount = itemEntity.getIntegrationAmount();
            BigDecimal couponAmount = itemEntity.getCouponAmount();
            coupon.add(couponAmount);
            promotion.add(promotionAmount);
            integration.add(integrationAmount);
            //积分/成长值
            giftIntegration += itemEntity.getGiftIntegration();
            giftGrowth += itemEntity.getGiftGrowth();
            total = total.add(realAmount);
        }
        orderEntity.setTotalAmount(total);
        //应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);
        //积分/成长值
        orderEntity.setGrowth(giftGrowth);
        orderEntity.setIntegration(giftIntegration);
        orderEntity.setDeleteStatus(0);


    }


    /**
     * 构建订单数据
     *
     * @param orderSn
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        //获取当前用户ID
        entity.setMemberId(memberRespVo.getId());
        //获取收货地址信息
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //运费信息
        BigDecimal fare1 = fareResp.getFare();
        entity.setFreightAmount(fare1);
        //收货人信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        //设置订单状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        return entity;
    }

    /**
     * 构建所有订单项数据
     *
     * @param
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //获取订单项信息
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(item -> {
                OrderItemEntity orderItemEntity = new OrderItemEntity();
                orderItemEntity = buildOrderItem(item);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建某一个订单项数据
     *
     * @param item
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        //订单信息: 订单号，
        //商品spu信息
        Long skuId = item.getSkuId();
        R infoBySkuId = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = infoBySkuId.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfo.getId());
        itemEntity.setSpuName(spuInfo.getSpuName());
        itemEntity.setSpuBrand(spuInfo.getBrandId().toString());
        itemEntity.setCategoryId(spuInfo.getCatalogId());
        //商品sku信息
        itemEntity.setSkuId(item.getSkuId());
        itemEntity.setSkuName(item.getTitle());
        itemEntity.setSkuPic(item.getImage());
        itemEntity.setSkuPrice(item.getPrice());
        itemEntity.setSkuQuantity(item.getCount());
        itemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";"));
        //商品优惠信息(忽略)
        //商品积分信息
        itemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
        //订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        //订单项的总金额
        BigDecimal total = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
        //订单项的实付金额
        BigDecimal subtract = total.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);
        return itemEntity;
    }


}