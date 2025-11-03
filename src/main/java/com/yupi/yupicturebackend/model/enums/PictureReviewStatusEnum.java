package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * @program: yu-picture
 * @description: 图片审核状态枚举
 * @author: Miao Zheng
 * @date: 2025-10-22 12:21
 **/
@Getter
public enum PictureReviewStatusEnum {

    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;

    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取对应的枚举实例
     *
     * @param value 枚举值的字符串表示
     * @return 匹配到的枚举实例，如果没有匹配则返回null
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {

        // 如果传入的值为空，则直接返回null
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        // 遍历枚举的所有实例
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            // 检查当前枚举实例的值是否与传入的值相等
            if (pictureReviewStatusEnum.value == value) {
                return pictureReviewStatusEnum;
            }
        }
        // 如果没有找到匹配的枚举实例，则返回null
        return null;
    }
}
