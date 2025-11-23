package com.yupi.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupicture.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.yupi.yupicture.interfaces.vo.user.LoginUserVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author zhengsmacbook
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-10-22 12:16:35
 */
public interface UserApplicationService {


    /**
     * 用户注册方法
     * @param userRegisterRequest 用户注册请求
     * @return 返回一个long类型的结果，可能表示注册状态码或用户ID
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录方法
     *
     * @param userLoginRequest      用户登录请求
     * @param request      HTTP请求对象，用于获取请求相关信息
     * @return LoginUserVO 登录成功后返回的用户信息视图对象
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);


    /**
     * 根据HTTP请求获取登录用户信息
     *
     * @param request HttpServletRequest对象，包含当前请求的所有信息
     * @return User 返回当前登录的用户对象，如果用户未登录则可能返回null
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 加密用户密码的方法
     *
     * @param userPassword 用户输入的原始密码
     * @return 加密后的密码字符串
     */
    String getEncryptPassword(String userPassword);

    /**
     * 根据用户信息获取登录用户视图对象
     *
     * @param user 用户实体对象
     * @return LoginUserVO 登录用户视图对象，包含用户登录相关信息
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 根据用户实体对象获取用户视图对象(UserVO) - 脱敏后的用户信息
     * 该方法用于将用户实体(User)转换为前端展示用的视图对象(UserVO)
     *
     * @param user 用户实体对象，包含用户的完整信息
     * @return UserVO 用户视图对象，用于前端展示的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 根据用户实体对象获取用户视图对象(UserVO) - 脱敏后的用户信息列表
     * 该方法用于将用户实体(User)转换为前端展示用的视图对象(UserVO)
     *
     * @param userList 用户实体对象，包含用户的完整信息
     * @return UserVO 用户视图对象，用于前端展示的用户信息
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    Long saveUser(User user);
}
