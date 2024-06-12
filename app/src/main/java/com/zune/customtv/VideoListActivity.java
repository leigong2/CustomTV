package com.zune.customtv;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.base.base.BaseActivity;
import com.zune.customtv.bean.BaseDataBean;
import com.zune.customtv.utils.MediaTag2MediaUrl;
import com.zune.customtv.utils.MemoryCache;
import com.zune.customtv.utils.Utils;

import java.util.ArrayList;

public class VideoListActivity extends BaseActivity {

    private BaseDataBean.Subcategories subcategories;

    public static void start(Context context, BaseDataBean.Subcategories subcategories) {
        Intent intent = new Intent(context, VideoListActivity.class);
        MemoryCache.getInstance().put("subcategories", subcategories);
        context.startActivity(intent);
    }

    @Override
    protected void initView() {
        subcategories = MemoryCache.getInstance().remove("subcategories");
        initData();
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_middle, parent, false)) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setPadding(0, 0, 0, 0);
                TextView title = holder.itemView.findViewById(R.id.title);
                RecyclerView recyclerView = holder.itemView.findViewById(R.id.recycler_view);
                setDatas(title, recyclerView, subcategories.subcategories.get(position));
            }

            @Override
            public int getItemCount() {
                return subcategories.subcategories.size();
            }
        });
    }

    private void initData() {
        for (BaseDataBean.Subcategories subcategory : subcategories.subcategories) {
            subcategory.mTitles = new ArrayList<>();
            subcategory.mImages = new ArrayList<>();
            subcategory.mDurations = new ArrayList<>();
            subcategory.mFirstPublished = new ArrayList<>();
            for (String ignored : subcategory.media) {
                subcategory.mTitles.add(null);
                subcategory.mImages.add(null);
                subcategory.mDurations.add(null);
                subcategory.mFirstPublished.add(null);
            }
        }
        for (BaseDataBean dataBean : NetDataManager.sBaseData) {
            if (TextUtils.isEmpty(dataBean.o.naturalKey)) {
                continue;
            }
            for (BaseDataBean.Subcategories subcategory : subcategories.subcategories) {
                for (int i = 0; i < subcategory.media.size(); i++) {
                    String s = subcategory.media.get(i);
                    String naturalKey = dataBean.o.naturalKey;
                    if (s.equals(naturalKey)) {
                        subcategory.mTitles.set(i, dataBean.o.title);
                        subcategory.mImages.set(i, dataBean.o.getThumb());
                        subcategory.mDurations.set(i, String.valueOf(dataBean.o.duration));
                        subcategory.mFirstPublished.set(i, String.valueOf(dataBean.o.firstPublished));
                    }
                }
            }
        }
    }

    private void setDatas(TextView title, RecyclerView recyclerView, BaseDataBean.Subcategories subcategories) {
        title.setText(subcategories.name);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_video, parent, false)) {
                };
                viewHolder.itemView.setOnFocusChangeListener((v, hasFocus) -> v.setBackgroundResource(hasFocus ? R.drawable.bg_select : R.drawable.bg_normal));
                viewHolder.itemView.setOnClickListener(v -> {
                    int position = (int) v.getTag();
                    ArrayList<String> urls = new ArrayList<>();
                    for (int i = position; i < position + 10; i++) {
                        if (i >= subcategories.media.size()) {
                            break;
                        }
                        urls.add(MediaTag2MediaUrl.tag2Url(subcategories.media.get(i)));
                    }
                    PlayActivity.start(v.getContext(), urls);
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setTag(position);
                ImageView imageView = holder.itemView.findViewById(R.id.image_view);
                TextView textView = holder.itemView.findViewById(R.id.text_name);
                TextView duration = holder.itemView.findViewById(R.id.duration);
                Glide.with(imageView).load(subcategories.mImages.get(position)).into(imageView);
                String text = subcategories.mFirstPublished.get(position) + "\n" + subcategories.mTitles.get(position);
                textView.setText(text);
                duration.setText(Utils.getDurationDate(subcategories.mDurations.get(position)));
            }

            @Override
            public int getItemCount() {
                return subcategories.media.size();
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_list_video;
    }
}
