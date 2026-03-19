package com.atguigu.gulimall.cart.controller;

import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;
    /**
     * 跳转到购物车页面
     * 浏览器cookie标识user:key,标识用户身份,一个月过期
     * 第一次使用，生成一个user-key,标识用户身份,一个月过期,每次都带
     * 登录了,session中有
     * 未登录，使用cookie中的user-key
     *
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(){

        UserInfoTo userInfoTo = CartInterceptor.ThreadLocal.get();
        System.out.println("userInfoTo = " + userInfoTo);

        return "cartList";
    }
    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) throws ExecutionException, InterruptedException {
        CartItem cartItem = cartService.addToCart(skuId,num);
        model.addAttribute("item", cartItem);
        return "success";
    }
}
