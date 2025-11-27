package com.yupi.yupicture.domain.space.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import lombok.Data;

/**
 * 空间用户关联实体类
 * <p>
 * 用于存储空间与用户之间的关联关系信息
 *
 * @TableName space_user - 对应数据库表名为 space_user
 */
@TableName(value = "space_user")  // 指定此实体类对应的数据库表名为 space_user
@Data  // Lombok注解，自动生成getter、setter、toString等方法
public class SpaceUser {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}