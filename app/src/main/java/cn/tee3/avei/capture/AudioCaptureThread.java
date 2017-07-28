package cn.tee3.avei.capture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by shengf on 2017/6/14.
 */
public class AudioCaptureThread extends Thread {
    private static final String TAG = "AudioCaptureThread";
    private final int kEncodeFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int kSampleRate = 16000;
    private int kChannelMode = AudioFormat.CHANNEL_IN_MONO;
    private int kChannels = 1;
    private int kFrameSize = 2 * 16000 / 100; // 10ms data

    private boolean mRunning = false;
    private AudioRecord mAudioRecord;

    public interface AudioDataListener {
        void onAudioData(long timestamp_ns, int sampleRate, int channels, byte[] data, int len);
    }

    AudioDataListener mListener;

    public AudioCaptureThread(AudioDataListener listener) {
        this.mListener = listener;
    }
    public AudioCaptureThread(AudioDataListener listener, int sampleRate, int channels) {
        this.kSampleRate = sampleRate;
        this.kChannels = channels;
        this.kChannelMode = (2 == channels) ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        this.kFrameSize = 2 * kSampleRate * kChannels/ 100;
        this.mListener = listener;
    }

    @Override
    public synchronized void start() {
        if (mRunning) {
            return;
        }
        mRunning = true;
        super.start();
    }

    public void stopMe() {
        mRunning = false;
        this.interrupt();
    }

    @Override
    public void run() {
        int minBufferSize = AudioRecord.getMinBufferSize(kSampleRate, kChannelMode,
                kEncodeFormat);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                kSampleRate, kChannelMode, kEncodeFormat, minBufferSize * 2);
        try {
            mAudioRecord.startRecording();
            Log.d(TAG, "startRecording");
            byte[] bufAudio = new byte[kFrameSize];
            while (mRunning && (!Thread.currentThread().isInterrupted())) {
                int recLen = mAudioRecord.read(bufAudio, 0, kFrameSize);
                if (recLen < kFrameSize || !mRunning) {
                    Log.e(TAG, "read failed. recLen=" + recLen);
                    break;
                }
                if (null != mListener) {
                    mListener.onAudioData(System.nanoTime(), kSampleRate, kChannels, bufAudio, recLen);
                }
            }
            Log.d(TAG, "exit loop");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startRecording or read exception:" + e.toString());
        }
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        Log.d(TAG, "stopRecording, clean up");
    }
}
