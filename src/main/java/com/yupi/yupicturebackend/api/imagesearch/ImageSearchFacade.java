package com.yupi.yupicturebackend.api.imagesearch;

import com.yupi.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.yupi.yupicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.yupi.yupicturebackend.api.imagesearch.sub.GetImageListApi;
import com.yupi.yupicturebackend.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

/**
 * @program: yu-picture
 * @description:
 * @author: Miao Zheng
 * @date: 2025-11-11 13:23
 **/
public class ImageSearchFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {

        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;

    }

    public static void main(String[] args) {
//        List<ImageSearchResult> imageList =
//                searchImage("https://pic.code-nav.cn/post_cover/1608440217629360130/hLDHxSd7guOKKf0Q.webp");
        List<ImageSearchResult> imageSearchResults = searchImage("https://yupi-1317805131.cos.ap-guangzhou.myqcloud.com/public/1980859362231947266/2025-11-03_Ht2FPLUcXAEaItJk.webp");
        System.out.println(imageSearchResults);
    }

}
