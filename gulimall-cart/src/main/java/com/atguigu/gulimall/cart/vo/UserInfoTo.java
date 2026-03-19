package com.atguigu.gulimall.cart.vo;

import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class UserInfoTo {
    private Long userId;
    private String userKey; 
 
    private boolean tempUser = false;   // 判断是否有临时用户
}