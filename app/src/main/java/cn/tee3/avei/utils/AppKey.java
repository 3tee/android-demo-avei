package cn.tee3.avei.utils;

/**
 * 叁体服务信息配置
 *
 * @note 主要配置叁体服务器信息，appkey和secretkey
 * appkey和secretkey请开发者用叁体分配的相关信息。
 */

public class AppKey {
    // updated in archive script
    public static final String
            tee3_avd_server = "3tee.cn:8080";
    public static final String
            tee3_app_key = "F89EB5C71E494850A061CC0C5F42C177";
    public static final String
            tee3_secret_key = "DDDF7445961C4D27A7DCE106001BBB4F";

    /**
     * RTSP
     *
     * @param[in] rtsp_uri流地址。
     * @param[in] rtsp_username认证用户名。
     * @param[in] rtsp_password认证密码。
     */
    public static final String
            rtsp_uri = "rtsp://192.168.1.121:554/hikvision://192.168.1.121:8000:0:0";
    public static final String
            rtsp_username = "admin";
    public static final String
            rtsp_password = "Hik12345";
}
