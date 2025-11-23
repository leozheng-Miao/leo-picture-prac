package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.space.repository.SpaceUserRepository;
import com.yupi.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

/**
 * @program: yu-picture DDD
 * @description:
 * @author: Miao Zheng
 * @date: 2025-11-23 23:45
 **/
@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
