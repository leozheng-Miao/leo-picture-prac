package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.repository.SpaceRepository;
import com.yupi.yupicture.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

/**
 * @program: yu-picture DDD
 * @description:
 * @author: Miao Zheng
 * @date: 2025-11-23 23:45
 **/
@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
