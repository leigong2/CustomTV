package com.zune.customtv;

import com.base.base.BuildConfig;
import com.google.gson.Gson;
import com.base.base.BaseApplication;
import com.base.base.BaseConstant;
import com.base.base.CallBack;
import com.zune.customtv.bean.BaseDataBean;
import com.zune.customtv.utils.GzipUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetDataManager {
    public static final List<BaseDataBean> sBaseData = new ArrayList<>();
    public static void getBaseData(CallBack<List<BaseDataBean>> callBack) {
        File file = new File(BaseApplication.getInstance().getExternalFilesDir(null), "baseData.json.gz");
        if (BuildConfig.DEBUG && file.exists()) {
            GzipUtils.unGzip(file, file.getParent());
            parseJson(new File(file.getParent(), file.getName().replaceAll(".gz", "")), callBack);
            return;
        }
        //1.创建一个okhttpclient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
        Request request = new Request.Builder()
                .url(BaseConstant.URL_BASE_DATA)
                .build();
        //3.创建一个Call对象，参数是request对象，发送请求
        Call call = okHttpClient.newCall(request);
        //4.异步请求，请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                BaseApplication.getInstance().getHandler().post(() -> callBack.onFail(e.toString()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //得到从网上获取资源，转换成我们想要的类型
                InputStream is = null;
                byte[] buf = new byte[1024];
                int len = 0;
                FileOutputStream fos = null;
                File file = new File(BaseApplication.getInstance().getExternalFilesDir(null), "baseData.json.gz");
                if (!file.exists()) {
                    file.createNewFile();
                }
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中更新进度条
                    }
                    fos.flush();
                    System.out.println("下载完成");
                    //下载完成
                } catch (Exception e) {
                    e.printStackTrace();
                    BaseApplication.getInstance().getHandler().post(() -> callBack.onFail(e.toString()));
                    return;
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        BaseApplication.getInstance().getHandler().post(() -> callBack.onFail(e.toString()));
                        return;
                    }
                }
                GzipUtils.unGzip(file, file.getParent());
                parseJson(new File(file.getParent(), file.getName().replaceAll(".gz", "")), callBack);
            }
        });
    }

    private static void parseJson(File file, CallBack<List<BaseDataBean>> callBack) {
        BufferedReader fileReader = null;
        Gson gson = new Gson();
        List<BaseDataBean> dataBeans = new ArrayList<>();
        try {
            fileReader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = fileReader.readLine()) != null) {
                try {
                    BaseDataBean baseDataBean = gson.fromJson(line, BaseDataBean.class);
                    dataBeans.add(baseDataBean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            BaseApplication.getInstance().getHandler().post(() -> callBack.onFail(e.toString()));
            return;
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    BaseApplication.getInstance().getHandler().post(() -> callBack.onFail(e.toString()));
                    return;
                }
            }
        }
        BaseApplication.getInstance().getHandler().post(() -> callBack.onResponse(dataBeans));
    }
}
