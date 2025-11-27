package com.yupi.yupicture.share.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.share.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.application.service.PictureApplicationService;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.yupi.yupicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private PictureApplicationService pictureApplicationService;
    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 根据 loginId 获取权限列表
     *
     * @param loginId
     * @param loginType
     * @return
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 校验登录类型：若 loginType 不是 space，则直接返回空权限列表
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }

        List<String> ADMIN_PERMISSIONS =
                spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 获取上下文对象：从请求中获取 SpaceUserAuthContext上下文，
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 检查上下文字段是否为空。若所有字段均为空，视为公共图库操作，直接返回管理员权限列表
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        //4. 校验登录状态：
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        // 校验管理员：若当前用户为管理员，则返回管理员权限
        if (loginUser.isAdmin()) {
            return ADMIN_PERMISSIONS;
        }

        Long userId = loginUser.getId();
        //5. 从上下文优先获取 SpaceUser 对象，若上下文中存在该对象，则直接从其角色获取权限列表
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        //6. 通过 spaceUserId 获取空间用户信息，如果存在 spaceUserId：
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            //  a. 根据 id 找到相对应的 SpaceUser 对象数据，若未找到抛出异常
            spaceUser = spaceUserApplicationService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            //  b. 校验当前用户是否属于当前空间，若不是，则返回空列表
            SpaceUser loginSpaceUser = spaceUserApplicationService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            //  c. 获取该用户的角色并返回对应的权限列表
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        //7. 通过 spaceId 或 pictureId 获取当前空间的信息：
        // 如果 spaceId 和 pictureId 都为空，默认视为管理员权限
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureApplicationService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            if (spaceId == null) {
                // 对于公共图库：如果图片是当前用户创建的，或者当前用户是管理员，则返回管理员权限列表；如果不是当前用户上传的，则只返回 ‘仅查看’ 的权限码
                if (picture.getUserId().equals(userId)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        //8. 获取 Space 对象并判断空间类型： 查询 Space 信息，若为空则抛异常，否则根据空间类型进行权限判断：
        Space space = spaceApplicationService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        if (SpaceTypeEnum.PRIVATE.getValue() == space.getSpaceType()) {
            //私有空间：该空间是否属于当前用户或管理员，若属于，返回管理员权限码，其他用户返回空列表
            if (space.getUserId().equals(userId)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间：查询登录用户在该空间的角色，并返回对应的权限码。若用户不属于该空间，则返回空列表
            spaceUser = spaceUserApplicationService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 校验上下文对象是否所有字段均为空
     *
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) return true;
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field))
                .allMatch(ObjectUtil::isEmpty);
    }

    /**
     * 本项目中不使用
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {

        return new ArrayList<>();
    }

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();
        // GET 和 POST 请求 获取参数的方式不一样
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 获取请求参数
        if (ContentType.JSON.getValue().equals(contentType)) {
            // request 的 body 是一个流的形式， 只能读一次，读完就没了
            // 所以，此时我们需要自定义请求包装类和请求包装类过滤器
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据 请求路径 区别 id字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            // 获取请求路径的 业务前缀
            // /api/picture/aaa?a=1
            String requestURI = request.getRequestURI();
            // 先替换掉上下文前缀， 剩下的就是前缀
            String partURI = requestURI.replace(contextPath + "/", "");
            // 获取前缀第一个 / 前的字符串
            String moduleName = StrUtil.subBefore(partURI, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }


}
