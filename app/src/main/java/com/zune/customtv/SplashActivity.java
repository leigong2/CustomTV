package com.zune.customtv;

import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;
import com.zune.customtv.base.BaseActivity;
import com.zune.customtv.base.BaseApplication;
import com.zune.customtv.base.CallBack;
import com.zune.customtv.bean.BaseDataBean;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SplashActivity extends BaseActivity {

    @Override
    protected void initView() {
        String[] abis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abis = Build.SUPPORTED_ABIS;
        } else {
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
        StringBuilder abiStr = new StringBuilder();
        for (String abi : abis) {
            abiStr.append(abi);
            abiStr.append(',');
        }
        Toast.makeText(this, "CPU: " + abiStr, Toast.LENGTH_LONG).show();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        NetDataManager.getBaseData(new CallBack<List<BaseDataBean>>() {
            @Override
            public void onResponse(List<BaseDataBean> baseDataBeans) {
                Collections.sort(baseDataBeans, new Comparator<BaseDataBean>() {
                    @Override
                    public int compare(BaseDataBean o1, BaseDataBean o2) {
                        if (o1 == o2) {
                            return 0;
                        }
                        try {
                            long ot1 = sdf.parse(o1.o.firstPublished.replaceAll("T", " ").replaceAll("Z", "")).getTime();
                            long ot2 = sdf.parse(o2.o.firstPublished.replaceAll("T", " ").replaceAll("Z", "")).getTime();
                            return Long.compare(ot1, ot2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return 0;
                    }
                });
                getSharedPreferences("data", MODE_PRIVATE).edit().putString("baseDataBean"
                        , BaseApplication.getInstance().getGson().toJson(baseDataBeans)).apply();
                NetDataManager.sBaseData.addAll(baseDataBeans);
                MainActivity.start(SplashActivity.this);
                finish();
            }

            @Override
            public void onFail(String fail) {
                String string = getSharedPreferences("data", MODE_PRIVATE).getString("baseDataBean", "");
                if (TextUtils.isEmpty(string)) {
                    Toast.makeText(SplashActivity.this, "网络异常，请重试", Toast.LENGTH_SHORT).show();
                } else {
                    List<BaseDataBean> o = BaseApplication.getInstance().getGson().fromJson(string, new TypeToken<List<BaseDataBean>>() {
                    }.getType());
                    onResponse(o);
                }
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }
}