package com.atguigu.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {
    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum, Model model,
                                  HttpServletRequest request) {
        //获取到支付宝传来的所有请求数据,验证签名


        Map<String, Object> page = new HashMap<>();
        page.put("page", pageNum.toString());
        // 获取当前登录用户的所有订单列表数据
        R r = orderFeignService.listWithItem(page);
        System.out.println("订单数据：" + JSON.toJSONString( r));
        model.addAttribute("orders", r);
        return "orderList";
    }
}
