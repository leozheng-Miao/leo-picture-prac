package com.yupi.yupicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * @program: yu-picture
 * @description: 添加 @Component 注解的目的是 确保 下面的状态 被 初始化
 * @author: Miao Zheng
 * @date: 2025-11-17 13:25
 **/
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    /**
     * 默认会话对象，管理所有用户的登录，权限认证 目前的项目没有用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象， 管理 Space 表 中 所有用户的登录，权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);

}
