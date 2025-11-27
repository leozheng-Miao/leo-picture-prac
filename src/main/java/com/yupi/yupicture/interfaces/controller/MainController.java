package com.yupi.yupicture.interfaces.controller;

import com.yupi.yupicture.infrastructure.common.BaseResponse;
import com.yupi.yupicture.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: yu-picture
 * @description:
 * @author: Miao Zheng
 * @date: 2025-10-21 13:58
 **/
@RestController
@RequestMapping("/")
public class MainController {
    /**
     * 健康检查接口
     *
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
