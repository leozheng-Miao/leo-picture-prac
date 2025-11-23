package com.yupi.yupicture.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.user.constant.UserConstant;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.user.repository.UserRepository;
import com.yupi.yupicture.domain.user.service.UserDomainService;
import com.yupi.yupicture.domain.user.valueobject.UserRoleEnum;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupicture.interfaces.vo.user.LoginUserVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import com.yupi.yupicturebackend.manager.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhengsmacbook
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-10-22 12:16:35
 */
@Service
@Slf4j
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    @Override  // 重写父类或接口中的方法，确保方法签名一致
    /**
     * 用户注册方法
     * @param userAccount 用户账号，用于注册时输入的用户名
     * @param userPassword 用户密码，用于注册时输入的密码
     * @param checkPassword 确认密码，用于二次确认用户输入的密码
     * @return 返回值为long类型，可能是用户ID或注册状态码，当前实现返回0
     */
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        // 2. 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在");
        }
        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = userRepository.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败，数据库错误");
        }

        return user.getId();
    }

    @Override
    /**
     * 用户登录方法
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HTTP请求对象，用于保存用户登录状态
     * @return LoginUserVO 包含用户登录信息的视图对象
     */
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        //2. 对用户传输的密码进行加密处理，确保密码安全
        String encryptPassword = getEncryptPassword(userPassword);
        //3. 查询用户信息 不存在 抛异常
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userRepository.getBaseMapper().selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
        }
        //4. 保存信息
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录态到 Sa-Token， 便于空间鉴权时使用
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        // 如果用户对象为空或用户ID为空，说明未登录
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //能不用缓存 就不用 - 注释说明此处不使用缓存，直接从数据库获取最新用户信息
        //校验用户是否合法
        Long userId = currentUser.getId();
        currentUser = userRepository.getById(userId); // 根据用户ID从数据库获取用户信息
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR); // 如果用户不存在，抛出未登录异常
        }
        return currentUser; // 返回合法的用户信息

    }

    @Override
    /**
     * 获取加密后的密码
     * @param userPassword 用户输入的原始密码
     * @return 返回经过MD5加密后的密码字符串
     */
    public String getEncryptPassword(String userPassword) {
        // 加盐， 混淆密码
        final String SALT = "yupi"; // 定义盐值，用于密码加密
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
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
        if (user == null) return null;
        // 创建登录用户视图对象实例
        LoginUserVO loginUserVO = new LoginUserVO();
        // 使用BeanUtil工具类将用户实体对象的属性复制到登录用户视图对象中
        BeanUtil.copyProperties(user, loginUserVO);
        // 返回填充好数据的登录用户视图对象
        return loginUserVO;
    }


    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户实体对象，包含用户的完整信息
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) return null;
        // 创建登录用户视图对象实例
        UserVO userVO = new UserVO();
        // 使用BeanUtil工具类将用户实体对象的属性复制到登录用户视图对象中
        BeanUtil.copyProperties(user, userVO);
        // 返回填充好数据的登录用户视图对象
        return userVO;
    }

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList 用户实体对象，包含用户的完整信息
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) return new ArrayList<>();
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        //判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 如果用户对象为空或用户ID为空，说明未登录
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        //移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    @Override
    public boolean saveUser(User user) {
        return userRepository.save(user);
    }

}

