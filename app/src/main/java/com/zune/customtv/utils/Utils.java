package com.zune.customtv.utils;

import com.zune.customtv.base.BaseApplication;

public class Utils {
    public static String getDurationDate(String duration) {
        int d = (int) Double.parseDouble(duration);
        if (d < 60) {
            return d + "s";
        }
        if (d < 3600) {
            return getTwoBite(d / 60) + ":" + getTwoBite(d % 60);
        }
        return getTwoBite(d / 3600) + ":" + getTwoBite(d / 60 % 60) + ":" + getTwoBite(d % 60);
    }

    private static String getTwoBite(int number) {
        if (number < 10) {
            return "0" + number;
        }
        return String.valueOf(number);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(float dpValue) {
        final float scale = BaseApplication.getInstance().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
