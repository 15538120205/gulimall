package com.atguigu.gulimall.member.service.impl;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneException;
import com.atguigu.gulimall.member.exception.UsernameException;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.gulimall.member.vo.GitHubUserInfo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.MemberUserLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity entity = new MemberEntity();
        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        //检查用户名,手机号是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        // 密码进行加密
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);
        entity.setNickname(vo.getUserName());
        entity.setGender(0);
        entity.setCreateTime(new Date());
        this.baseMapper.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneException {
        Integer phoneCount = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (phoneCount > 0) {
            throw new PhoneException();
        }

    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameException {
        Integer usernameCount = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (usernameCount > 0) {
            throw new UsernameException();
        }

    }

    @Override
    public MemberEntity login(MemberUserLoginVo userLoginVo) {
        String loginacct = userLoginVo.getLoginacct();
        String password = userLoginVo.getPassword();
        //1、去数据库查询 SELECT * FROM ums_member WHERE username=? OR mobile=?
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct).or().eq("email", loginacct));
        if (memberEntity != null) {
            //2、校验密码
            String password1 = memberEntity.getPassword();
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            boolean matches = bCryptPasswordEncoder.matches(password, password1);
            if (matches) return memberEntity;
        }
        return null;
    }

    @Override
    public MemberEntity login(GitHubUserInfo gitHubUserInfo) {
        String uid = gitHubUserInfo.getId().toString();
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            MemberEntity update= new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(gitHubUserInfo.getAccessToken());
            this.baseMapper.updateById(update);
            memberEntity.setAccessToken(gitHubUserInfo.getAccessToken());
            return memberEntity;
        }else {
            //注册
            MemberEntity register = new MemberEntity();
            register.setNickname(gitHubUserInfo.getName());
            register.setUsername(gitHubUserInfo.getLogin());
            register.setEmail(gitHubUserInfo.getEmail());
            register.setHeader(gitHubUserInfo.getAvatarUrl());
            register.setGender(0);
            register.setCreateTime(new Date());
            register.setSocialUid(uid);
            register.setAccessToken(gitHubUserInfo.getAccessToken());
            register.setSign(gitHubUserInfo.getBio());
            register.setPassword("123456");//初始密码
            register.setLevelId(1L);
            this.baseMapper.insert(register);
            return register;
        }

    }


}