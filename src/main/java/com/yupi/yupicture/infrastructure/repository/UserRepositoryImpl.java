package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.user.repository.UserRepository;
import com.yupi.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * @program: yu-picture DDD
 * @description:
 * @author: Miao Zheng
 * @date: 2025-11-21 18:08
 **/
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
