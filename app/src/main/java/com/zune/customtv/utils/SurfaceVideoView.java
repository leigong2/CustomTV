package com.zune.customtv.utils;

import static java.sql.DriverManager.println;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class SurfaceVideoView extends TextureView implements TextureView.SurfaceTextureListener {

    public SurfaceVideoView(Context context) {
        super(context);
    }

    public SurfaceVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SurfaceVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addCallBack() {
        setSurfaceTextureListener(this);
    }

    public Function1<SurfaceTexture, Unit> callBack = null;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        if (callBack != null) {
            callBack.invoke(surfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

}
