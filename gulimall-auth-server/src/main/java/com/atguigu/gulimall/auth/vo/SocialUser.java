package com.atguigu.gulimall.auth.vo;

import lombok.Data;

@Data
public class SocialUser {

    private String access_token;
    private String token_type;
    private String scope;
}