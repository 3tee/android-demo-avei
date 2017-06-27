package cn.tee3.avei;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.tee3.avd.AVDEngine;
import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.RoomInfo;
import cn.tee3.avei.utils.AppKey;

/**
 * 作者：jksfood on 2017/4/17 10:36
 */

public class AveiApp extends Application implements AVDEngine.Listener {
    private static final String TAG = "LawPush4AndroidPad";
    private static Context context;
    private RoomListener rListener;

    public interface RoomListener {
        void RoomEvent(RoomEventType roomEventType, int ret, String roomIdResult);
    }

    public void setRoomListener(RoomListener rListener) {
        this.rListener = rListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate, begin init AVDEngine ");
        context = getApplicationContext();
        String tee3dir = getTee3Dir();
        DateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dumpfile = tee3dir + "3tee.cn" + format.format(new Date()) + ".dump";
        AVDEngine.instance().setDumpFile(dumpfile);

        String logfile = tee3dir + "3tee.cn" + format.format(new Date()) + ".log";
        AVDEngine.instance().setLogParams("debug verbose", logfile);

        int ret = AVDEngine.instance().init(getApplicationContext(), this, AppKey.tee3_avd_server, AppKey.tee3_app_key, AppKey.tee3_secret_key);
        if (ErrorCode.AVD_OK != ret) {
            Log.e(TAG, "onCreate, init AVDEngine failed. ret=" + ret);
        }
    }

    //返回
    public static Context getContextObject() {
        return context;
    }

    public static enum RoomEventType {
        GetRoomResult,//查询房间返回
        ScheduleRoomResult//安排房间返回
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        AVDEngine.instance().uninit();
        Log.i(TAG, "onTerminate, after uninit AVDEngine ");
    }

    static public String getTee3Dir() {
        String tee3dir = "/sdcard/cn.tee3.avei/";
        if (isFolderExists(tee3dir)) {
            return tee3dir;
        } else {
            return "/sdcard/";
        }
    }

    static boolean isFolderExists(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onInitResult(int result) {
        Log.i(TAG, "onInitResult result:" + result);
    }

    @Override
    public void onUninitResult(int reason) {
        Log.i(TAG, "onUninitResult reason:" + reason);
    }

    @Override
    public void onGetRoomResult(int result, RoomInfo roomInfo) {
        Log.i(TAG, "onScheduleRoomResult,result=" + result + ",roomId=" + roomInfo.toString());
        rListener.RoomEvent(RoomEventType.GetRoomResult, result, roomInfo.getRoomId());
    }

    @Override
    public void onFindRoomsResult(int i, List<RoomInfo> list) {

    }

    @Override
    public void onScheduleRoomResult(int result, String roomId) {
        Log.i(TAG, "onScheduleRoomResult,result=" + result + ",roomId=" + roomId);
        rListener.RoomEvent(RoomEventType.ScheduleRoomResult, result, roomId);
    }

    @Override
    public void onCancelRoomResult(int i, String s) {
    }

}
