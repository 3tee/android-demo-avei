package cn.tee3.avei.avimport;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.IOException;
import java.io.InputStream;

import cn.tee3.avd.AVDEngine;
import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.FakeAudioCapturer;
import cn.tee3.avd.MAudio;
import cn.tee3.avd.Room;
import cn.tee3.avei.R;
import cn.tee3.avei.avroom.AVRoom;
import cn.tee3.avei.capture.AudioCaptureThread;
import cn.tee3.avei.utils.StringUtils;

import static java.lang.Thread.sleep;

/**
 * 纯音频导入、混音后导出
 * Created by shengf on 2017/7/14.
 */

public class AudioMixExport1 extends Activity implements View.OnClickListener, MAudio.MixerDataListener {
    private static final String TAG = "AudioMixExport1";

    private TextView tvImport;
    private TextView tvMix;
    private TextView tvChange;
    private TextView tvAecChange;
    private AVRoom avRoom;
    private FakeAudioCapturer mFakeAudioCapturer;
    private AudioCaptureThread mRecordingThread;

    private String option = "false";
    private String eo_audio_ae_option = "false";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_mix_layout);
        tvImport = (TextView) findViewById(R.id.tv_import);
        tvMix = (TextView) findViewById(R.id.tv_mix);
        tvChange = (TextView) findViewById(R.id.tv_change);
        tvAecChange = (TextView) findViewById(R.id.tv_aec_change);
        tvImport.setOnClickListener(this);
        tvMix.setOnClickListener(this);
        tvChange.setOnClickListener(this);
        tvAecChange.setOnClickListener(this);
        WebRtcAudioUtils.enableBuiltInAEC(false);
        startUpVideo("r8");
    }

    private void startUpVideo(String roomId) {
        // step1: 加入房间
        avRoom = new AVRoom(roomId);
        avRoom.room.setOption(Room.Option.ro_audio_option_codec, "opus");
        setAVDEngineOption();
//        setAVDEngineAudioAec();
        int ret = avRoom.join(StringUtils.getUUID(), "androidUser" + (int) (Math.random() * 100000000), new Room.JoinResultListener() {
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
//        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_aec_Enable, option);
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_noiseSuppression_Enable, option);
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_highpassFilter_Enable, option);
        //AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_autoGainControl_Enable, option);
        //AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_aec_DAEcho_Enable, option);
    }

    private void setAVDEngineAudioAec() {
        AVDEngine.instance().setOption(AVDEngine.Option.eo_audio_aec_Enable, eo_audio_ae_option);
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
                    tvChange.setText("声音处理已打开，点击关闭");
                } else {
                    option = "false";
                    tvChange.setText("声音处理已关闭，点击打开");
                }
                setAVDEngineOption();
                avRoom.room.setOption(Room.Option.ro_room_options_apply, "audio_options");
                Log.i(TAG, "option:" + option);
                break;
            case R.id.tv_aec_change:
                if (eo_audio_ae_option.equals("false")) {
                    eo_audio_ae_option = "true";
                    tvAecChange.setText("回音消除已打开，点击关闭");
                } else {
                    eo_audio_ae_option = "false";
                    tvAecChange.setText("回音消除已关闭，点击打开");
                }
                setAVDEngineAudioAec();
                avRoom.room.setOption(Room.Option.ro_room_options_apply, "audio_options");
                Log.i(TAG, "eo_audio_ae_option:" + eo_audio_ae_option);
                break;
        }
    }

    private int samplerate = 44100;
    private int channels = 2;
    private int pcmsize = samplerate / 100 * channels * 2;

    private void startImporter() {
        mFakeAudioCapturer.enable(true);
        avRoom.maudio.openMicrophone();
        //音频导入
        if (null == mRecordingThread) {
            mRecordingThread = new AudioCaptureThread(new AudioCaptureThread.AudioDataListener() {
                @Override
                public void onAudioData(long timestamp_ns, int sampleRate, int channels, byte[] data, int len) {
                    int ret = mFakeAudioCapturer.inputCapturedFrame(timestamp_ns, sampleRate, channels, data, len);
                    if (0 != ret) {
                        Log.e(TAG, "source inputCapturedFrame failed. ret=" + ret);
                    }
                }
            }, samplerate, channels);
            mRecordingThread.start();
        }
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
        if (null != mFakeAudioCapturer) {
            FakeAudioCapturer.destoryCapturer(mFakeAudioCapturer);
            mFakeAudioCapturer = null;
        }
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
