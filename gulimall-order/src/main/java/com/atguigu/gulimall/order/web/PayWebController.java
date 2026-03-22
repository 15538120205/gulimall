package com.atguigu.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {
    @Autowired
    AlipayTemplate alipayTemplate;
    @Autowired
    OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/payOrder",produces = "text/html;charset=UTF-8")
    public String payOrder(@RequestParam String orderSn) throws AlipayApiException {
        System.out.println("订单编号：" + orderSn);
        PayVo payVo = orderService.getOrderPay(orderSn);
        //支付宝返回支付页面
        String pay = alipayTemplate.pay(payVo);
        System.out.println(pay);
        return pay;
    }
}
