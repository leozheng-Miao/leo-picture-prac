package com.yupi.yupicturebackend.mapper;

import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author zhengsmacbook
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-10-24 17:52:52
 * @Entity com.yupi.yupicturebackend.model.entity.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 查询分类统计信息
     *
     * @return 分类统计列表
     */
    @MapKey("period")
    List<Map<String, Object>> analyzeByTimeDimension(@Param("params") Map<String, Object> params);

}




