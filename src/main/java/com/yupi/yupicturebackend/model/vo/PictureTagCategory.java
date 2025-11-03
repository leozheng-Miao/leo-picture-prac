package com.yupi.yupicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @program: yu-picture
 * @description: 图片标签分类标签列表
 * @author: Miao Zheng
 * @date: 2025-10-26 21:17
 **/
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
