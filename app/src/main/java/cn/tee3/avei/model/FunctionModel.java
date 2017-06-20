package cn.tee3.avei.model;

/**
 * 功能model
 * Created by shengf on 2017/6/13.
 */

public class FunctionModel {
    public static final int TITLE = 0;

    public static final int RECORD = 1;//录制功能
    public static final int EXPORT = 2;//导出功能
    public static final int AV_IMPORT = 3;//简单导入功能
    public static final int RAW_DATA_IMPORT = 4;//原始数据导入功能
    public static final int ENCODED_IMPORT = 5;//编码数据导入功能
    public static final int RTSP_IMPORT = 6;//rtsp数据导入功能

    //功能状态
    public static enum FunctionStateOptions {
        start,//开始（正在录制、导入等）
        stop//暂停
    }

    private int type;
    private String name;
    private String describe;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

}
