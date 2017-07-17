package cn.tee3.avei.avroom;

import android.util.Log;

import java.util.List;

import cn.tee3.avd.Device;
import cn.tee3.avd.ErrorCode;
import cn.tee3.avd.MAudio;
import cn.tee3.avd.MVideo;
import cn.tee3.avd.Room;
import cn.tee3.avd.RoomInfo;
import cn.tee3.avd.User;
import cn.tee3.avei.utils.StringUtils;

/**
 * Created by shengf on 2017/6/1.
 */

public class AVRoom {
    private static final String TAG = "AVRoom";
    public Room room;
    public MAudio maudio;
    public MVideo mvideo;
    protected int callret;

    public AVRoom(String roomId) {
        room = Room.obtain(roomId);
    }

    public interface CameraPublishListener {
        void CameraPublishEvent(boolean isCameraOpen, MVideo.Camera camera);
    }

    CameraPublishListener mListener;

    public AVRoom(String roomId, CameraPublishListener listener) {
        room = Room.obtain(roomId);
        this.mListener = listener;
    }

    public void dispose() {
        if (null != room) {
            Room.destoryRoom(room);
        }
        room = null;
        maudio = null;
        mvideo = null;
    }

    Room.JoinResultListener mJoinResultListener = null;

    /**
     * 加入房间
     *
     * @param userId   用户id
     * @param userName 用户名
     * @return
     */
    public int join(String userId, String userName, Room.JoinResultListener joinResult) {
        if (null == room) {
            Log.e(TAG, "join, room is null");
            return ErrorCode.Err_Wrong_Status;
        }
        User self = new User(userId, userName, "");
        room.setListener(roomcb);
        callret = room.join(self, "", null);
        if (callret != ErrorCode.AVD_OK) {
            Log.e(TAG, "join, room join failed. ret=" + callret);
            return callret;
        }
        maudio = MAudio.getAudio(room);
        maudio.setListener(maudiocb);
        mvideo = MVideo.getVideo(room);
        mvideo.setListener(mvideocb);
        mJoinResultListener = joinResult;
        return callret;
    }

    /**
     * 获取视频摄像头信息列表
     *
     * @return
     */
    public List<MVideo.Camera> getMVideoCameras() {
        List<MVideo.Camera> publisheds = mvideo.getPublishedCameras();
        for (int i = 0; i < publisheds.size(); i++) {
            publisheds.get(i).setName(mvideo.getOwnerName(publisheds.get(i).getId()) + ":" + publisheds.get(i).getName());
        }
        return publisheds;
    }

    /**
     * 离开房间
     */
    public void leave() {
        if (null == room) {
            return;
        }
        if (null != mvideo) {
            mvideo.setListener(null);
            mvideo = null;
        }
        if (null != maudio) {
            maudio.setListener(null);
            maudio = null;
        }
        room.setListener(null);
        room.leave(ErrorCode.AVD_OK);
        callret = 0;
    }

    /**
     * 获取房间号
     *
     * @return 房间id
     */
    public String getRoomId() {
        if (StringUtils.isNotEmpty(room.getRoomId())) {
            return room.getRoomId();
        } else {
            return "";
        }
    }

    Room.Listener roomcb = new Room.Listener() {
        @Override
        public void onJoinResult(int result) {

            if (null != mJoinResultListener) {
                mJoinResultListener.onJoinResult(result);
            }
        }

        @Override
        public void onLeaveIndication(int reason, String fromId) {
        }

        @Override
        public void onPublicData(byte[] data, int len, String fromId) {
        }

        @Override
        public void onPrivateData(byte[] data, int len, String fromId) {
        }

        @Override
        public void onAppDataNotify(String key, String value) {
        }

        @Override
        public void onRoomStatusNotify(RoomInfo.RoomStatus status) {
        }

        @Override
        public void onConnectionStatus(Room.ConnectionStatus status) {
        }
    };

    MAudio.Listener maudiocb = new MAudio.Listener() {
        @Override
        public void onMicrophoneStatusNotify(Device.DeviceStatus status, String fromUserId) {
        }

        @Override
        public void onAudioLevelMonitorNotify(MAudio.AudioInfo info) {
        }

        @Override
        public void onOpenMicrophoneResult(int result) {
            Log.i(TAG, "onOpenMicrophoneResult, result=" + result);
        }

        @Override
        public void onCloseMicrophoneResult(int result) {
            Log.i(TAG, "onCloseMicrophoneResult, result=" + result);
        }
    };

    MVideo.Listener mvideocb = new MVideo.Listener() {
        @Override
        public void onCameraStatusNotify(Device.DeviceStatus status, String fromId) {
        }

        @Override
        public void onCameraDataNotify(int level, String description, String fromId) {
        }

        @Override
        public void onPublishCameraNotify(MVideo.Camera camera) {
            Log.i(TAG, "onPublishCameraNotify, id:" + camera.getId());
            //视频打开通知
            if (null != mListener) {
                mListener.CameraPublishEvent(true, camera);
            }
        }

        @Override
        public void onUnpublishCameraNotify(MVideo.Camera camera) {
            Log.i(TAG, "onUnpublishCameraNotify, id:" + camera.getId());
            //视频关闭通知
            if (null != mListener) {
                mListener.CameraPublishEvent(false, camera);
            }
        }

        @Override
        public void onSubscribeResult(int result, String fromId) {
            Log.i(TAG, "onSubscribeResult, result: " + result + ",fromId:" + fromId);
        }

        @Override
        public void onUnsubscribeResult(int result, String fromId) {
            Log.i(TAG, "onUnsubscribeResult, result: " + result + ",fromId:" + fromId);
        }

        @Override
        public void onPublishLocalResult(int result, String fromId) {
            Log.i(TAG, "onPublishLocalResult, result: " + result + ",fromId:" + fromId);
        }

        @Override
        public void onUnpublishLocalResult(int result, String fromId) {
            Log.i(TAG, "onUnpublishLocalResult, result: " + result + ",fromId:" + fromId);
        }
    };

}
