package com.yupi.yupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: yu-picture
 * @description: 获取以图搜图页面地址 step 1
 * @author: Miao Zheng
 * @date: 2025-11-11 11:46
 **/
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {

        //1. 准备请求参数
        HashMap<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String ack_token = "1762751569056_1762836390746_LizCkwtd8aCT8Q0g5H/g4AlrTZBfrH+Nxozej5Ubpi1sEVJEH1H3xiSscdYCx1Dkl9gtOALXYfoMOdZRsIBacLDuu18VZdwV8egB116YIiYLX00OYQx1cMOgdKOrf06UaG8tituUHCrSu+4/sM1YcP)8FdVAVZ6mT9TJ9rNn8sUP1btLSeedhmiioU7zwUvG4iSsT9020gF4n2fE+h0m9J659me585GUavF4zHF6esEBygtm2ncQA7bNX0Z3kkGI4CVCROFQrT9r2KgZgQdjAZgRf10RYQ20Refm3UeRA/dpNMaMV2aM8BfcHCN9ix4DWkSeRkLT5504sNJH82NvGtVOXa8+84Au5a8woHAZG4YBIMzb16ECY254V6SEC9S6FaJrY63c0.tPu0oD+tadgaAsc9baFWZv6M7T6D8QKOHLB9SWwwSBOS5ChWYsTg";
        try {

            // 2. 发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .header("acs-token", ack_token)
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            //解析响应：
            String body = httpResponse.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);

            //3 . 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            // url解码
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }
            return searchResultUrl;

        } catch (Exception e) {
            log.error("百度接口调用失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }

    }

    public static void main(String[] args) {
        String imageUrl = "https://pic.code-nav.cn/post_picture/1631593416829796353/fuPqWqEOl4fTX9kD.webp";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println(searchResultUrl);
    }
}
