package com.zune.customtv.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;

/**
 * @author wangzhilong
 * @date 2022/7/29 029
 * @link @KeyEvent
 */
public class YaoKongUtils {

    public static void back() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Message message = Message.obtain();
                message.what = KeyEvent.KEYCODE_BACK;
                mHandler.sendMessage(message);
            }
        }.start();
    }

    public static void playOrPause() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Message message = Message.obtain();
                message.what = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                mHandler.sendMessage(message);
            }
        }.start();
    }

    private static final Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message paramMessage) {
            switch (paramMessage.what) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:   //播放或暂停
                    execByRuntime("input keyevent 85");
                    break;
                case KeyEvent.KEYCODE_BACK:   //返回键
                    execByRuntime("input keyevent 4");
                    break;
            }
        }
    };


    public static void execByRuntime(String cmd) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                try {
                    process.destroy();
                } catch (Throwable t) {
                    //
                }
            }
        }
    }
}
