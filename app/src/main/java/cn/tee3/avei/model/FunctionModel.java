package cn.tee3.avei.model;

import android.app.Activity;

/**
 * 功能model
 * Created by shengf on 2017/6/13.
 */

public class FunctionModel {
    //功能类型
    public static enum FunctionType {
        TITLE,
        RECORD,//录制功能
        EXPORT,//导出功能
        AV_IMPORT,//简单导入功能
        RAW_DATA_IMPORT,//原始数据导入功能
        ENCODED_IMPORT,//编码数据导入功能
        RTSP_IMPORT//rtsp数据导入功能
    }

    //功能状态
    public static enum FunctionStateOptions {
        start,//开始（正在录制、导入等）
        stop//暂停
    }


    private FunctionType functionType;
    private String name;
    private String describe;
    private Activity mActivity;
    private String appType;

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public Activity getmActivity() {
        return mActivity;
    }

    public void setmActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public void setFunctionType(FunctionType functionType) {
        this.functionType = functionType;
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
