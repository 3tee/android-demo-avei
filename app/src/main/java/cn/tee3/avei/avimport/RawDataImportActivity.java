package cn.tee3.avei.avimport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.FakeAudioCapturer;
import cn.tee3.avd.FakeVideoCapturer;
import cn.tee3.avd.MVideo;
import cn.tee3.avd.PublishVideoOptions;
import cn.tee3.avd.Room;
import cn.tee3.avd.VideoOptions;
import cn.tee3.avei.PreviewSurface;
import cn.tee3.avei.R;
import cn.tee3.avei.avroom.AVRoom;
import cn.tee3.avei.capture.AudioCaptureThread;
import cn.tee3.avei.capture.VideoCapture;
import cn.tee3.avei.utils.EventLogView;
import cn.tee3.avei.utils.FilesUtils;
import cn.tee3.avei.utils.TimerUtils;

/**
 * 音视频原始数据导入
 * Created by shengf on 2017/6/15.
 */

public class RawDataImportActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "RawDataImportActivity";
    private TextView tvTitle;//标题
    private TextView tvImport;//导入
    private TextView tvImportTime;//导入的时间
    private EventLogView logView;
    private PreviewSurface mLocalPreviewSurface;

    private AVRoom avRoom;
    private VideoCapture mVideoCapture;
    private MVideo.Camera mFakeCam;
    private FakeAudioCapturer mFakeAudioCapturer;
    private FakeVideoCapturer mFakeVideoCapturer;
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
        mFakeCam = new MVideo.Camera("fcid", "fake camera");
        PublishVideoOptions pubOptions = new PublishVideoOptions(new MVideo.CameraCapability(640, 480, 30), VideoOptions.VideoCodec.codec_h264);
        mFakeCam.setPublishedQualities(pubOptions);

        mVideoCapture = new VideoCapture();
        mLocalPreviewSurface = (PreviewSurface) findViewById(R.id.ps_local);
        mLocalPreviewSurface.setVisibility(View.VISIBLE);
        mLocalPreviewSurface.setCallback(mVideoCapture.getCallback());
        mVideoCapture.openCamera(VideoCapture.CAMERA_FACING_FRONT, 640, 480,
                getResources().getConfiguration().orientation);

        startUpVideo(roomId);
    }

    private void startUpVideo(String roomId) {
        Log.i(TAG, "startUpVideo");
        // step1: 加入房间
        avRoom = new AVRoom(roomId);

        int ret = avRoom.join("testuserId", "test_username", new Room.JoinResultListener() {
            @Override
            public void onJoinResult(int result) {
                if (ErrorCode.AVD_OK != result) {
                    check_ret(result);
                    return;
                }
                if (null == mFakeAudioCapturer) {
                    mFakeAudioCapturer = FakeAudioCapturer.instance();
                }
                if (null == mFakeVideoCapturer) {
                    mFakeVideoCapturer = FakeVideoCapturer.Create(sourceFVC_listener, FakeVideoCapturer.FourccType.ft_NV21, false);
                }
            }
        });
        check_ret(ret);
    }

    private FakeVideoCapturer.Listener sourceFVC_listener = new FakeVideoCapturer.Listener() {
        @Override
        public void onStart() {
            Log.i(TAG, "sourceFVC onStart ");
        }

        @Override
        public void onStop() {
            Log.i(TAG, "sourceFVC onStop ");
        }
    };

    boolean check_ret(int ret) {
        if (ErrorCode.AVD_OK != ret) {
            logView.addVeryImportantLog("加入房间失败：ErrorCode=" + ret);
            Log.w(TAG, "check_ret: ret=" + ret);
            avRoom.dispose();
            avRoom = null;
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
                    stopImporter();
                } else {
                    TimerUtils.updatePrefixLabel("导入时长 ");
                    tvImport.setText("停止导入");
                    startImporter();
                    mHandler.postDelayed(messageRunnable, 3000);
                }
                break;
            default:
                break;
        }
    }

    private void startImporter() {
        //发布虚拟Camera
        int ret = avRoom.mvideo.publishLocalCamera(mFakeCam, mFakeVideoCapturer);
        if (0 != ret) {
            Log.e(TAG, "publishLocalCamera failed. ret=" + ret);
            logView.addVeryImportantLog("发布虚拟摄像头失败：ErrorCode=" + ret);
        }
        //启用麦克风
        mFakeAudioCapturer.enable(true);
        avRoom.maudio.openMicrophone();

        logView.addImportantLog("开始导入音视频");
        logView.addVeryImportantLog("请用幸会加入此房间查看导入的音视频");
        isImport = true;
        mTimerUtils.updateTimer();
        mVideoCapture.setOnPreviewFrameCallback(mOnPreviewFrameCallback);
        //音频导入
        if (null == mRecordingThread) {
            mRecordingThread = new AudioCaptureThread(new AudioCaptureThread.AudioDataListener() {
                @Override
                public void onAudioData(long timestamp_ns, int sampleRate, int channels, byte[] data, int len) {
                    int ret = mFakeAudioCapturer.inputCapturedFrame(timestamp_ns, sampleRate, channels, data, len);
                    audioFrameNum = audioFrameNum + 1;
                    audioDataSize = audioDataSize + len;
                    if (0 != ret) {
                        Log.e(TAG, "source inputCapturedFrame failed. ret=" + ret);
                        logView.addVeryImportantLog("音频导入失败：ErrorCode=" + ret);
                    }
                }
            });
            mRecordingThread.start();
        }
    }

    //视频导入
    private VideoCapture.OnPreviewFrameCallback mOnPreviewFrameCallback = new VideoCapture.OnPreviewFrameCallback() {
        @Override
        public void onPreviewFrameCaptured(int width, int height, byte[] data) { // 摄像头原始图像导入
            if (null != mFakeVideoCapturer && mFakeVideoCapturer.isRunning()) {
                if (isImport) {
                    Log.v(TAG, "inputCapturedFrame mwidth=" + width + ",mheight=" + height + "" + mVideoCapture.getmOrientation());
                    long pts = System.nanoTime();
                    int ret = mFakeVideoCapturer.inputCapturedFrame(
                            pts, width, height, data, data.length,
                            mVideoCapture.getmOrientation(), mVideoCapture.isFrontCamera());
                    videoFrameNum = videoFrameNum + 1;
                    videoDataSize = videoDataSize + data.length;
                    Log.i(TAG, "inputCapturedFrame pts=" + pts);
                    if (0 != ret) {
                        Log.e(TAG, "source inputCapturedFrame failed. ret=" + ret);
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

    private void stopImporter() {
        //取消发布虚拟Camera
        avRoom.mvideo.unpublishLocalCamera(mFakeCam.getId());
        //关闭麦克风//此处不设置 mFakeAudioCapturer.enable也不影响
        mFakeAudioCapturer.enable(false);
        avRoom.maudio.closeMicrophone();

        if (null == avRoom) {
            Log.i(TAG, "stopRoom, room is not created.");
            return;
        }
        logView.addImportantLog("停止导入音视频");
        Log.i(TAG, "stopImporter, begin...");
        //视频停止导入
        if (null != mVideoCapture) {
            isImport = false;
        }
        //计时器清除
        mTimerUtils.clearTimer();
        //音频停止
        if (null != mRecordingThread) {
            mRecordingThread.stopMe();
            mRecordingThread = null;
        }
        mHandler.removeCallbacks(messageRunnable);
        videoFrameNum = 0;
        audioFrameNum = 0;
        videoDataSize = 0;
        audioDataSize = 0;
        Log.i(TAG, "stopRoom, end");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoCapture.destroyCamera();
        stopImporter();
        if (null != avRoom) {
            if (null != mFakeVideoCapturer) {
                FakeVideoCapturer.destoryCapturer(mFakeVideoCapturer);
                mFakeVideoCapturer = null;
            }
            if (null != mFakeAudioCapturer) {
                FakeAudioCapturer.destoryCapturer(mFakeAudioCapturer);
                mFakeAudioCapturer = null;
            }
            mFakeCam = null;
            avRoom.dispose();
            Log.i(TAG, "onDestory");
        }
    }
}
