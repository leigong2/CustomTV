package com.base.base;

public interface CallBack<T> {
    void onResponse(T t);
    void onFail(String fail);
}
