package com.zune.customtv.base;

public interface CallBack<T> {
    void onResponse(T t);
    void onFail(String fail);
}
