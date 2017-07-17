package cn.tee3.avei.model;

/**
 * 叁体服务信息配置
 * Created by shegf on 2017/7/12.
 */

public class DemoParams {
    private DemoOption option;
    private int ret;//返回结果0为成功
    private String server_uri;
    private String secret_key;
    private String access_key;

    public DemoOption getOption() {
        return option;
    }

    public void setOption(DemoOption option) {
        this.option = option;
    }

    public int getRet() {
        return ret;
    }

    public void setRet(int ret) {
        this.ret = ret;
    }

    public String getServer_uri() {
        return server_uri;
    }

    public void setServer_uri(String server_uri) {
        this.server_uri = server_uri;
    }

    public String getSecret_key() {
        return secret_key;
    }

    public void setSecret_key(String secret_key) {
        this.secret_key = secret_key;
    }

    public String getAccess_key() {
        return access_key;
    }

    public void setAccess_key(String access_key) {
        this.access_key = access_key;
    }
}
