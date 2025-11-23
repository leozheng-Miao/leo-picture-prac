package com.yupi.yupicture.infrastructure.api;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.yupicture.infrastructure.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: yu-picture
 * @description:
 * @author: Miao Zheng
 * @date: 2025-10-24 16:53
 **/
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key
     * @param file
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public COSObject getObject(String key)
            throws CosClientException, CosServiceException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传对象 (并且附带图片的信息)
     *
     * @param key
     * @param file
     * @return
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理 ( 获取 基本信息页也被视为一种图片的处理)
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> rules = new ArrayList<>();
        //1.  图片压缩 - 转换成 webp 格式
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 2. 缩略图处理 - > 仅对 > 20 KB 的 图片进行压缩
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            // 若后缀值为空，则规定一个默认后缀
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }

        picOperations.setRules(rules);

        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     * @param key
     * @throws CosClientException
     * @throws CosServiceException
     */
    public void deleteObject(String key)
            throws CosClientException, CosServiceException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}
