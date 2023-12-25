package com.base.base;

import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDexApplication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BaseApplication extends MultiDexApplication {
    private static BaseApplication sApplication;
    private static Handler mHandler;
    private static Gson mGson;

    public static BaseApplication getInstance() {
        return sApplication;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public Gson getGson() {
        return mGson;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        mHandler = new Handler(Looper.getMainLooper());
        mGson = new GsonBuilder().create();
    }
}
