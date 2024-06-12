package com.zune.customtv.fragment;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.zune.customtv.NetDataManager;
import com.zune.customtv.PlayActivity;
import com.zune.customtv.R;
import com.base.base.BaseConstant;
import com.base.base.BaseFragment;
import com.zune.customtv.bean.BaseDataBean;
import com.zune.customtv.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private final List<BaseDataBean> mResultData = new ArrayList<>();
    private final List<BaseDataBean> mData = new ArrayList<>();
    private RecyclerView recyclerView;

    @Override
    protected void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        EditText editText = view.findViewById(R.id.edit_text);
        View.OnFocusChangeListener l = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundResource(hasFocus ? R.drawable.bg_select : R.drawable.bg_normal);
            }
        };
        editText.setOnFocusChangeListener(l);
        TextView startSearch = view.findViewById(R.id.start_search);
        startSearch.setOnFocusChangeListener(l);
        startSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch(editText.getText().toString());
            }
        });
        TextView allLenth = view.findViewById(R.id.all_lenth);
        allLenth.setOnFocusChangeListener(l);
        allLenth.setOnClickListener(v -> filterData(0));
        TextView min_lenth = view.findViewById(R.id.min_lenth);
        min_lenth.setOnFocusChangeListener(l);
        min_lenth.setOnClickListener(v -> filterData(1));
        TextView middle_lenth = view.findViewById(R.id.middle_lenth);
        middle_lenth.setOnFocusChangeListener(l);
        middle_lenth.setOnClickListener(v -> filterData(2));
        TextView large_lenth = view.findViewById(R.id.large_lenth);
        large_lenth.setOnFocusChangeListener(l);
        large_lenth.setOnClickListener(v -> filterData(3));
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        editText.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm == null) {
                            return false;
                        }
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        startSearch(editText.getText().toString());
                        break;
                }
                return false;
            }
        });
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
                    ArrayList<String> urls  = new ArrayList<>();
                    for (int i = position; i < position + 10; i++) {
                        if (i >= mData.size()) {
                            return;
                        }
                        BaseDataBean dataBean = mData.get(i);
                        StringBuilder sb = new StringBuilder(BaseConstant.URL_GET_MEDIA);
                        if (dataBean.o.keyParts.pubSymbol != null) {
                            sb.append("&pub=").append(dataBean.o.keyParts.pubSymbol);
                        }
                        sb.append("&track=").append(dataBean.o.keyParts.track);
                        if (dataBean.o.keyParts.issueDate != null) {
                            sb.append("&issue=").append(dataBean.o.keyParts.issueDate);
                        }
                        if (dataBean.o.keyParts.docID != null) {
                            sb.append("&docid=").append(dataBean.o.keyParts.docID);
                        }
                        sb.append("&fileformat=").append("mp4%2Cm4v");
                        urls.add(sb.toString());
                    }
                    PlayActivity.start(v.getContext(), urls);
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
                    Glide.with(imageView).load(baseDataBean.o.getThumb()).into(imageView);
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

    private void filterData(int level) {
        switch (level) {
            case 0:
                mData.clear();
                mData.addAll(mResultData);
                break;
            case 1:
                mData.clear();
                for (BaseDataBean result : mResultData) {
                    int d = (int) result.o.duration;
                    if (d < 10 * 60) {
                        mData.add(result);
                    }
                }
                break;
            case 2:
                mData.clear();
                for (BaseDataBean result : mResultData) {
                    int d = (int) result.o.duration;
                    if (d >= 10 * 60 && d < 60 * 60) {
                        mData.add(result);
                    }
                }
                break;
            case 3:
                mData.clear();
                for (BaseDataBean result : mResultData) {
                    int d = (int) result.o.duration;
                    if (d >= 60 * 60) {
                        mData.add(result);
                    }
                }
                break;
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void loadData() {
        List<BaseDataBean> baseData = NetDataManager.sBaseData;
        if (baseData.size() > 200) {
            List<BaseDataBean> newData = baseData.subList(baseData.size() - 200, baseData.size());
            mData.addAll(newData);
            mResultData.addAll(newData);
        } else {
            mResultData.addAll(baseData);
        }
        Collections.reverse(mData);
        Collections.reverse(mResultData);
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void startSearch(String string) {
        mData.clear();
        mResultData.clear();
        String shortString = null;
        try {
            shortString = PinyinHelper.getShortPinyin(string);
        } catch (PinyinException e) {
            e.printStackTrace();
        }
        List<BaseDataBean> datas = new ArrayList<>();
        for (BaseDataBean sBaseDatum : NetDataManager.sBaseData) {
            String text = sBaseDatum.o.firstPublished + "\n" + sBaseDatum.o.title;
            if (text.contains(string)) {
                datas.add(sBaseDatum);
            }
        }
        Collections.reverse(datas);
        mData.addAll(datas);
        mResultData.addAll(datas);
        List<BaseDataBean> shortDatas = new ArrayList<>();
        if (shortString != null) {
            for (BaseDataBean sBaseDatum : NetDataManager.sBaseData) {
                String text = sBaseDatum.o.firstPublished + "\n" + sBaseDatum.o.title;
                try {
                    String shortPinyin = PinyinHelper.getShortPinyin(text);
                    if (shortPinyin.contains(shortString) && !mData.contains(sBaseDatum)) {
                        shortDatas.add(sBaseDatum);
                    }
                } catch (PinyinException e) {
                    e.printStackTrace();
                }
            }
        }
        Collections.reverse(shortDatas);
        mData.addAll(shortDatas);
        mResultData.addAll(shortDatas);
        if (recyclerView.getAdapter() != null) {
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }
}
