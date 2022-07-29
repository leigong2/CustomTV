package com.zune.customtv.fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zune.customtv.PlayActivity;
import com.zune.customtv.R;
import com.zune.customtv.base.BaseFragment;
import com.zune.customtv.bean.BaseDataBean;
import com.zune.customtv.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AiQingFragment extends BaseFragment {

    private final List<BaseDataBean> mData = new ArrayList<>();
    private RecyclerView recyclerView;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home, parent, false)) {
                };
                viewHolder.itemView.setOnFocusChangeListener((v, hasFocus) -> v.setBackgroundResource(hasFocus ? R.drawable.bg_select : R.drawable.bg_normal));
                viewHolder.itemView.setOnClickListener(v -> {
                    int position = (int) v.getTag();
                    String url = mData.get(position).o.key;
                    PlayActivity.start(v.getContext(), new ArrayList<>(Collections.singletonList(url)));
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setTag(position);
                ImageView imageView = holder.itemView.findViewById(R.id.image_view);
                TextView textName = holder.itemView.findViewById(R.id.text_name);
                TextView duration = holder.itemView.findViewById(R.id.duration);
                BaseDataBean baseDataBean = mData.get(position);
                if (baseDataBean.o != null) {
                    if (TextUtils.isEmpty(baseDataBean.o.getThumb())) {
                        imageView.setImageResource(R.mipmap.aiqingbaoweizhan);
                    } else {
                        Glide.with(imageView).load(baseDataBean.o.getThumb()).into(imageView);
                    }
                    String text = baseDataBean.o.firstPublished + "\n" + baseDataBean.o.title;
                    textName.setText(text);
                }
                if (baseDataBean.o != null && baseDataBean.o.duration > 0) {
                    duration.setText(Utils.getDurationDate(String.valueOf(baseDataBean.o.duration)));
                }
            }

            @Override
            public int getItemCount() {
                return mData.size();
            }
        });
        loadData();
    }

    private void loadData() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    //1.创建一个okhttpclient对象
                    OkHttpClient okHttpClient = new OkHttpClient();
                    //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
                    Request request = new Request.Builder()
                            .url("https://www.pianba.tv/html/251021.html")
                            .build();
                    //3.创建一个Call对象，参数是request对象，发送请求
                    Call call = okHttpClient.newCall(request);
                    Response response = call.execute();
                    if (response != null && response.body() != null) {
                        String string = response.body().string();
                        for (String s : string.split("\n")) {
                            if (s.contains("第2022") && s.contains("期") && s.contains(".html")) {
                                String[] split = s.split("[第期]");
                                String name = "第" + split[1] + "期";
                                String[] split1 = s.split("\"");
                                String url = "https://www.pianba.tv" + split1[split1.length - 2];
                                BaseDataBean.ODTO o = new BaseDataBean.ODTO();
                                o.firstPublished = "";
                                o.title = name;
                                o.key = url;
                                BaseDataBean b = new BaseDataBean();
                                b.o = o;
                                mData.add(b);
                            }
                        }
                        Collections.reverse(mData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (recyclerView != null && recyclerView.getAdapter() != null) {
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }
                });
            }
        }.start();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_ai_qing;
    }
}
