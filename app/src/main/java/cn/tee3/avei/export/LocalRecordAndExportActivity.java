package cn.tee3.avei.export;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.MLocalRecord;
import cn.tee3.avd.MVideo;
import cn.tee3.avd.Room;
import cn.tee3.avei.AveiApp;
import cn.tee3.avei.Constants;
import cn.tee3.avei.R;
import cn.tee3.avei.avroom.AVExportRoom;
import cn.tee3.avei.avroom.AVRoom;
import cn.tee3.avei.files.FileListActivity;
import cn.tee3.avei.model.FunctionModel;
import cn.tee3.avei.view.AveiDialog;
import cn.tee3.avei.view.EventLogView;
import cn.tee3.avei.utils.FilesUtils;
import cn.tee3.avei.utils.StringUtils;
import cn.tee3.avei.utils.TimerUtils;

/**
 * 本地录制以及导出
 * <p>
 * Created by shengf on 2017/6/5.
 */

public class LocalRecordAndExportActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener, RadioGroup.OnCheckedChangeListener, MLocalRecord.StreamOutListener {
    private static final String TAG = "LocalRecordAndExportActivity";

    private TextView tvTitle;
    private TextView tvRecord;//录制
    private TextView tvExport;//导出
    private TextView tvRecordExportTime;//导出或者录制的时间
    private TextView tvFileList;//文件列表
    private EventLogView logView;
    private RadioGroup rgAudioSelect;//rb_audio_no无音频;rb_audio_one仅所选中视频用户的音频;rb_audio_without_me房间内除自己外的音频;rb_audio_all房间所有音频
    private ListView lvCameras;
    private View transView;

    private AVExportRoom mRoom;
    private List<MVideo.Camera> mPublishedCameras;//视频摄像头信息列表
    private CamerasAdapter cAdapter;
    private TimerUtils mTimerUtils;

    private String mvideoDeviceId = "";//录制所选择视频设备Id
    private String maudioUserId = "";//录制所选择音频所属用户Id
    private int videoFrameNum = 0;//视频上传的帧数
    private int audioFrameNum = 0;//音频上传的帧数（次数）
    private long videoDataSize = 0;//已上传视频数据的大小
    private long audioDataSize = 0;//已上传音频文件的大小
    private AVExportRoom.AudioOptions audioEIType = AVExportRoom.AudioOptions.record_one_by_video;//录制导出时所选的音频参数,默认选中一个用户

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_export_layout);
        Intent intent = getIntent();
        String roomId = intent.getStringExtra("roomId");

        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText("房间号：" + roomId);

        transView = findViewById(R.id.trans_view);
        logView = (EventLogView) findViewById(R.id.event_view);
        tvRecord = (TextView) findViewById(R.id.tv_record);
        tvExport = (TextView) findViewById(R.id.tv_export);
        lvCameras = (ListView) findViewById(R.id.lv_cameras);
        tvRecordExportTime = (TextView) findViewById(R.id.tv_record_export_time);
        tvFileList = (TextView) findViewById(R.id.tv_filelist);
        rgAudioSelect = (RadioGroup) findViewById(R.id.rg_audio_select);
        tvRecord.setTag(0);
        tvExport.setTag(0);

        tvRecord.setOnClickListener(this);
        tvExport.setOnClickListener(this);
        tvFileList.setOnClickListener(this);
        lvCameras.setOnItemClickListener(this);
        rgAudioSelect.setOnCheckedChangeListener(this);

        mTimerUtils = new TimerUtils(tvRecordExportTime);

        startUpVideo(roomId);//加入房间
    }


    void startUpVideo(String roomId) {
        Log.i(TAG, "startUpVideo");
        // step1: 加入房间
        mRoom = new AVExportRoom(roomId, new AVRoom.CameraPublishListener() {

            @Override
            public void CameraPublishEvent(boolean isCameraOpen, MVideo.Camera camera) {
                updatePublishedCameras(isCameraOpen, camera);
            }
        });
        int ret = mRoom.join("testuserId", "test_username", new Room.JoinResultListener() {
            @Override
            public void onJoinResult(int result) {
                if (ErrorCode.AVD_OK != result) {
                    check_ret(result);
                    return;
                }
                // step2: 列出房间中发布的视频
                mPublishedCameras = mRoom.getMVideoCameras();
                cAdapter = new CamerasAdapter(AveiApp.getContextObject(), mPublishedCameras);
                lvCameras.setAdapter(cAdapter);
                cAdapter.notifyDataSetChanged();
                if (mPublishedCameras == null || mPublishedCameras.size() == 0) {
                    logView.addVeryImportantLog("请用幸会加入此房间并打开摄像头，协助测试");
                }
            }
        });
        check_ret(ret);
    }

    boolean check_ret(int ret) {
        if (ErrorCode.AVD_OK != ret) {
            logView.addVeryImportantLog("加入房间失败：ErrorCode=" + ret);
            Log.w(TAG, "check_ret: ret=" + ret);
            mRoom.dispose();
            mRoom = null;
            return false;
        }
        logView.addImportantLog("加入房间成功");
        return true;
    }

    /**
     * 更新视频列表
     *
     * @param isCameraOpen
     * @param camera
     */
    public void updatePublishedCameras(boolean isCameraOpen, MVideo.Camera camera) {
        String ownerName = mRoom.mvideo.getOwnerName(camera.getId());
        if (isCameraOpen) {//房间加入摄像头,添加至列表
            //重新设置camera的setName（）方法 在摄像头设备的name前添加用户name，便于区分
            camera.setName(ownerName + ":" + camera.getName());
            //判断加入的摄像头是否已存在列表
            boolean isRepeat = mPublishedCameras.contains(camera);
            if (!isRepeat) {
                mPublishedCameras.add(camera);
                logView.addNormalLog(ownerName + "开启摄像头");
                cAdapter.notifyDataSetChanged();
            }
        } else {//房间有摄像头离开,从列表中移除列表
            for (int i = 0; i < mPublishedCameras.size(); i++) {
                if (mPublishedCameras.get(i).getId().equals(camera.getId())) {
                    mPublishedCameras.remove(i);
                    logView.addNormalLog(ownerName + "关闭摄像头");
                    cAdapter.notifyDataSetChanged();
                    if (Constants.SELECT_CAMERA_ID.equals(camera.getId())) {
                        Constants.SELECT_CAMERA_ID = "";
                    }
                }
            }
            if (mPublishedCameras == null || mPublishedCameras.size() == 0) {
                logView.addVeryImportantLog("请用幸会加入此房间并打开摄像头，协助测试");
            }
        }
    }

    @Override
    public void videoStreamOut(String recId, long timestamp_ns, int w, int h, boolean isKeyFrame, byte[] data, int len) {
        videoFrameNum = videoFrameNum + 1;
        videoDataSize = videoDataSize + data.length;
    }

    @Override
    public void audioStreamOut(String recId, long timestamp_ns, int sampleRate, int channels, byte[] data, int len) {
        audioFrameNum = audioFrameNum + 1;
        audioDataSize = audioDataSize + data.length;
    }

    private Runnable messageRunnable = new Runnable() {
        public void run() {
            logView.addDetailsLog("已导出视频" + videoFrameNum + "帧," + FilesUtils.FormetFileSize(videoDataSize));
            logView.addDetailsLog("已导出音频" + audioFrameNum + "帧," + FilesUtils.FormetFileSize(audioDataSize));
            mHandler.postDelayed(messageRunnable, 3000);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_record://录制
                TimerUtils.updatePrefixLabel("录制时长 ");
                int recordTag = (int) tvRecord.getTag();
                if (checkLocalRecordValid()) {
                    if (recordTag == 0) {
                        mTimerUtils.updateTimer();//开始计时
                        updateUI(FunctionModel.FunctionType.RECORD, FunctionModel.FunctionStateOptions.start);
                        mRoom.startLocalRecord(getLocalFile(), mvideoDeviceId, maudioUserId, audioEIType);
                    } else {
                        stopExport();
                        updateUI(FunctionModel.FunctionType.RECORD, FunctionModel.FunctionStateOptions.stop);
                        mRoom.stopRecordOrExport();//停止录制或导出
                    }
                }
                break;
            case R.id.tv_export://导出
                TimerUtils.updatePrefixLabel("导出时长 ");
                int exportTag = (int) tvExport.getTag();
                if (checkLocalRecordValid()) {
                    if (exportTag == 0) {
                        mHandler.postDelayed(messageRunnable, 0);
                        mTimerUtils.updateTimer();//开始计时
                        updateUI(FunctionModel.FunctionType.EXPORT, FunctionModel.FunctionStateOptions.start);
                        mRoom.startLocalExport(this, mvideoDeviceId, maudioUserId, audioEIType);
                    } else {
                        stopExport();
                        updateUI(FunctionModel.FunctionType.EXPORT, FunctionModel.FunctionStateOptions.stop);
                        mRoom.stopRecordOrExport();//停止录制或导出
                    }
                }
                break;
            case R.id.tv_filelist:
                Intent intent = new Intent(this, FileListActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

    }

    private boolean checkLocalRecordValid() {
        //已选视频
        if (StringUtils.isNotEmpty(mvideoDeviceId)) {
            return true;
        }
        //未选视频,选择了以下音频
        if (audioEIType == AVExportRoom.AudioOptions.record_all_withoutme || audioEIType == AVExportRoom.AudioOptions.record_all_inroom) {
            return true;
        }
        Toast.makeText(AveiApp.getContextObject(), "未选择视频的情况下,音频只可选择2、3选项", Toast.LENGTH_SHORT).show();
        return false;
    }

    private String getLocalFile() {
        String tee3dir = AveiApp.getTee3Dir();
        DateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return tee3dir + "avei" + format.format(new Date());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MVideo.Camera remote = mPublishedCameras.get(position);
        if (remote != null) {
            mvideoDeviceId = remote.getId();
            maudioUserId = mRoom.mvideo.getOwnerId(mvideoDeviceId);

            String selectOwner = mRoom.mvideo.getUserName(maudioUserId);
            logView.addNormalLog("您已选择" + selectOwner + "的视频");
            if (audioEIType == AVExportRoom.AudioOptions.record_one_by_video) {
                logView.addNormalLog("您已选择" + selectOwner + "的音频");
            }
            //选中视频后，改变改栏底色
            Constants.SELECT_CAMERA_ID = mvideoDeviceId;
            cAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 跟新UI
     *
     * @param type    录制/导出
     * @param options start：正在录制；stop：完成
     */
    public void updateUI(FunctionModel.FunctionType type, FunctionModel.FunctionStateOptions options) {
        if (type == FunctionModel.FunctionType.RECORD) {
            switch (options) {
                case stop:
                    logView.addImportantLog("停止录制");
                    tvRecord.setText("开始录制");
                    tvFileList.setClickable(true);
                    tvExport.setClickable(true);
                    tvRecord.setTag(0);
                    tvExport.setBackgroundResource(R.drawable.round_corner_blue);
                    transView.setBackgroundColor(getResources().getColor(R.color.transparent));
                    enableRadioGroup(rgAudioSelect);
                    lvCameras.setEnabled(true);
                    break;
                case start:
                    logView.addImportantLog("开始录制");
                    tvRecord.setText("停止录制");
                    tvFileList.setClickable(false);
                    tvExport.setClickable(false);
                    tvRecord.setTag(1);
                    tvExport.setBackgroundResource(R.drawable.round_corner_gray);
                    transView.setBackgroundColor(getResources().getColor(R.color.transparent_40));
                    disableRadioGroup(rgAudioSelect);
                    lvCameras.setEnabled(false);
                    break;
            }
        } else if (type == FunctionModel.FunctionType.EXPORT) {
            switch (options) {
                case stop:
                    logView.addImportantLog("停止导出");
                    tvExport.setText("开始导出");
                    tvFileList.setClickable(true);
                    tvRecord.setClickable(true);
                    tvExport.setTag(0);
                    tvRecord.setBackgroundResource(R.drawable.round_corner_blue);
                    transView.setBackgroundColor(getResources().getColor(R.color.transparent));
                    enableRadioGroup(rgAudioSelect);
                    lvCameras.setEnabled(true);
                    break;
                case start:
                    logView.addImportantLog("开始导出");
                    tvExport.setText("停止导出");
                    tvFileList.setClickable(false);
                    tvRecord.setClickable(false);
                    tvExport.setTag(1);
                    tvRecord.setBackgroundResource(R.drawable.round_corner_gray);
                    transView.setBackgroundColor(getResources().getColor(R.color.transparent_40));
                    disableRadioGroup(rgAudioSelect);
                    lvCameras.setEnabled(false);
                    break;
            }
        }
    }

    private void stopExport() {
        if (null != messageRunnable) {
            mHandler.removeCallbacks(messageRunnable);
        }
        videoFrameNum = 0;
        audioFrameNum = 0;
        videoDataSize = 0;
        audioDataSize = 0;
        mTimerUtils.clearTimer();//定时器清除
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mRoom) {
            stopExport();
            mRoom.dispose();
            //退出房间时，已选中视频设为""
            Constants.SELECT_CAMERA_ID = "";
        }
        Log.i(TAG, "onDestory");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 退出
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && KeyEvent.KEYCODE_BACK == keyCode) {
            if (tvExport.isClickable()) {
                AveiDialog.finishDialog(this, "正在导出,是否直接退出？", new AveiDialog.MCallBack() {
                    @Override
                    public boolean OnCallBackDispath(Boolean bSucceed) {
                        finish();
                        return false;
                    }
                });
            } else if (tvRecord.isClickable()) {
                AveiDialog.finishDialog(this, "正在录制,是否直接退出？", new AveiDialog.MCallBack() {
                    @Override
                    public boolean OnCallBackDispath(Boolean bSucceed) {
                        finish();
                        return false;
                    }
                });
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * OnCheckedChangeListener
     *
     * @param group
     * @param checkedId rb_audio_no无音频;rb_audio_one仅所选中视频用户的音频;rb_audio_without_me房间内除自己外的音频;rb_audio_all房间所有音频
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_audio_no:
                audioEIType = AVExportRoom.AudioOptions.record_no_audio;
                logView.addNormalLog("您已选择不录制房间所有的音频");
                break;
            case R.id.rb_audio_one:
                audioEIType = AVExportRoom.AudioOptions.record_one_by_video;
                if (StringUtils.isNotEmpty(maudioUserId)) {
                    String selectOwner = mRoom.mvideo.getUserName(maudioUserId);
                    logView.addNormalLog("您已选择" + selectOwner + "的音频");
                }
                break;
            case R.id.rb_audio_without_me:
                audioEIType = AVExportRoom.AudioOptions.record_all_withoutme;
                logView.addNormalLog("您已选择房间内除自己外的音频");
                break;
            case R.id.rb_audio_all:
                audioEIType = AVExportRoom.AudioOptions.record_all_inroom;
                logView.addNormalLog("您已选择房间内所有用户的音频");
                break;
            default:
                break;
        }
    }


    /*********************设置RadioGroup是否可以点击**************************/
    public void disableRadioGroup(RadioGroup testRadioGroup) {
        for (int i = 0; i < testRadioGroup.getChildCount(); i++) {
            testRadioGroup.getChildAt(i).setEnabled(false);
        }
    }

    public void enableRadioGroup(RadioGroup testRadioGroup) {
        for (int i = 0; i < testRadioGroup.getChildCount(); i++) {
            testRadioGroup.getChildAt(i).setEnabled(true);
        }
    }
}
