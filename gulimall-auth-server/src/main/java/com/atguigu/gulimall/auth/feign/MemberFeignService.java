package com.atguigu.gulimall.auth.feign;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @PostMapping("/member/member/regist")
    R register(@RequestBody UserRegistVo vo);

    /**
     * 远程调用用户登录
     * @param userLoginVo
     * @return
     */
    @PostMapping(value = "/member/member/login")
    R login(@RequestBody UserLoginVo userLoginVo);
}
