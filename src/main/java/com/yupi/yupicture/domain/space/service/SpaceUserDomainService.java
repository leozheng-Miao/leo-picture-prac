package com.yupi.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
 * @author zhengsmacbook
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-11-14 13:48:24
 */
public interface SpaceUserDomainService extends IService<SpaceUser> {

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);


}
