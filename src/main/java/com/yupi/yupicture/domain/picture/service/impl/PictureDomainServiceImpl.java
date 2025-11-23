package com.yupi.yupicture.domain.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicture.domain.picture.entity.Picture;
import com.yupi.yupicture.domain.picture.repository.PictureRepository;
import com.yupi.yupicture.domain.picture.service.PictureDomainService;
import com.yupi.yupicture.domain.picture.valueobject.PictureReviewStatusEnum;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.api.CosManager;
import com.yupi.yupicture.infrastructure.api.aliyunai.AliYunApi;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.infrastructure.utils.ColorSimilarUtils;
import com.yupi.yupicture.interfaces.dto.picture.*;
import com.yupi.yupicture.interfaces.vo.picture.PictureVO;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhengsmacbook
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-10-24 17:52:52
 */
@Slf4j
@Service
public class PictureDomainServiceImpl implements PictureDomainService {


    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private AliYunApi aliYunApi;
    @Resource
    private PictureRepository pictureRepository;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 教研参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 有空间 仅管理员才能上传
//            if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
            //校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间大小不足");
            }
        }

        // 校验 是否有空间权限

        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {

            Picture oldPicture = pictureRepository.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, " 图片不存在 ");

            // 仅本人或管理员可以编辑图片
//            if (!oldPicture.getUserId().equals(loginUser.getId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的spaceId （兼容公共图库）
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 穿了 spaceId， 必须和之前的spaceId一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间不一致");
                }
            }
        }
        //上传图片 得到图片信息
        // 按照用户id 划分目录 => 改为按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        // 根据inputSource的类型 区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 默认从图片结果中拿名称， 若外层名称不为空则用外层名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        //操作数据库
        // 如果 pictureId 不为空 表示更新 否则是 新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 更新空间的使用额度
        // 牺牲了一些精确度
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = pictureRepository.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, " 图片上传失败, 数据库操作失败 ");
            if (finalSpaceId != null) {
                boolean update = spaceApplicationService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, " 额度更新失败 ");
            }
            return picture;
        });

        // todo 可自行实现： 如果是更新，可以清理图片资源
//        this.clearPictureFile(picture);
        return PictureVO.objToVo(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();


        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(Picture picture, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        Integer reviewStatus = picture.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 判断图片是否存在
        Picture oldPicture = pictureRepository.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3.判断审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 4。 数据库操作
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "审核失败");
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {

        if (loginUser.isAdmin()) {
            // 管理员直接通过审核
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
        } else {
            // 非管理员 一律 审核中 状态
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 教研参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();

        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条" );
        // 名称前缀默认等于搜索关键词
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        //抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
             document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        //解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        // 遍历元素， 依次处理上传图片
        int uploadCount = 0;
        for (Element element : div.select("img.mimg")) {
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片的地址， 防止 转义 或者 和对象存储冲突的问题
            // codefather.cn?yupi=dog     干掉 ？ 后面的内容
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));

            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功， id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败， fileUrl = {}", fileUrl, e);
                continue;
            }

            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;

    }


    // 这里的异步 需要 新开一个线程， 默认使用 SimpleAsyncTaskExecutor 线程池
    // SimpleAsyncTaskExecutor 线程池， 每次都会新建一个线程， 不会重用线程
    // 所以这个默认线程池 并不好， 因为每次都会新建一个线程池，会占用系统本身的默认线程池
    @Async // 异步操作， 要在 启动类 添加 @EnableAsync 注解
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断 该 图片 是否 为多条记录使用
        String pictureUrl = oldPicture.getUrl();
        Long count = pictureRepository.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 如果有不止一条记录使用了该图片，则不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在

        Picture oldPicture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        //校验权限
        // 已经改为注解式鉴权
//        checkPictureAuth(loginUser, oldPicture);

        Long spaceId = oldPicture.getSpaceId();

        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = pictureRepository.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");
            // 更新空间的使用额度， 释放额度
            if (spaceId != null) {
                boolean update = spaceApplicationService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, " 额度更新失败 ");
            }
            return true;
        });
        this.clearPictureFile(oldPicture);

    }

    @Override
    public void editPicture(Picture picture, User loginUser) {

        //设计编辑时间
        picture.setUpdateTime(new Date());
        // 数据校验
        picture.validPicture();
        // 判断是否存在
        Long pictureId = picture.getId();
        Picture oldPicture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 判断权限 仅 本人和管理员可以编辑
        // 已经改为注解式鉴权
//        this.checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);

        // 操作数据库
        boolean result = pictureRepository.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库 仅本人或管理员可以操作
            if (!picture.getUserId().equals(loginUserId) && !loginUser.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
            }
        } else {
            // 私有图库 仅本人可以操作
            if (!picture.getUserId().equals(loginUserId))
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 教研参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2. 校验权限
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        // 3. 查询该空间下的所有图片
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 没有图片 直接返回
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        Color targetColor = Color.decode(picColor);
        //4. 将颜色字符串转换为//5. 计算相似度
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hasColor = picture.getPicColor();
                    if (StrUtil.isBlank(hasColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hasColor);
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12)
                .collect(Collectors.toList());

        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());

    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 获取和教研参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);

        //校验空间权限
        Space space = spaceApplicationService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }

        // 查询指定图片 （仅选择需要的字段）
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .select(Picture::getId, Picture::getUserId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        //更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        // 批量更新
        boolean result = pictureRepository.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "批量更新失败");
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = pictureRepository.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 已经改为注解式鉴权
//        checkPictureAuth(loginUser, picture);
        // 调用阿里云接口
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 创建任务
        return aliYunApi.createOutPaintingTaskResponse(createOutPaintingTaskRequest);
    }

    /**
     * nameRule 格式为：    图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        // 获取序号
        int index = 1;
        try {
            for (Picture picture : pictureList) {
                picture.setName(nameRule.replaceAll("\\{序号}", String.valueOf(index++)));
            }
        } catch (Exception e) {
            log.error("命名规则错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "命名规则错误");
        }
    }
}




