package cn.tee3.avei;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PreviewSurface extends SurfaceView {

    public PreviewSurface(Context context) {
        super(context);
        setWillNotDraw(false);
        setKeepScreenOn(true);
    }

    public PreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setKeepScreenOn(true);
    }

    public void setCallback(SurfaceHolder.Callback callback) {
        getHolder().addCallback(callback);
    }
}
