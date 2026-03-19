package com.atguigu.gulimall.member.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * GitHub OAuth2 用户信息实体类
 * 对应 GitHub API https://api.github.com/user 返回的JSON数据
 * 适配谷粒商城第三方登录业务场景
 */
@Data
public class GitHubUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * GitHub 登录用户名（如：15538120205）
     * 谷粒商城用途：昵称兜底（当name为空时使用）
     */
    private String login;

    /**
     * GitHub 全局唯一用户ID（数字）
     * 谷粒商城用途：作为第三方用户唯一标识，关联本地数据库
     */
    private Long id;

    /**
     * GitHub 节点ID（内部标识）
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("node_id")
    private String nodeId;

    /**
     * 用户头像URL（GitHub CDN地址）
     * 谷粒商城用途：展示用户头像，可下载到本地CDN
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /**
     * Gravatar头像ID（已废弃，基本为空）
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("gravatar_id")
    private String gravatarId;

    /**
     * 用户API地址
     * 谷粒商城用途：暂不使用
     */
    private String url;

    /**
     * 用户GitHub主页地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("html_url")
    private String htmlUrl;

    /**
     * 粉丝列表API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("followers_url")
    private String followersUrl;

    /**
     * 关注列表API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("following_url")
    private String followingUrl;

    /**
     * Gists代码片段API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("gists_url")
    private String gistsUrl;

    /**
     * 收藏仓库API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("starred_url")
    private String starredUrl;

    /**
     * 订阅仓库API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("subscriptions_url")
    private String subscriptionsUrl;

    /**
     * 所属组织API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("organizations_url")
    private String organizationsUrl;

    /**
     * 个人仓库API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("repos_url")
    private String reposUrl;

    /**
     * 事件列表API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("events_url")
    private String eventsUrl;

    /**
     * 收到的事件列表API地址
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("received_events_url")
    private String receivedEventsUrl;

    /**
     * 用户类型（固定为User）
     * 谷粒商城用途：暂不使用
     */
    private String type;

    /**
     * 用户视图类型（公开/私有）
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("user_view_type")
    private String userViewType;

    /**
     * 是否为GitHub站点管理员
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("site_admin")
    private Boolean siteAdmin;

    /**
     * 用户昵称（如：acmdyohhh）
     * 谷粒商城用途：展示在个人中心/头像旁
     */
    private String name;

    /**
     * 所属公司
     * 谷粒商城用途：暂不使用
     */
    private String company;

    /**
     * 个人博客地址
     * 谷粒商城用途：暂不使用
     */
    private String blog;

    /**
     * 所在地
     * 谷粒商城用途：暂不使用
     */
    private String location;

    /**
     * 公开邮箱（如：liu241023@qq.com）
     * 谷粒商城用途：关联用户账号，用于登录/找回密码
     */
    private String email;

    /**
     * 是否可被雇佣
     * 谷粒商城用途：暂不使用
     */
    private Boolean hireable;

    /**
     * 个人简介
     * 谷粒商城用途：暂不使用
     */
    private String bio;

    /**
     * Twitter用户名
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("twitter_username")
    private String twitterUsername;

    /**
     * 通知邮箱
     * 谷粒商城用途：补充邮箱信息（当email为空时使用）
     */
    @JsonProperty("notification_email")
    private String notificationEmail;

    /**
     * 公共仓库数量
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("public_repos")
    private Integer publicRepos;

    /**
     * 公共Gists数量
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("public_gists")
    private Integer publicGists;

    /**
     * 粉丝数量
     * 谷粒商城用途：暂不使用
     */
    private Integer followers;

    /**
     * 关注数量
     * 谷粒商城用途：暂不使用
     */
    private Integer following;

    /**
     * 账号创建时间（ISO格式：2022-01-06T04:08:13Z）
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    /**
     * 账号最后更新时间
     * 谷粒商城用途：暂不使用
     */
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("social_uid")
    private String socialUid; // 社交登录的唯一标识
    @JsonProperty("access_token")
    private String accessToken; // 访问令牌
}