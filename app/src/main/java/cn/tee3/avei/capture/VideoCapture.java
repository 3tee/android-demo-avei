package cn.tee3.avei.capture;

import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class VideoCapture implements PreviewCallback {

    private static final String TAG = VideoCapture.class.getSimpleName();

    public static final int CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static final int CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;

    public static final int MAX_CALLBACK_BUFFER_NUM = 3;

    private Camera mCamera;
    private CameraSurfaceCallback mCameraSurfaceCallback;
    private OnPreviewFrameCallback mOnPreviewFrameCallback;
    private int mOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private int mCameraFacing = CAMERA_FACING_FRONT;

    public VideoCapture() {
        mCameraSurfaceCallback = new CameraSurfaceCallback();
    }

    public int getmOrientation() {
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            return 0;
        } else {
            return 270;
        }
    }

    public boolean isFrontCamera() {
        return mCameraFacing == CAMERA_FACING_FRONT;
    }

    public interface OnPreviewFrameCallback {
        void onPreviewFrameCaptured(int width, int height, byte[] data);
    }

    public void openCamera(int cameraFacing, int width, int height, int orientation) {
        Log.i(TAG, "openCamera, cameraFacing=" + cameraFacing + ",w=" + width + ",h=" + height + ",orientation=" + orientation);
        if (mCamera != null) {
            return;
        }

        mCameraFacing = cameraFacing;
        mPreviewWidth = width;
        mPreviewHeight = height;
        mOrientation = orientation;

        mCamera = Camera.open(cameraFacing);

        showSupportedPreviewSizes();
        showSupportedFormats();

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mCamera.setDisplayOrientation(0);
        } else {
            mCamera.setDisplayOrientation(90);
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewSize(width, height);
        mCamera.setParameters(params);

        final Size previewSize = params.getPreviewSize();
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;

        final int bitsPerPixel = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
        final int previewBufferSize = (previewSize.width * previewSize.height * bitsPerPixel) / 8;
        for (int i = 0; i < MAX_CALLBACK_BUFFER_NUM; i++) {
            mCamera.addCallbackBuffer(new byte[previewBufferSize]);
        }

        mCamera.setPreviewCallbackWithBuffer(this);
        Log.i(TAG, "openCamera");
    }

    public void switchCamera() {

        if (mCamera == null) {
            return;
        }

        stopPreview();
        closeCamera();

        //切换Camera
        if (mCameraFacing == CAMERA_FACING_BACK) {
            mCameraFacing = CAMERA_FACING_FRONT;
        } else {
            mCameraFacing = CAMERA_FACING_BACK;
        }

        openCamera(mCameraFacing, mPreviewWidth, mPreviewHeight, mOrientation);
        startPreview();
        Log.i(TAG, "switchCamera");
    }

    public void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        Log.i(TAG, "closeCamera");
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public void setOnPreviewFrameCallback(OnPreviewFrameCallback callback) {
        mOnPreviewFrameCallback = callback;
    }

    public void showSupportedPreviewSizes() {
        List<Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        for (Size size : sizes) {
            Log.d(TAG, "Supported preview size:" + size.width + "x" + size.height);
        }
    }

    public void showSupportedFormats() {
        List<Integer> formats = mCamera.getParameters().getSupportedPreviewFormats();
        for (Integer format : formats) {
            Log.d(TAG, "Supported YUV format idx: " + format);
        }
    }

    public SurfaceHolder.Callback getCallback() {
        return mCameraSurfaceCallback;
    }

    public Surface getSurface() {
        return mCameraSurfaceCallback.mSurfaceHolder.getSurface();
    }

    protected void startPreview() {
        if (mCamera == null) {
            return;
        }
        Log.i(TAG, "startPreview");
        try {
            mCamera.setPreviewDisplay(mCameraSurfaceCallback.mSurfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void stopPreview() {
        if (mCamera == null) {
            return;
        }
        Log.i(TAG, "stopPreview");
        mCamera.stopPreview();
    }

    public void destroyCamera() {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private class CameraSurfaceCallback implements SurfaceHolder.Callback {

        private SurfaceHolder mSurfaceHolder;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceHolder = holder;
            startPreview();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPreview();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        // 默认的格式是NV21
        // 格式可以通过setPreviewFormat(ImageFormat.NV21)来修改

        if (mOnPreviewFrameCallback != null) {
            mOnPreviewFrameCallback.onPreviewFrameCaptured(mPreviewWidth, mPreviewHeight, data);
        }

        if (mCamera != null) {
            mCamera.addCallbackBuffer(data);
        }
    }
}
