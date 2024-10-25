package com.zune.customtv.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.base.base.BaseApplication;

/**
 * Created by cxf on 2018/9/17.
 * SharedPreferences 封装
 */

public class SpUtil {

    private static SpUtil sInstance;
    private SharedPreferences mSharedPreferences;

    private SpUtil() {
        mSharedPreferences = BaseApplication.getInstance().getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
    }

    public static SpUtil getInstance() {
        if (sInstance == null) {
            synchronized (SpUtil.class) {
                if (sInstance == null) {
                    sInstance = new SpUtil();
                }
            }
        }
        return sInstance;
    }

    /**
     * 保存一个字符串
     */
    public void setStringValue(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * 获取一个字符串
     */
    public String getStringValue(String key) {
        return mSharedPreferences.getString(key, "");
    }
    public String getStringValue(String key, String value) {
        return mSharedPreferences.getString(key, value);
    }


    /**
     * 保存一个布尔值
     */
    public void setBooleanValue(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * 获取一个布尔值
     */
    public boolean getBooleanValue(String key) {
        return mSharedPreferences.getBoolean(key, false);
    }

    /**
     * 获取一个布尔值
     */
    public boolean getBooleanValue(String key, Boolean b) {
        return mSharedPreferences.getBoolean(key, b);
    }

    /**
     * 获取一个Long值
     */
    public Long getLongValue(String key, long defaultVal) {
        return mSharedPreferences.getLong(key, defaultVal);
    }

    /**
     * 保存一个Long值
     */
    public void setLongValue(String key, long value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * 保存一个int
     */
    public void setIntValue(String key, int value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void clear(){
        mSharedPreferences.edit().clear().apply();
    }

    /**
     * 获取一个int
     */
    public int getIntValue(String key) {
        return getIntValue(key, 0);
    }
    public int getIntValue(String key, int value) {
        return mSharedPreferences.getInt(key, value);
    }

}
