package com.zune.customtv.fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zune.customtv.NetDataManager;
import com.zune.customtv.R;
import com.zune.customtv.VideoListActivity;
import com.zune.customtv.base.BaseFragment;
import com.zune.customtv.bean.BaseDataBean;

public class VideoFragment extends BaseFragment {

    private BaseDataBean mVideoData;
    private RecyclerView recyclerView;

    @Override
    protected void initView(View view) {
        for (BaseDataBean baseDataBean : NetDataManager.sBaseData) {
            if (baseDataBean.o != null && "VideoOnDemand".equals(baseDataBean.o.key)) {
                mVideoData = baseDataBean;
                break;
            }
        }
        if (mVideoData == null || mVideoData.o == null || mVideoData.o.subcategories == null) {
            return;
        }
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false)) {
                };
                viewHolder.itemView.setOnFocusChangeListener((v, hasFocus) -> v.setBackgroundResource(hasFocus ? R.drawable.bg_select : R.drawable.bg_normal));
                viewHolder.itemView.setOnClickListener(v -> {
                    int position = (int) v.getTag();
                    BaseDataBean.Subcategories subcategories = mVideoData.o.subcategories.get(position);
                    Log.i("zune: ", "subcategories： " + subcategories.toString());
                    VideoListActivity.start(v.getContext(), subcategories);
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView imageView = holder.itemView.findViewById(R.id.image_view);
                TextView textView = holder.itemView.findViewById(R.id.text_view);
                Glide.with(imageView).load(mVideoData.o.subcategories.get(position).getThumb()).into(imageView);
                textView.setText(mVideoData.o.subcategories.get(position).name);
                holder.itemView.setTag(position);
            }

            @Override
            public int getItemCount() {
                return mVideoData.o.subcategories.size();
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_video;
    }
}
