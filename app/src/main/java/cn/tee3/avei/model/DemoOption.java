package cn.tee3.avei.model;

/**
 * 接口返回rtsp的相关信息
 * Created by shengf on 2017/7/12.
 */

public class DemoOption {
    private String userAddress;
    private String login_name;
    private String login_password;

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public String getLogin_name() {
        return login_name;
    }

    public void setLogin_name(String login_name) {
        this.login_name = login_name;
    }

    public String getLogin_password() {
        return login_password;
    }

    public void setLogin_password(String login_password) {
        this.login_password = login_password;
    }
}
