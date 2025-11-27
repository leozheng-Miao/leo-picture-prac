package com.yupi.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.picture.repository.PictureRepository;
import com.yupi.yupicture.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * @program: yu-picture DDD
 * @description: 图片仓储实现
 * @author: Miao Zheng
 * @date: 2025-11-22 11:41
 **/
@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
