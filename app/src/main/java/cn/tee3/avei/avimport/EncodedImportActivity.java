package cn.tee3.avei.avimport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Arrays;

import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.FakeVideoCapturer;
import cn.tee3.avd.MVideo;
import cn.tee3.avd.PublishVideoOptions;
import cn.tee3.avd.Room;
import cn.tee3.avd.VideoOptions;
import cn.tee3.avei.R;
import cn.tee3.avei.avroom.AVRoom;
import cn.tee3.avei.utils.EventLogView;
import cn.tee3.avei.utils.FilesUtils;
import cn.tee3.avei.utils.TimerUtils;

import static java.lang.Thread.sleep;

/**
 * 编码后的数据导入
 * Created by shengf on 2017/6/16.
 */

public class EncodedImportActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "EncodedImportActivity";
    private TextView tvTitle;//标题
    private TextView tvImport;//导入
    private TextView tvImportTime;//导入的时间
    private EventLogView logView;
    private TextView tvCodedImport;

    private AVRoom avRoom;
    private MVideo.Camera mFakeCam;
    private FakeVideoCapturer mFakeVideoCapturer;
    private TimerUtils mTimerUtils;

    private boolean isImport = false;
    private int videoFrameNum = 0;//视频上传的帧数
    private long videoDataSize = 0;//已上传视频数据的大小

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avimport_layout);

        Intent intent = getIntent();
        String roomId = intent.getStringExtra("roomId");

        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText("房间号：" + roomId);

        logView = (EventLogView) findViewById(R.id.event_view);
        tvImport = (TextView) findViewById(R.id.tv_import);
        tvImportTime = (TextView) findViewById(R.id.tv_import_time);
        tvCodedImport = (TextView) findViewById(R.id.tv_coded_import);

        tvCodedImport.setVisibility(View.VISIBLE);
        tvImport.setOnClickListener(this);

        mTimerUtils = new TimerUtils(tvImportTime);

        //创建一个虚拟Camera
        mFakeCam = new MVideo.Camera("fcid", "fake camera");
        /******注意，此处为h264编码后数据导入， VideoOptions.VideoCodec必须设置为codec_h264************/
        PublishVideoOptions pubOptions = new PublishVideoOptions(new MVideo.CameraCapability(640, 480, 30), VideoOptions.VideoCodec.codec_h264);
        mFakeCam.setPublishedQualities(pubOptions);

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
                if (null == mFakeVideoCapturer) {
                    mFakeVideoCapturer = FakeVideoCapturer.Create(sourceFVC_listener, FakeVideoCapturer.FourccType.ft_H264, false);
                    //发布改虚拟Camera
                    int ret1 = avRoom.mvideo.publishLocalCamera(mFakeCam, mFakeVideoCapturer);
                    if (0 != ret1) {
                        Log.e(TAG, "publishLocalCamera failed. ret=" + ret1);
                    } else {
                        Log.i(TAG, "publishLocalCamera. ret=" + ret1);
                    }
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
            logView.addEventLog("加入房间失败");
            Log.w(TAG, "check_ret: ret=" + ret);
            avRoom.dispose();
            avRoom = null;
            return false;
        }
        logView.addEventLog("加入房间成功");
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
                    mHandler.postDelayed(messageRunnable, 0);
                }
                break;
            default:
                break;
        }
    }

    private InputStream is;//文件流
    private int buffer_len = 1024 * 16;//每一个缓冲区为16K
    private byte[] h264_video_buffer = new byte[buffer_len];//16K的缓冲区
    private byte[] data = new byte[0];//currData[4]=104、103、6、101时需要拼接后进行上传

    private void startImporter() {
        //视频导入
        is = getResources().openRawResource(R.raw.test);
        logView.addEventLog("开始导入音视频");
        logView.addEventLog("请用幸会加入此房间查看导入的音视频");
        tvCodedImport.setText("正在从文件 test.h264 导入视频数据...");
        isImport = true;
        mTimerUtils.updateTimer();

        mHandler.postDelayed(readH264DataRunnable, 0);
    }

    private Runnable readH264DataRunnable = new Runnable() {
        public void run() {
            if (readH264DataAndImport()) {
                if (isImport) {
                    mHandler.postDelayed(readH264DataRunnable, 30);
                }
            }
        }
    };

    private int currbegin = 0;//开始的头信息所在位置
    private int currend = 0;//结束的头信息所在位置
    private boolean findOneHeader = false;//是否已经找到第一个头信息的位置
    private boolean first = true;

    /**
     * 获取H264数据并上传
     *
     * @return
     */
    public boolean readH264DataAndImport() {
        try {
            int curr = 0;
            int readed = 0;

            if (first) {
                readed = is.read(h264_video_buffer);
                first = false;
            } else {
                System.arraycopy(h264_video_buffer, currend, h264_video_buffer, 0, buffer_len - currend);

                readed = is.read(h264_video_buffer, buffer_len - currend, currend);
                readed = readed + buffer_len - currend;
            }
            findOneHeader = false;
            while (curr < h264_video_buffer.length - 4) {
                if (0 == h264_video_buffer[curr] && 0 == h264_video_buffer[curr + 1] && 0 == h264_video_buffer[curr + 2] && 1 == h264_video_buffer[curr + 3]) {
                    if (!findOneHeader) {
                        findOneHeader = true;
                        currbegin = curr;
                    } else {
                        currend = curr;
                        byte[] currData = Arrays.copyOfRange(h264_video_buffer, currbegin, currend);
                        getImportData(currData);
                        currbegin = curr;
                    }
                }
                ++curr;
            }
            return (readed == buffer_len) ? true : false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * 整理得到的数据
     *
     * @param currData 数据 注意： currData[4]=104、103、6、101时需要拼接后进行上传，其余直接上传
     */
    private void getImportData(byte[] currData) {
        if (103 == currData[4]) { // sps frame
            data = concat(data, currData);
            return;
        }
        if (104 == currData[4]) { // pps frame
            data = concat(data, currData);
            return;
        }
        if (6 == currData[4]) { // 参数帧
            data = concat(data, currData);
            return;
        }
        if (101 == currData[4]) { // 关键帧；sps, pps等参数帧与关键帧一起导入
            data = concat(data, currData);
            //拼接后上传该帧数据
            avImport(data);
            data = new byte[0];
            return;
        } else {                // 非关键帧
            //直接上传该帧数据
            avImport(currData);
        }
    }

    /**
     * 将整理后的数据导入
     *
     * @param importData 上传数据
     */
    private void avImport(byte[] importData) {
        //未保证质量30毫秒传一帧，少于30毫秒进行等待
        long now = System.currentTimeMillis();

        long pts = System.nanoTime();
        int ret = mFakeVideoCapturer.inputEncodedFrame(
                pts, 640, 480, importData, importData.length);
        videoFrameNum = videoFrameNum + 1;
        videoDataSize = videoDataSize + data.length;
        Log.i(TAG, "inputEncodedFrame pts=" + pts + data.length);
        if (0 != ret) {
            Log.i(TAG, "source inputEncodedFrame failed. ret=" + ret);
        }

        now = System.currentTimeMillis() - now;
        if (now < 30) {//小于30ms则等待
            try {
                sleep(30 - now);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 两个数组合并
     *
     * @param a 需要放置在起始的byte数组
     * @param b 需要拼接在后面的byte数组
     * @return result 返回结果：拼接完成的数组
     */
    private byte[] concat(byte[] a, byte[] b) {
        final int alen = a.length;
        final int blen = b.length;
        if (alen == 0) {
            return b;
        }
        if (blen == 0) {
            return a;
        }
        final byte[] result = (byte[]) java.lang.reflect.Array.
                newInstance(a.getClass().getComponentType(), alen + blen);
        System.arraycopy(a, 0, result, 0, alen);
        System.arraycopy(b, 0, result, alen, blen);
        return result;
    }

    private Runnable messageRunnable = new Runnable() {
        public void run() {
            if (isImport) {
                logView.addEventLog("已导入视频" + videoFrameNum + "帧," + FilesUtils.FormetFileSize(videoDataSize));
                mHandler.postDelayed(messageRunnable, 3000);
            }
        }
    };

    /**
     * 停止导入
     */
    private void stopImporter() {
        if (null == avRoom) {
            Log.i(TAG, "stopRoom, room is not created.");
            return;
        }
        logView.addEventLog("停止导入音视频");
        tvCodedImport.setText("请导入视频数据");
        Log.i(TAG, "stopImporter, begin...");
        //视频停止导入
        isImport = false;
        //计时器清除
        mTimerUtils.clearTimer();

        mHandler.removeCallbacks(messageRunnable);
        videoFrameNum = 0;
        videoDataSize = 0;
        Log.i(TAG, "stopRoom, end");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopImporter();
        if (null != avRoom) {
            if (null != mFakeVideoCapturer) {
                FakeVideoCapturer.destoryCapturer(mFakeVideoCapturer);
                mFakeVideoCapturer = null;
            }
            mFakeCam = null;
            avRoom.dispose();
            Log.i(TAG, "onDestory");
        }
    }
}
