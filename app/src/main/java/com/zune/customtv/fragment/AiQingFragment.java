package com.zune.customtv.fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import com.zune.customtv.PlayActivity;
import com.zune.customtv.R;
import com.base.base.BaseApplication;
import com.base.base.BaseFragment;
import com.zune.customtv.bean.AiQing;
import com.zune.customtv.bean.BaseDataBean;
import com.zune.customtv.utils.SSLSocketClient;
import com.zune.customtv.utils.Utils;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    String text = baseDataBean.o.title;
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
        mData.clear();
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    //1.创建一个okhttpclient对象
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())//配置
                            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置
                            .build();
                    //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
                    Request request = new Request.Builder()
                            .url("https://waipian10.com/video/176972/")
                            .build();
                    //3.创建一个Call对象，参数是request对象，发送请求
                    Call call = okHttpClient.newCall(request);
                    Response response = call.execute();
                    if (response != null && response.body() != null) {
                        String string = response.body().string();
                        List<String> keyWords = getKeyWords(string, "<a class=\"module-play-list-link\"", "</a>");
                        Log.i("zune: ", "keyWords = " + keyWords);
                        Collections.reverse(keyWords);
                        for (String keyWord : keyWords) {
                            // href="/play/176972-2-40/" title="播放爱情保卫战202320230118"><span>20230118</span>
                            String url = getKeyWord(keyWord, "href=\"", "\" title=\"");
                            if (url.contains("/play/176972-2")) {
                                String title = getKeyWord(keyWord, "<span>", "</span>");
                                Request request2 = new Request.Builder()
                                        .url("https://waipian10.com" + url)
                                        .build();
                                Call call2 = okHttpClient.newCall(request2);
                                Response response2 = call2.execute();
                                if (response2 != null && response2.body() != null) {
                                    String string2 = response2.body().string();
                                    String json = getKeyWord(string2, "var player_aaaa=", "</script><script type=\"text/javascript\"");
                                    String urlSplit = getKeyWord(json, "\"url\":\"", "\",\"url_next\"");
                                    String decode = URLDecoder.decode(urlSplit, "UTF-8");
                                    String playUrl = decode.split(".m3u8")[0] + ".m3u8";
                                    BaseDataBean.ODTO o = new BaseDataBean.ODTO();
                                    o.firstPublished = "";
                                    o.title = title;
                                    o.key = playUrl;
                                    BaseDataBean b = new BaseDataBean();
                                    b.o = o;
                                    mData.add(b);
                                    refreshUi(mData.size() - 1, mData.size());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 取出在before 和 after之间的字符串
     *
     * @param string
     * @param before
     * @param after
     * @return
     */
    protected static List<String> getKeyWords(String string, String before, String after) {
        List<String> list = new ArrayList<>();
        String p = before + "(.*?)" + after;
        Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string.trim());
        while (matcher.find()) {
            String group = matcher.group();
            if (!TextUtils.isEmpty(group)) {
                String substring = group.substring(before.length());
                list.add(substring.substring(0, substring.length() - after.length()));
            }
        }
        return list;
    }

    /**
     * 取出在before 和 after之间的字符串
     *
     * @param string
     * @param before
     * @param after
     * @return
     */
    protected static String getKeyWord(String string, String before, String after) {
        String p = before + "(.*?)" + after;
        Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string.trim());
        while (matcher.find()) {
            String group = matcher.group();
            if (!TextUtils.isEmpty(group)) {
                String substring = group.substring(before.length());
                return substring.substring(0, substring.length() - after.length());
            }
        }
        return "";
    }

    private void loadData2() {
        mData.clear();
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    //1.创建一个okhttpclient对象
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())//配置
                            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置
                            .build();
                    //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
                    Request request = new Request.Builder()
                            .url("https://v.qq.com/x/search/?q=爱情保卫战&stag=0&smartbox_ab=")
                            .build();
                    //3.创建一个Call对象，参数是request对象，发送请求
                    Call call = okHttpClient.newCall(request);
                    Response response = call.execute();
                    if (response != null && response.body() != null) {
                        String string = response.body().string();
                        String[] split = string.split("\n");
                        for (String s : split) {
                            if (s.contains("<div class=\"item\"><a href=")) {
                                String hrefUrl = s.split("<a href=\"")[1].split("\" target=")[0];
                                String hrefTitle = s.split(" title=\"")[1].split("\" dt-eid=")[0];
                                if (TextUtils.isEmpty(hrefUrl) || hrefTitle.contains("查看更多")) {
                                    continue;
                                }
                                BaseDataBean.ODTO o = new BaseDataBean.ODTO();
                                o.firstPublished = "";
                                o.title = hrefTitle;
                                o.key = hrefUrl;
                                BaseDataBean b = new BaseDataBean();
                                b.o = o;
                                mData.add(b);
                            }
                        }
                        loadNextPage(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void refreshUi(int start, int count) {
        mHandler.post(() -> {
            if (recyclerView != null && recyclerView.getAdapter() != null) {
                recyclerView.getAdapter().notifyItemRangeChanged(start, count);
            }
        });
    }

    private void loadNextPage(int page) {
        try {
            String url = "https://pbaccess.video.qq.com/trpc.videosearch.search_cgi.http/load_playsource_list_info?pageNum=" + page +
                    "&id=mzc00200zixidqy" +
                    "&dataType=2" +
                    "&pageContext=need_async%3Dtrue%26offset_begin%3D5" +
                    "&scene=3" +
                    "&platform=2" +
                    "&appId=10718" +
                    "&site=qq" +
                    "&vappid=34382579" +
                    "&vsecret=e496b057758aeb04b3a2d623c952a1c47e04ffb0a01e19cf" +
                    "&g_tk=" +
                    "&g_vstk=" +
                    "&g_actk=";
            //1.创建一个okhttpclient对象
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())//配置
                    .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置
                    .build();
            //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            //3.创建一个Call对象，参数是request对象，发送请求
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response != null && response.body() != null) {
                String string = response.body().string();
                try {
                    AiQing aiQing = BaseApplication.getInstance().getGson().fromJson(string, AiQing.class);
                    List<BaseDataBean> temp = new ArrayList<>();
                    for (AiQing.DataDTO.NormalListDTO.ItemListDTO.VideoInfoDTO.FirstBlockSitesDTO.EpisodeInfoListDTO episodeInfoListDTO : aiQing.data.normalList.itemList.get(0).videoInfo.firstBlockSites.get(0).episodeInfoList) {
                        String hrefUrl = episodeInfoListDTO.url;
                        String hrefTitle = episodeInfoListDTO.title;
                        if (TextUtils.isEmpty(hrefUrl) || hrefTitle.contains("查看更多")) {
                            continue;
                        }
                        BaseDataBean.ODTO o = new BaseDataBean.ODTO();
                        o.firstPublished = "";
                        o.title = hrefTitle;
                        o.key = hrefUrl;
                        BaseDataBean b = new BaseDataBean();
                        b.o = o;
                        temp.add(b);
                    }
                    if (!temp.isEmpty()) {
                        mData.addAll(temp);
                        refreshUi(mData.size() - temp.size(), temp.size());
                        loadNextPage(++page);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_ai_qing;
    }
}
