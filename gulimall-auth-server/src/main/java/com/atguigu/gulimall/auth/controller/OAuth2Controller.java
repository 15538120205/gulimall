package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.GitHubUserInfo;
import com.atguigu.gulimall.auth.vo.MemberRespVo;
import com.atguigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 处理社交登录请求
 */
@Controller
@Slf4j
public class OAuth2Controller {
    @Autowired
    private MemberFeignService memberFeignService;
    @GetMapping("/oauth2.0/github/success")
    public String github(@RequestParam("code") String code, HttpSession session) throws Exception {
        //根据code获取access_token
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept","application/json");
        Map<String, String> querys = new HashMap<>();
        querys.put("client_id","Ov23liNCLPajMl0k7YG3");
        querys.put("client_secret","1cdb60aa04fd03628281057da2ed72012b7b0989");
        querys.put("code",code);
        querys.put("redirect_uri","http://auth.gulimall.com/oauth2.0/github/success");
        Map<String, String> bodys = new HashMap<>();

        HttpResponse response = HttpUtils.doPost("https://github.com", "/login/oauth/access_token", "POST", headers, querys, bodys);
        //处理
        if(response.getStatusLine().getStatusCode() ==200){
            //获取到了access_token
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            String access_token = socialUser.getAccess_token();
            System.out.println("888888888888888888888888888888888888888"+access_token);
            //封装用户数据
            GitHubUserInfo gitHubUserInfo = new GitHubUserInfo();
            Map<String, String> headers1 = new HashMap<>();
            headers1.put("Authorization","Bearer "+access_token);
            headers1.put("Accept","application/vnd.github.v3+json");
            Map<String, String> querys1 = new HashMap<>();
            HttpResponse get = HttpUtils.doGet("https://api.github.com", "/user", "GET", headers1, querys1);
            if (get.getStatusLine().getStatusCode() == 200){
                String json1 = EntityUtils.toString(get.getEntity());
                gitHubUserInfo = JSON.parseObject(json1, GitHubUserInfo.class);
                gitHubUserInfo.setAccessToken(access_token);
            }else {
                //失败
                return "redirect:http://auth.gulimall.com/login.html";
            }
            //判断当前用户是否存在, 不存在则注册
            R r = memberFeignService.oauth2Login(gitHubUserInfo);
            if (r.getCode() == 0){
                //获取信息
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {});
                session.setAttribute("loginUser",data);
                System.out.println("登录成功,用户信息:"+data.toString());
                //登录成功
                return "redirect:http://gulimall.com";
            }else {
                //失败
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else {
            //失败
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
