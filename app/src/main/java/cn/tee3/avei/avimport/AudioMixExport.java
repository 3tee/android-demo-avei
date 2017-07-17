package cn.tee3.avei.avimport;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import cn.tee3.avd.AVDEngine;
import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.FakeAudioCapturer;
import cn.tee3.avd.MAudio;
import cn.tee3.avd.Room;
import cn.tee3.avei.R;
import cn.tee3.avei.avroom.AVRoom;

import static java.lang.Thread.sleep;

/**
 * 纯音频导入、混音后导出
 * Created by shengf on 2017/7/14.
 */

public class AudioMixExport extends Activity implements View.OnClickListener, MAudio.MixerDataListener {
    private static final String TAG = "AudioMixExport";

    private TextView tvImport;
    private TextView tvMix;
    private TextView tvChange;
    private AVRoom avRoom;
    private FakeAudioCapturer mFakeAudioCapturer;

    private Handler mHandler = new Handler();

    private String option = "false";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_mix_layout);
        tvImport = (TextView) findViewById(R.id.tv_import);
        tvMix = (TextView) findViewById(R.id.tv_mix);
        tvChange = (TextView) findViewById(R.id.tv_change);
        tvImport.setOnClickListener(this);
        tvMix.setOnClickListener(this);
        tvChange.setOnClickListener(this);

        startUpVideo("r7");
    }

    private void startUpVideo(String roomId) {
        // step1: 加入房间
        avRoom = new AVRoom(roomId);
        avRoom.room.setOption(Room.Option.ro_audio_option_codec, "opus");
//        setAVDEngineOption();
        int ret = avRoom.join("testuserId", "test_username", new Room.JoinResultListener() {
            @Override
            public void onJoinResult(int result) {
                if (ErrorCode.AVD_OK != result) {
                    check_ret(result);
                    return;
                }
                if (null == mFakeAudioCapturer) {
                    mFakeAudioCapturer = FakeAudioCapturer.instance();
                    mFakeAudioCapturer.enable(true);
                }
            }
        });
        check_ret(ret);
    }

    private void setAVDEngineOption() {
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_aec_Enable, option);
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_noiseSuppression_Enable, option);
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_highpassFilter_Enable, option);
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_autoGainControl_Enable, option);
        //AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_aec_DAEcho_Enable, option);
    }

    boolean check_ret(int ret) {
        if (ErrorCode.AVD_OK != ret) {
            avRoom.dispose();
            avRoom = null;
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_import://导入
                startImporter();
                break;
            case R.id.tv_mix://混音
                startMix();
                break;
            case R.id.tv_change:
                if (option.equals("false")) {
                    option = "true";
                } else {
                    option = "false";
                }
                setAVDEngineOption();
                avRoom.room.setOption(Room.Option.ro_room_options_apply, "audio_options");
                Log.i(TAG, "option:" + option);
                break;
        }
    }

    private InputStream is;//文件流
    private int samplerate = 48000;
    private int channels = 2;
    private int pcmsize = samplerate / 100 * channels * 2;
    private byte[] pcm_buffer = new byte[pcmsize];//缓冲区

    private void startImporter() {
       // is = getResources().openRawResource(R.raw.audio_test);
        mFakeAudioCapturer.enable(true);
        avRoom.maudio.openMicrophone();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    long ret = readPcmData();
                    try {
                        sleep(ret);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private Runnable readPcmRunnable = new Runnable() {
        public void run() {
            long ret = readPcmData();
            mHandler.postDelayed(readPcmRunnable, ret);
        }
    };

    private long readPcmData() {
        long now = System.currentTimeMillis();
        try {
            is.read(pcm_buffer);
            importPcm(pcm_buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        now = System.currentTimeMillis() - now;
        if (now < 10) {//小于10ms则等待
            now = 10 - now;
        } else {
            now = 0;
        }
        return now;
    }

    private void importPcm(byte[] data) {
        long pts = System.nanoTime();
        int ret = mFakeAudioCapturer.inputCapturedFrame(pts, samplerate, channels, data, pcmsize);
        if (0 != ret) {
            Log.i(TAG, "inputCapturedFrame failed. ret=" + ret + data.length);
        }
    }

    private void startMix() {
        avRoom.maudio.setMixerDataListener(this, 44100, 2);
//        avRoom.maudio.setMixerDataListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(readPcmRunnable);
        if (null != avRoom) {
            avRoom.dispose();
            Log.i(TAG, "onDestory");
        }
    }

    /*********************MAudio.MixerDataListener*********************/
    @Override
    public void onAudioParam(int sampleRate, int channels) {
        Log.i(TAG, "-onAudioParam-" + "-sampleRate-" + sampleRate + "-channels:" + channels);
    }

    @Override
    public void onAudioData(long timestamp_ns, byte[] buf, int len) {
        Log.i(TAG, "-onAudioData-" + timestamp_ns + "-buf.length-" + buf.length + "-len:" + len);
    }
    /*********************MAudio.MixerDataListener*********************/
}
