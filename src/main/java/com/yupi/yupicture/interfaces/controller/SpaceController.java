package com.yupi.yupicture.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.yupi.yupicture.infrastructure.annotation.AuthCheck;
import com.yupi.yupicture.infrastructure.common.BaseResponse;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.common.ResultUtils;
import com.yupi.yupicture.domain.user.constant.UserConstant;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.assembler.SpaceAssembler;
import com.yupi.yupicture.interfaces.dto.space.*;
import com.yupi.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @program: yu-space
 * @description:
 * @author: Miao Zheng
 * @date: 2025-10-24 17:02
 **/
@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        User loginUser = userApplicationService.getLoginUser(request);
        long newId = spaceApplicationService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User loginUser = userApplicationService.getLoginUser(request);
        Long spaceId = deleteRequest.getId();
        // 先判断是否存在
        Space oldSpace = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 仅本人或管理员可以删除
        spaceApplicationService.checkSpaceAuth(loginUser, oldSpace);
        // 操作数据库
        boolean result = spaceApplicationService.removeById(spaceId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除空间失败");
        return ResultUtils.success(true);
    }

    /**
     * 更新空间 - 仅管理员
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        Space space = SpaceAssembler.toSpaceEntity(spaceUpdateRequest);
        //自动填充数据
        spaceApplicationService.fillSpaceBySpaceLevel(space);

        //数据校验
        space.validSpace(false);
        // 判断是否存在
        Space oldSpace = spaceApplicationService.getById(spaceUpdateRequest.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        //操作数据库
        boolean result = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新空间失败");
        return ResultUtils.success(true);

    }

    /**
     * 根据id 获取空间 -  仅 管理员可用
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestParam Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "参数为空");
        //查询数据库
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        //获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据id获取 空间vo封装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "参数为空");
        //查询数据库
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 添加权限列表到 SpaceVO 中
        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space, request);
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permisssionList = spaceUserAuthManager.getPermisssionList(space, loginUser);
        spaceVO.setPermissionList(permisssionList);
        //获取封装类
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {

        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();

        // 查询数据库
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, size),
                spaceApplicationService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);

    }

    /**
     * 分页获取空间列表 -  封装类
     *
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {

        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();

        //限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "不允许查询过多数据");
        //查询数据库
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, size),
                spaceApplicationService.getQueryWrapper(spaceQueryRequest));

        //获取封装类
        return ResultUtils.success(spaceApplicationService.getSpaceVOPage(spacePage, request));

    }

    /**
     * 编辑空间 - 给用户素使用
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest
            , HttpServletRequest request) {

        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 将实体类和 DTO进行转换
        Space space = SpaceAssembler.toSpaceEntity(spaceEditRequest);
        // 数据填充
        spaceApplicationService.fillSpaceBySpaceLevel(space);
        //设计编辑时间
        space.setUpdateTime(new Date());
        // 数据校验
        space.validSpace(false);
        User loginUser = userApplicationService.getLoginUser(request);

        // 判断是否存在
        Long spaceId = spaceEditRequest.getId();
        Space oldSpace = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 判断权限 仅 本人和管理员可以编辑
        spaceApplicationService.checkSpaceAuth(loginUser, oldSpace);

        // 操作数据库
        boolean result = spaceApplicationService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);

    }

    /**
     * 获取空间级别列表，便于展示
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());

        return ResultUtils.success(spaceLevelList);

    }

}
