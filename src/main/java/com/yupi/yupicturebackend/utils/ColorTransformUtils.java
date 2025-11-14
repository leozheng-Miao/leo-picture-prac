package com.yupi.yupicturebackend.utils;

/**
 * @program: yu-picture
 * @description: 图片颜色转换
 * @author: Miao Zheng
 * @date: 2025-11-11 16:22
 **/
public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    /**
     * 将不完整的颜色字符串标准化为 8 位格式 (0xRRGGBB)
     *
     * @param imageAve 从腾讯COS获取的主色调字符串，可能缺少前导零
     * @return 标准化后的 8 位颜色字符串
     */
    public static String normalizeColor(String imageAve) {
        // 处理空值
        if (imageAve == null || imageAve.trim().isEmpty()) {
            return "0x000000";
        }

        // 去除空格并转换为小写
        imageAve = imageAve.trim().toLowerCase();

        // 去掉 0x 前缀
        String hexValue = imageAve.replace("0x", "");

        // 验证是否为有效的十六进制字符串
        if (!hexValue.matches("[0-9a-f]+")) {
            throw new IllegalArgumentException("无效的颜色值: " + imageAve);
        }

        // 补零到 6 位
        hexValue = String.format("%6s", hexValue).replace(' ', '0');

        // 返回标准格式
        return "0x" + hexValue;
    }

    /**
     * 方法2：使用整数转换（推荐）
     * 更加安全，可以处理各种格式
     */
    public static String normalizeColorSafe(String imageAve) {
        if (imageAve == null || imageAve.trim().isEmpty()) {
            return "0x000000";
        }

        try {
            // 去除 0x 前缀并解析为整数
            String hexValue = imageAve.trim().toLowerCase().replace("0x", "");
            int colorValue = Integer.parseInt(hexValue, 16);

            // 确保在有效范围内 (0x000000 - 0xFFFFFF)
            if (colorValue < 0 || colorValue > 0xFFFFFF) {
                throw new IllegalArgumentException("颜色值超出范围: " + imageAve);
            }

            // 格式化为 6 位十六进制，保持小写
            return String.format("0x%06x", colorValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的颜色值: " + imageAve, e);
        }
    }

    /**
     * 解析颜色值为RGB分量
     */
    public static int[] parseRGB(String colorStr) {
        String normalized = normalizeColorSafe(colorStr);
        String hex = normalized.replace("0x", "");

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new int[]{r, g, b};
    }

    // 测试方法
    public static void main(String[] args) {
        // 测试用例
        String[] testCases = {
                "0x0e00",      // 你的绿色图片例子
                "0x00e0",      // 你的蓝色图片例子
                "0xff0000",    // 纯红色（完整格式）
                "0xf00",       // 纯红色（缺少零）
                "0x0",         // 黑色（极端情况）
                "0xffffff",    // 纯白色
                "0x123456"     // 正常情况
        };

        System.out.println("颜色字符串标准化测试：\n");
        System.out.println("原始值\t\t标准化结果\tRGB值");
        System.out.println("=========================================");

        for (String testCase : testCases) {
            try {
                String normalized = normalizeColorSafe(testCase);
                int[] rgb = parseRGB(testCase);
                System.out.printf("%s\t\t%s\tR=%d, G=%d, B=%d\n",
                        testCase, normalized, rgb[0], rgb[1], rgb[2]);
            } catch (Exception e) {
                System.out.printf("%s\t\t错误: %s\n", testCase, e.getMessage());
            }
        }

        System.out.println("\n特别验证你的案例：");
        System.out.println("0x0e00 (绿色图) -> " + normalizeColorSafe("0x0e00") +
                " = RGB(0, 14, 0) - 深绿色");
        System.out.println("0x00e0 (蓝色图) -> " + normalizeColorSafe("0x00e0") +
                " = RGB(0, 0, 224) - 亮蓝色");
    }

}
