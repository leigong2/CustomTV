package com.zhangteng.projectionscreensender.record;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;

import com.zhangteng.projectionscreensender.ScreenService;
import com.zhangteng.projectionscreensender.configuration.VideoConfiguration;
import com.zhangteng.projectionscreensender.controller.StreamController;
import com.zhangteng.projectionscreensender.packer.Packer;
import com.zhangteng.projectionscreensender.sender.Sender;

import java.util.Objects;

/**
 * Created by swing on 2018/8/21.
 */
public class ScreenRecordActivity extends Activity {
    private static final int RECORD_REQUEST_CODE = 101;
    private MediaProjectionManager mMediaProjectionManage;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void requestRecording() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            Log.d("Pro..Sc..Activity", "Device don't support screen recording.");
            return;
        }
        mMediaProjectionManage = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mMediaProjectionManage.createScreenCaptureIntent();
        startActivityForResult(captureIntent, RECORD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Intent service = new Intent(this, com.zhangteng.projectionscreensender.ScreenService.class);
                service.putExtra("code", resultCode);
                service.putExtra("data", data);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
            } else {
                requestRecordFail();
            }
        }
    }

    protected void requestRecordSuccess() {

    }

    protected void requestRecordFail() {

    }

    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.setVideoConfiguration(videoConfiguration);
            }
        } catch (Exception ignore) {
        }
    }

    protected void startRecording() {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.start();
            }
        } catch (Exception ignore) {
        }
    }

    protected void stopRecording() {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.stop();
            }
        } catch (Exception ignore) {
        }
    }

    protected void pauseRecording() {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.pause();
            }
        } catch (Exception ignore) {
        }
    }


    protected void resumeRecording() {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.resume();
            }
        } catch (Exception ignore) {
        }
    }

    protected boolean setRecordBps(int bps) {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                return controller.setVideoBps(bps);
            } else {
                return false;
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    protected void setRecordPacker(Packer packer) {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.setPacker(packer);
            }
        } catch (Exception ignore) {
        }
    }

    protected void setRecordSender(Sender sender) {
        try {
            StreamController controller = Objects.requireNonNull(ScreenService.getSelf()).get().mStreamController;
            if (controller != null) {
                controller.setSender(sender);
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void onDestroy() {
        stopRecording();
        super.onDestroy();
    }
}
