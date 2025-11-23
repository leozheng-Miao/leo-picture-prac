package com.yupi.yupicture.domain.space.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import lombok.Data;

/**
 * 空间
 * @TableName space
 */
@TableName(value ="space")
@Data
public class Space {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public void validSpace(boolean add) {

        String spaceName = this.getSpaceName();
        Integer spaceLevel = this.getSpaceLevel();
        Integer spaceType = this.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 创建时的 校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        //修改时的校验， 空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }

        // 空间级别的值不存在
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }

        // 空间类型的值不存在
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }

    }

    public void fillDefaultValue() {
        if (StrUtil.isBlank(this.getSpaceName())) {
            this.setSpaceName("我的空间");
        }
        if (this.getSpaceLevel() == null) {
            this.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (this.getSpaceType() == null) {
            this.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
    }
}