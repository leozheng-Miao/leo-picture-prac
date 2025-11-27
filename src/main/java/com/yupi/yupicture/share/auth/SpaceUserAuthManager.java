package com.yupi.yupicture.share.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicture.share.auth.model.SpaceUserAuthConfig;
import com.yupi.yupicture.share.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicture.share.auth.model.SpaceUserRole;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @program: yu-picture
 * @description: 空间成员权限管理
 * @author: Miao Zheng
 * @date: 2025-11-17 11:29
 **/
@Component
public class SpaceUserAuthManager {

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(map -> map.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);

        if (role == null) {
            return new ArrayList<>();
        }

        return role.getPermissions();

    }

    /**
     * 获取权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermisssionList(Space space, User loginUser) {

        // 校验参数
        if (loginUser == null) {
            return new ArrayList<>();
        }
        //管理员权限
        List<String> ADMIN_PERMISSIONS =
                getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 是否为公共图库
        if (space == null) {
            if (loginUser.isAdmin()) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        //判断空间类型
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        //团队空间 还是 私有空间
        switch (spaceTypeEnum) {
            case TEAM:
                SpaceUser spaceUser = spaceUserApplicationService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
            case PRIVATE:
                if (loginUser.isAdmin() || space.getUserId().equals(loginUser.getId())) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
        }
        return new ArrayList<>();
    }

}
