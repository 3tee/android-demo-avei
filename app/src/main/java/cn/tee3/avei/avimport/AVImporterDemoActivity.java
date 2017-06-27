package cn.tee3.avei.avimport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import cn.tee3.avd.AVImporter;
import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.FakeVideoCapturer;
import cn.tee3.avd.User;
import cn.tee3.avei.PreviewSurface;
import cn.tee3.avei.R;
import cn.tee3.avei.capture.AudioCaptureThread;
import cn.tee3.avei.capture.VideoCapture;
import cn.tee3.avei.view.EventLogView;
import cn.tee3.avei.utils.FilesUtils;
import cn.tee3.avei.utils.TimerUtils;

/**
 * 音视频简单导入
 * Created by shengf on 2017/6/14.
 */

public class AVImporterDemoActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "AVImporterDemoActivity";
    private TextView tvTitle;//标题
    private TextView tvImport;//导入
    private TextView tvImportTime;//导入的时间
    private EventLogView logView;
    private PreviewSurface mLocalPreviewSurface;

    private AVImporter mAVimporter;
    private VideoCapture mVideoCapture;
    private AudioCaptureThread mRecordingThread;
    private TimerUtils mTimerUtils;

    private String roomId = "";
    private boolean isImport = false;
    private int videoFrameNum = 0;//视频上传的帧数
    private int audioFrameNum = 0;//音频上传的帧数（次数）
    private long videoDataSize = 0;//已上传视频数据的大小
    private long audioDataSize = 0;//已上传音频文件的大小

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avimport_layout);

        Intent intent = getIntent();
        roomId = intent.getStringExtra("roomId");

        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText("房间号：" + roomId);

        logView = (EventLogView) findViewById(R.id.event_view);
        tvImport = (TextView) findViewById(R.id.tv_import);
        tvImportTime = (TextView) findViewById(R.id.tv_import_time);

        tvImport.setOnClickListener(this);

        mTimerUtils = new TimerUtils(tvImportTime);

        //加入房间，并初始化设备
        startUpVideo(roomId);
    }

    /**
     * 加入房间，并初始化设备
     *
     * @param roomId 房间号
     */
    private void startUpVideo(String roomId) {
        Log.i(TAG, "startUpVideo");
        mAVimporter = AVImporter.obtain(roomId);
        mAVimporter.setListener(new AVImporter.Listener() {
            @Override
            public void onStatus(int result) {
                Log.i(TAG, "AVImporter onStatus, result=" + result);
            }

            @Override
            public void onError(int reason) {
                Log.i(TAG, "AVImporter onError, reason=" + reason);
            }
        });
        //加入房间
        int ret = mAVimporter.join(new User("testuserId", "test_username", ""), new AVImporter.RoomJoinResultListener() {
                    @Override
                    public void onRoomJoinResult(String roomId, int result) {
                        if (ErrorCode.AVD_OK != result) {
                            check_ret(result);
                            return;
                        }
                    }
                }
        );
        initDevice(ret);
    }

    /**
     * 初始化设备（摄像头麦克风）
     *
     * @param ret
     */
    private void initDevice(int ret) {
        if (check_ret(ret)) {
            mVideoCapture = new VideoCapture();
            mLocalPreviewSurface = (PreviewSurface) findViewById(R.id.ps_local);
            mLocalPreviewSurface.setVisibility(View.VISIBLE);
            mLocalPreviewSurface.setCallback(mVideoCapture.getCallback());
            mVideoCapture.openCamera(VideoCapture.CAMERA_FACING_FRONT, 640, 480,
                    getResources().getConfiguration().orientation);

            mVideoCapture.setOnPreviewFrameCallback(mOnPreviewFrameCallback);
        }
    }

    /**
     * 加入房间是否成功
     *
     * @param ret 加入房间结果返回  ErrorCode.AVD_OK：加入房间成功
     * @return
     */
    private boolean check_ret(int ret) {
        if (ErrorCode.AVD_OK != ret) {
            Log.w(TAG, "check_ret: ret=" + ret);
            logView.addVeryImportantLog("加入房间失败：ErrorCode=" + ret);
            AVImporter.destoryImporter(mAVimporter);
            mAVimporter = null;
            return false;
        }
        logView.addImportantLog("加入房间成功");
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_import://导入
                if (isImport) {
                    tvImport.setText("开始导入");
                    //停用导入音频、导入视频
                    mAVimporter.enableAudio(false);
                    mAVimporter.enableVideo(false);
                    stopImporter();
                } else {
                    TimerUtils.updatePrefixLabel("导入时长 ");
                    tvImport.setText("停止导入");
                    //启用导入音频、导入视频
                    mAVimporter.enableAudio(true);
                    mAVimporter.enableVideo(true);
                    startImporter();
                    mHandler.postDelayed(messageRunnable, 0);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 开始导入音视频
     */
    private void startImporter() {
        logView.addImportantLog("开始导入音视频");
        logView.addVeryImportantLog("请用幸会加入此房间查看导入的音视频");
        isImport = true;
        mTimerUtils.updateTimer();

        //音频导入
        if (null == mRecordingThread) {
            mRecordingThread = new AudioCaptureThread(new AudioCaptureThread.AudioDataListener() {
                @Override
                public void onAudioData(long timestamp_ns, int sampleRate, int channels, byte[] data, int len) {
                    int ret = mAVimporter.audio_inputPCMFrame(timestamp_ns, sampleRate, channels, data, len);
                    if (0 != ret) {
                        Log.e(TAG, "audio_inputPCMFrame failed. ret=" + ret);
                        logView.addVeryImportantLog("音频导入失败：ErrorCode=" + ret);
                    }
                    audioFrameNum = audioFrameNum + 1;
                    audioDataSize = audioDataSize + len;
                }
            });
            mRecordingThread.start();
        }
    }

    //视频导入
    private VideoCapture.OnPreviewFrameCallback mOnPreviewFrameCallback = new VideoCapture.OnPreviewFrameCallback() {
        @Override
        public void onPreviewFrameCaptured(int width, int height, byte[] data) { // 摄像头原始图像导入
            if (null != mAVimporter && mAVimporter.isWorking()) {
                Log.v(TAG, "video_inputRAWFrame mwidth=" + width + ",mheight=" + height + "" + mVideoCapture.getmOrientation());
                if (isImport) {//导入
                    int ret = mAVimporter.video_inputRAWFrame(System.nanoTime(), width, height, data, data.length,
                            mVideoCapture.getmOrientation(), mVideoCapture.isFrontCamera(), FakeVideoCapturer.FourccType.ft_NV21);
                    videoFrameNum = videoFrameNum + 1;
                    videoDataSize = videoDataSize + data.length;
                    if (0 != ret) {
                        Log.e(TAG, "video_inputRAWFrame failed. ret=" + ret);
                        logView.addVeryImportantLog("视频导入失败：ErrorCode=" + ret);
                    }
                }
            }
        }
    };

    private Runnable messageRunnable = new Runnable() {
        public void run() {
            if (isImport) {
                logView.addDetailsLog("已导入视频" + videoFrameNum + "帧," + FilesUtils.FormetFileSize(videoDataSize));
                logView.addDetailsLog("已导入音频" + audioFrameNum + "帧," + FilesUtils.FormetFileSize(audioDataSize));
                mHandler.postDelayed(messageRunnable, 3000);
            }
        }
    };


    /**
     * 停止导入音视频
     */
    private void stopImporter() {
        if (null == mAVimporter) {
            return;
        }
        logView.addImportantLog("停止导入音视频");
        Log.i(TAG, "stopImporter, begin...");
        if (null != mRecordingThread) {
            mRecordingThread.stopMe();
            mRecordingThread = null;
        }
        if (null != mVideoCapture) {
            isImport = false;
        }
        mTimerUtils.clearTimer();
        mHandler.removeCallbacks(messageRunnable);
        videoFrameNum = 0;
        audioFrameNum = 0;
        videoDataSize = 0;
        audioDataSize = 0;
        Log.i(TAG, "stopImporter, end");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoCapture.destroyCamera();
        Log.i(TAG, "onDestory");
        stopImporter();
        AVImporter.destoryImporter(mAVimporter);
    }
}
