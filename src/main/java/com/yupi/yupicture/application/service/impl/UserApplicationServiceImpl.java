package com.yupi.yupicture.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.user.service.UserDomainService;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupicture.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.yupi.yupicture.interfaces.vo.user.LoginUserVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import com.yupi.yupicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author zhengsmacbook
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-10-22 12:16:35
 */
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    /**
     * 用户注册方法
     * @param userRegisterRequest 用户注册请求
     * @return 返回值为long类型，可能是用户ID或注册状态码，当前实现返回0
     */
    @Override  // 重写父类或接口中的方法，确保方法签名一致
    public long userRegister(UserRegisterRequest userRegisterRequest) {

        String userPassword = userRegisterRequest.getUserPassword();
        String userAccount = userRegisterRequest.getUserAccount();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 校验
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 执行注册
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);

    }

    /**
     * 用户登录方法
     * @param userLoginRequest 用户登录请求
     * @param request HTTP请求对象，用于保存用户登录状态
     * @return LoginUserVO 包含用户登录信息的视图对象
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        User.validUserLogin(userAccount, userPassword);
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {

        return userDomainService.getLoginUser(request)
;
    }

    /**
     * 获取加密后的密码
     * @param userPassword 用户输入的原始密码
     * @return 返回经过MD5加密后的密码字符串
     */
    @Override
    public String getEncryptPassword(String userPassword) {

        return userDomainService.getEncryptPassword(userPassword);
    }

    /**
     * 重写父类方法，根据用户信息获取登录用户视图对象
     * 该方法将用户实体对象转换为登录用户视图对象，用于前端展示
     *
     * @param user 用户实体对象，包含用户的完整信息
     * @return LoginUserVO 登录用户视图对象，用于前端展示的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }


    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户实体对象，包含用户的完整信息
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList 用户实体对象，包含用户的完整信息
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }

    @Override
    public Long saveUser(User user) {

        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userDomainService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userDomainService.saveUser(user);
        ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "添加失败");
        return user.getId();
    }

}






