package cn.tee3.avei.avroom;

import android.util.Log;

import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.MLocalRecord;
import cn.tee3.avd.MVideo;
import cn.tee3.avd.Room;
import cn.tee3.avd.VideoOptions;
import cn.tee3.avei.utils.StringUtils;

/**
 * 本地导出、录制房间
 * Created by shengf on 2017/6/1.
 */

public class AVExportRoom extends AVRoom {
    private static final String TAG = "AVExportRoom";
    protected MLocalRecord mlocalRecord;
    protected String mRecorderId;

    public AVExportRoom(String roomId, CameraPublishListener listener) {
        super(roomId, listener);
    }

    @Override
    public void dispose() {
        super.dispose();
        mlocalRecord = null;
    }

    @Override
    public int join(String userId, String userName, Room.JoinResultListener joinResult) {
        callret = super.join(userId, userName, joinResult);
        if (ErrorCode.AVD_OK == callret) {
            mlocalRecord = MLocalRecord.getRecord(room);
        }
        return callret;
    }

    @Override
    public void leave() {
        super.leave();
        if (null != mlocalRecord) {
            mlocalRecord.stopRecorderAll();
            mlocalRecord = null;
        }
    }

    /**
     * 暂停录制和导出
     */
    public synchronized void stopRecordOrExport() {
        if (null != mlocalRecord) {
            mlocalRecord.stopRecorderAll();
            mRecorderId = null;
        }
    }

    //录制音频选项
    public static enum AudioOptions {
        record_no_audio,        //不录制音频
        record_one_by_video,    //录制选中的视频
        record_all_withoutme,   //录制除自己外的音频
        record_all_inroom       //录制所有音频
    }

    /**
     * 本地录制
     *
     * @param filePath      文件路径
     * @param videoDeviceId 所选视频的设备id
     * @param audioUserId   音频用户id
     * @param option        录音选择
     */
    public boolean startLocalRecord(String filePath, String videoDeviceId, String audioUserId, AudioOptions option) {
        Log.i(TAG, "startLocalRecord filePath=" + filePath
                + ",videoDeviceId=" + videoDeviceId + ",audioUserId=" + audioUserId + ",option=" + option);
        // file type from: video codec
        MVideo.Camera camera = mvideo.getCamera(videoDeviceId);
        VideoOptions.VideoCodec videoCodec = camera.getPublishedQualities().getStreamCodec(VideoOptions.StreamType.stream_main);
        if (VideoOptions.VideoCodec.codec_h264.equals(videoCodec.name())) {//通过编码参数来选择录制的文件类型
            filePath = filePath + ".mp4";
        } else {
            filePath = filePath + ".webm";
        }
        // create recorder
        mRecorderId = mlocalRecord.createRecorder(filePath, "");
        return startRecord(mRecorderId, videoDeviceId, audioUserId, option);
    }

    /**
     * 导出音视频
     *
     * @param videoDeviceId 所选视频的设备id
     * @param audioUserId   音频用户id
     * @param option        录音选择
     */
    public boolean startLocalExport(MLocalRecord.StreamOutListener listener, String videoDeviceId, String audioUserId, AudioOptions option) {
        // create recorder
        mRecorderId = mlocalRecord.createRecorder2(listener, "", false);
        return startRecord(mRecorderId, videoDeviceId, audioUserId, option);
    }

    /**
     * 录制导出
     *
     * @param mRecorderId   录制容器id
     * @param videoDeviceId 所选视频的设备id
     * @param audioUserId   音频用户id
     * @param option        录音选择
     * @return
     */
    boolean startRecord(String mRecorderId, String videoDeviceId, String audioUserId, AudioOptions option) {
        if (StringUtils.isEmpty(mRecorderId)) {
            Log.e(TAG, "startRecord createRecorder failed.");
            return false;
        }

        // select audio for recorder
        switch (option) {
            case record_no_audio:
                break;
            case record_one_by_video:
                mlocalRecord.selectAudio4Recorder(mRecorderId, audioUserId);
                break;
            case record_all_withoutme:
                mlocalRecord.selectAllAudioWithoutMe4Recorder(mRecorderId);
                break;
            case record_all_inroom:
                mlocalRecord.selectAllAudio4Recorder(mRecorderId);
                break;
            default:
                break;
        }
        // select video for recorder
        Log.i(TAG, "localRecord, mRecorderId" + mRecorderId);
        if (StringUtils.isNotEmpty(mRecorderId)) {
            mlocalRecord.selectVideo4Recorder(mRecorderId, videoDeviceId);
            if (!mvideo.isCameraSubscribed(videoDeviceId)) {
                mvideo.subscribe(videoDeviceId);
            }
        }
        return true;
    }
}
