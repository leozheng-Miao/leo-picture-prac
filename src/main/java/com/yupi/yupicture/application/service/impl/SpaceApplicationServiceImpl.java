package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicture.domain.space.service.SpaceDomainService;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.infrastructure.mapper.SpaceMapper;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author zhengsmacbook
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-11-04 13:19:43
*/
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceApplicationService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private SpaceDomainService spaceDomainService;


    @Resource
    private TransactionTemplate transactionTemplate;
    @Qualifier("spaceDomainService")


//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
//        ● 填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        space.fillDefaultValue();
        this.fillSpaceBySpaceLevel(space);
//        ● 校验参数
        space.validSpace( true);
//        ● 校验权限 - 非管理员用户只能创建普通版私有空间
        Long userId = loginUser.getId();
        space.setUserId(userId);

        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // ● 事务 - 一个用户只能创建一个私有空间 且 只能创建一个团队空间
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try {
                // 判断是否已有空间
                // exists 和 count 的区别
                // exists 是判断是否存在， count 是获取数量
                // exists会更快一些
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                // 如果有空间 则不能创建
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "一个用户每类空间只能创建一个");
                //创建
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                //创建成功后，如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                    // 新增团队成员记录
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserApplicationService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间成员到数据库失败");
                }
                //创建分表
//                dynamicShardingManager.createSpacePictureTable(space);
                // 返回用户id
                return space.getId();
            } finally {
                lockMap.remove(userId);
            }
        }

//        ● 限制 - 一个用户只能创建一个私有空间
        // 锁的力度限制在 每个用户上， 每个用户有不同的锁
        // 所以，我们的锁要根据 userId 生成
        // intern() 方法的作用是，
        // 如果常量池中存在相同内容的字符串，
        // 则返回池中的字符串，否则将字符串添加到常量池中，并返回该字符串的引用。
//        String lock = String.valueOf(userId).intern();
//        synchronized (lock) {
//            Long newSpaceId = transactionTemplate.execute(status -> {
//                // 判断是否已有空间
//                // exists 和 count 的区别
//                // exists 是判断是否存在， count 是获取数量
//                // exists会更快一些
//                boolean exists = this.lambdaQuery()
//                        .eq(Space::getUserId, userId)
//                        .exists();
//                // 如果有空间 则不能创建
//                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "一个用户只能创建一个私有空间");
//                //创建
//                boolean result = this.save(space);
//                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
//                // 返回用户id
//                return space.getId();
//            });
//            return Optional.ofNullable(newSpaceId).orElse(-1L);
//        }

    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());

        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        // 对象列表 =》 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        //1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdsListMap =
                userApplicationService.listByIds(userIdSet)
                        .stream()
                        .collect(Collectors.groupingBy(User::getId));

        //2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdsListMap.containsKey(userId)) {
                user = userIdsListMap.get(userId).get(0);
            }
            spaceVO.setUser(userApplicationService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }
}




