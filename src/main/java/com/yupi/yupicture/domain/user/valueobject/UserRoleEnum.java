package com.yupi.yupicture.domain.user.valueobject;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * @program: yu-picture
 * @description: 用户角色枚举
 * @author: Miao Zheng
 * @date: 2025-10-22 12:21
 **/
@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取对应的枚举实例
     *
     * @param value 枚举值的字符串表示
     * @return 匹配到的枚举实例，如果没有匹配则返回null
     */
    public static UserRoleEnum getEnumByValue(String value) {

        // 如果传入的值为空，则直接返回null
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        // 遍历枚举的所有实例
        for (UserRoleEnum roleEnum : UserRoleEnum.values()) {
            // 检查当前枚举实例的值是否与传入的值相等
            if (roleEnum.getValue().equals(value)) {
                // 如果匹配，则返回该枚举实例
                return roleEnum;
            }
        }
        // 如果没有找到匹配的枚举实例，则返回null
        return null;
    }
}
