package com.yupi.yupicture.infrastructure.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @program: yu-picture
 * @description:
 * @author: Miao Zheng
 * @date: 2025-11-12 16:48
 **/
@Slf4j
@Component
public class AliYunApi {

    @Value("${aliYunAi.apikey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    //创建任务
    public CreateOutPaintingTaskResponse createOutPaintingTaskResponse(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {

        HttpRequest request = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                // 1. 为了扩展
                // 2. 显式
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        //处理响应
        try (HttpResponse httpResponse = request.execute()) {
            if (!httpResponse.isOk()) {
                log.error("AI 扩图失败，响应码：{}，响应内容：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse =
                    JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse.getCode() != null) {
                String message = createOutPaintingTaskResponse.getMessage();
                log.error("AI 扩图失败，响应码：{}，响应内容：{}", createOutPaintingTaskResponse.getCode(), message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败" + message);
            }
            return createOutPaintingTaskResponse;
        }
    }

    //查询创建的任务结果
    public GetOutPaintingTaskResponse getOutPaintingTaskResponse(String taskId) {

        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务id不能为空");
        }
        String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        //处理响应
        try (HttpResponse httpResponse = HttpRequest.get(url).
                header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("AI 扩图失败，响应码：{}，响应内容：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

}
