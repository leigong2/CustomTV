package com.base.base;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.multidex.MultiDexApplication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class BaseApplication extends MultiDexApplication {
    private static BaseApplication sApplication;
    private static Handler mHandler;
    private static Gson mGson;

    public Activity topActivity;

    public static BaseApplication getInstance() {
        return sApplication;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public Gson getGson() {
        return mGson;
    }

    public MutableLiveData<Integer> orientation = new MutableLiveData<>();

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        mHandler = new Handler(Looper.getMainLooper());
        mGson = new GsonBuilder().create();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {}
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityResumed(@NonNull Activity activity) {topActivity = activity;}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {}
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        registerReceiver(new OrientationReceiver(),intentFilter);
    }
    private static class OrientationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Integer value = BaseApplication.getInstance().orientation.getValue();
            BaseApplication.getInstance().orientation.postValue(((value == null ? 0 : value) + 90) % 180);
        }
    }
}
