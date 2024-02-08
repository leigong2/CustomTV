package com.zune.customtv;

import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.upstream.cache.CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.base.base.BaseActivity;
import com.base.base.BaseApplication;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheEvictor;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.zune.customtv.bean.AiQingResponse;
import com.zune.customtv.bean.Mp4Bean;
import com.zune.customtv.utils.SSLSocketClient;
import com.zune.customtv.utils.SurfaceControllerView;
import com.zune.customtv.utils.YaoKongUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlayActivity extends BaseActivity {

    public static final String ua = Util.getUserAgent(BaseApplication.getInstance(), "AppName");
    private SimpleExoPlayer player;
    private Mp4Bean mMp4Bean;
    private int currentPosition;
    private TextView tvChangeVideo;
    private ArrayList<String> mediaUrls;
    private int mCurrentPosition;
    private MediaPlayer mediaPlayer;
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), (X509TrustManager) SSLSocketClient.getTrustManager()[0])
            .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
            .build();

    public static void start(Context context, ArrayList<String> mediaUrl) {
        Intent intent = new Intent(context, PlayActivity.class);
        intent.putStringArrayListExtra("mediaUrl", mediaUrl);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_play;
    }

    @Override
    protected void initView() {
//        initWebView(webView);
//        webView.loadUrl("https://jx2022.laobandq.com/jiexi20210115/8090.php?url=https://v.qq.com/x/cover/mzc00200zixidqy/p0046443rsg.html");
        findViewById(R.id.back).setOnClickListener(v -> YaoKongUtils.back());
        findViewById(R.id.play).setOnClickListener(v -> YaoKongUtils.playOrPause());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tvChangeVideo = findViewById(R.id.tv_change_video);
        mediaUrls = getIntent().getStringArrayListExtra("mediaUrl");
        if (mediaUrls != null && mediaUrls.size() == 1 && !mediaUrls.get(0).contains("app.jw")) {
            if (mediaUrls.get(0).endsWith(".m3u8")) {
                startPlayByVideoView(mediaUrls.get(0));
                return;
            }
            parseThird(mediaUrls.get(0));
            return;
        }
        playWithUrl(mCurrentPosition);
    }

    private void parseThird(String s) {
        WebView webView = findViewById(R.id.web_view);
        initWebView(webView);
        webView.loadUrl("https://jx.jsonplayer.com/player/?url=" + s);
    }

    private void parseFirst(String s) {
        try {
            //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
            Request request0 = new Request.Builder()
                    .url("https://1717yun.com.zh188.net/0907/index.php?url=" + s)
                    .addHeader("referer", "https://1717yun.com.zh188.net/0828/?url=" + s)
                    .build();
            //3.创建一个Call对象，参数是request对象，发送请求
            Call call0 = okHttpClient.newCall(request0);
            Response response0 = call0.execute();
            if (response0 != null && response0.body() != null) {
                String string0 = response0.body().string();
                String referer = getKeyWords(string0, "'referer':'", "','ref'");
                String url = getKeyWords(string0, "'url':'", "','referer'");
                // https://1717yun.com.zh188.net/0907/api.php
                String other = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    other = Base64.getEncoder().encodeToString(url.getBytes());
                }
                RequestBody body = new FormBody.Builder()
                        .add("ios", "")
                        .add("other", other)
                        .add("ref", "0")
                        .add("referer", referer)
                        .add("time", String.valueOf(System.currentTimeMillis() / 1000))
                        .add("type", "")
                        .add("url", url)
                        .build();
                Request request3 = new Request.Builder()
                        .url("https://1717yun.com.zh188.net/20220722/..index..php")
                        .post(body)
                        .build();
                //3.创建一个Call对象，参数是request对象，发送请求
                Call call3 = okHttpClient.newCall(request3);
                Response response3 = call3.execute();
                if (response3 != null && response3.body() != null) {
                    String string3 = response3.body().string();
                    AiQingResponse aiQingResponse = BaseApplication.getInstance().getGson().fromJson(string3, AiQingResponse.class);
                    String realMp4Url = aiQingResponse.url;
                    BaseApplication.getInstance().getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            startPlay(realMp4Url, true);
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            parseSecond(mediaUrls.get(0));
        }
    }

    private void parseSecond(String s) {
        try {
            //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
            Request request0 = new Request.Builder()
                    .url("https://jx2022.laobandq.com/20221120/?url=" + s)
                    .addHeader("referer", "https://jx2022.laobandq.com/jiexi20210115/8090.php?url=" + s)
                    .build();
            //3.创建一个Call对象，参数是request对象，发送请求
            Call call0 = okHttpClient.newCall(request0);
            Response response0 = call0.execute();
            if (response0 != null && response0.body() != null) {
                String string0 = response0.body().string();
                String referer = getKeyWords(string0, "'referer':'", "','ref'");
                String url = getKeyWords(string0, "'url':'", "','referer'");
                // https://1717yun.com.zh188.net/0907/api.php
                String other = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    other = Base64.getEncoder().encodeToString(url.getBytes());
                }
                RequestBody body = new FormBody.Builder()
                        .add("ios", "")
                        .add("other", other)
                        .add("ref", "0")
                        .add("referer", referer)
                        .add("time", String.valueOf(System.currentTimeMillis() / 1000))
                        .add("type", "")
                        .add("url", url)
                        .build();
                Request request3 = new Request.Builder()
                        .url("https://jx2022.laobandq.com/20221120/%E3%80%80%E3%80%80.%E3%80%80.php")
                        .post(body)
                        .build();
                //3.创建一个Call对象，参数是request对象，发送请求
                Call call3 = okHttpClient.newCall(request3);
                Response response3 = call3.execute();
                if (response3 != null && response3.body() != null) {
                    String string3 = response3.body().string();
                    AiQingResponse aiQingResponse = BaseApplication.getInstance().getGson().fromJson(string3, AiQingResponse.class);
                    String realMp4Url = aiQingResponse.url;
                    BaseApplication.getInstance().getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            startPlayByVideoView(realMp4Url);
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取出在before 和 after之间的字符串
     *
     * @param string
     * @param before
     * @param after
     * @return
     */
    protected static String getKeyWords(String string, String before, String after) {
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

    private void startPlayByVideoView(String url) {
        TextureView videoView = findViewById(R.id.video_view);
        SurfaceControllerView videoController = findViewById(R.id.video_controller);
        videoView.setVisibility(View.VISIBLE);
        videoController.setVisibility(View.VISIBLE);
        mediaPlayer = new MediaPlayer();
        videoController.setIjkMediaPlayer(mediaPlayer);
//        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("sec-ch-ua", "\"Google Chrome\";v=\"113\", \"Chromium\";v=\"113\", \"Not-A.Brand\";v=\"24\"");
            headers.put("User-Agent", ua);
            headers.put("sec-ch-ua-mobile", "?0");
            headers.put("sec-ch-ua-platform", "\"Windows\"");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Sec-Fetch-Site", "none");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-User", "?1");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            mediaPlayer.setDataSource(BaseApplication.getInstance(), Uri.parse(url), headers);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PlaybackParams params = new PlaybackParams();
                params.setSpeed(1);
                params.setPitch(1);
                mediaPlayer.setPlaybackParams(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (videoView.getSurfaceTexture() == null) {
                    return;
                }
                mp.setSurface(new Surface(videoView.getSurfaceTexture()));
                mp.start();
                videoController.startPlay();
                refreshVideoSize(videoView, (MediaPlayer) mp);
            }
        });
    }


    public void refreshVideoSize(TextureView textureView, MediaPlayer ijkMediaPlayer) {
        int videoWidth = ijkMediaPlayer.getVideoWidth();
        int videoHeight = ijkMediaPlayer.getVideoHeight();
        int surfaceWidth = getScreenWidth();
        int surfaceHeight = getScreenHeight();
        if (surfaceWidth > surfaceHeight) {
            textureView.getLayoutParams().width = (int) (videoWidth * 1f / videoHeight * surfaceHeight);
            textureView.getLayoutParams().height = surfaceHeight;
        } else {
            textureView.getLayoutParams().width = surfaceWidth;
            textureView.getLayoutParams().height = (int) (videoHeight * 1f / videoWidth * surfaceWidth);
        }
        textureView.setVisibility(View.VISIBLE);
    }

    /**
     * Return the width of screen, in pixel.
     *
     * @return the width of screen, in pixel
     */
    public static int getScreenWidth() {
        WindowManager wm = (WindowManager) BaseApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return BaseApplication.getInstance().getResources().getDisplayMetrics().widthPixels;
        }
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            wm.getDefaultDisplay().getSize(point);
        }
        return point.x;
    }

    /**
     * Return the height of screen, in pixel.
     *
     * @return the height of screen, in pixel
     */
    public static int getScreenHeight() {
        WindowManager wm = (WindowManager) BaseApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return BaseApplication.getInstance().getResources().getDisplayMetrics().heightPixels;
        }
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            wm.getDefaultDisplay().getSize(point);
        }
        return point.y;
    }

    private void initWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setAllowFileAccess(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(false);
//        settings.setAppCachePath(APP_CACHE_DIRNAME);
        //        settings.setAppCachePath(APP_CACHE_DIRNAME);
        settings.setUseWideViewPort(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        settings.setSaveFormData(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        webView.setVisibility(View.VISIBLE);
        webView.setWebViewClient(new MyWebViewClient());
    }

    static class MyWebViewClient extends WebViewClient {
        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (url.startsWith("https://110.42.2.247:9092")) {
                view.loadUrl("javascript:(function(){" +
                        "var objs = document.getElementById(\"yzmplayer-icon yzmplayer-play-icon\");" +
                        "document.getElementsByClassName('yzmplayer-controller')[0].style.marginBottom='-40px';" +
                        " objs.click(); "+
                        "})()");
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            return super.onRenderProcessGone(view, detail);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    private void playWithUrl(int position) {
        if (position >= mediaUrls.size()) {
            return;
        }
        //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
        Request request = new Request.Builder()
                .url(mediaUrls.get(position))
                .build();
        //3.创建一个Call对象，参数是request对象，发送请求
        Call call = okHttpClient.newCall(request);
        //4.异步请求，请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("zune: ", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.body() == null) {
                    return;
                }
                try {
                    String string = new String(response.body().bytes());
                    Log.i("zune: ", string);
                    Mp4Bean mp4Bean = BaseApplication.getInstance().getGson().fromJson(string, Mp4Bean.class);
                    BaseApplication.getInstance().getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            String mp4VideoUri = getMp4Url(mp4Bean);
                            startPlay(mp4VideoUri, false);
                        }
                    });
                } catch (Exception e) {
                    playWithUrl(++mCurrentPosition);
                }
            }
        });
    }

    private boolean mIsPlaying;

    Runnable r = new Runnable() {
        @Override
        public void run() {
            if (isDestroyed() || isFinishing() || mIsPlaying) {
                return;
            }
            changeSmallerSource(player.getCurrentPosition());
        }
    };

    private void startPlay(String mp4VideoUri, boolean withHeader) {
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setVisibility(View.VISIBLE);
        SimpleExoPlayer.Builder builder = new SimpleExoPlayer.Builder(BaseApplication.getInstance());
        player = builder.build();
        if (withHeader) {
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, ua);
            File cacheFile = new File(getCacheDir(), "video");
            CacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(20 * 1024 * 1024);
            DatabaseProvider databaseProvider = new ExoDatabaseProvider(this);
            Cache cache = new SimpleCache(cacheFile, evictor, databaseProvider);
            CacheDataSourceFactory cacheFactory = new CacheDataSourceFactory(cache, dataSourceFactory, FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS);
            ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheFactory)
                    .setDrmHttpDataSourceFactory(new HttpDataSource.Factory() {
                        @Override
                        public HttpDataSource createDataSource() {
                            return new DefaultHttpDataSource("");
                        }

                        @Override
                        public HttpDataSource.RequestProperties getDefaultRequestProperties() {
                            return null;
                        }

                        @Override
                        public HttpDataSource.Factory setDefaultRequestProperties(@NonNull Map<String, String> headers) {
                            headers.put("sec-ch-ua", "\"Google Chrome\";v=\"113\", \"Chromium\";v=\"113\", \"Not-A.Brand\";v=\"24\"");
                            headers.put("User-Agent", ua);
                            headers.put("sec-ch-ua-mobile", "?0");
                            headers.put("sec-ch-ua-platform", "\"Windows\"");
                            headers.put("Upgrade-Insecure-Requests", "1");
                            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                            headers.put("Sec-Fetch-Site", "none");
                            headers.put("Sec-Fetch-Mode", "navigate");
                            headers.put("Sec-Fetch-User", "?1");
                            headers.put("Sec-Fetch-Dest", "document");
                            headers.put("Accept-Encoding", "gzip, deflate, br");
                            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
                            return this;
                        }
                    })
                    .createMediaSource(MediaItem.fromUri(mp4VideoUri));
            player.setMediaSource(mediaSource, true);
        } else {
            MediaItem mediaItem = MediaItem.fromUri(mp4VideoUri);
            player.setMediaItem(mediaItem);
        }
        PlaybackParameters params = new PlaybackParameters(1,1);
        player.setPlaybackParameters(params);
        playerView.setPlayer(player);
        player.prepare();
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            private boolean isPlayed;
            private boolean isPlayEnd;

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == STATE_ENDED) {
                    if (withHeader) {
                        return;
                    }
                    System.out.println("zune: onPlaybackStateChanged = " + playbackState);
                    mIsPlaying = true;
                    isPlayEnd = true;
                    player.removeListener(this);
                    playWithUrl(++mCurrentPosition);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlayEnd) {
                    System.out.println("zune: isPlayEnd = " + true);
                    return;
                }
                mIsPlaying = isPlaying;
                System.out.println("zune: isPlaying = " + isPlaying);
                if (withHeader) {
                    return;
                }
                if (isPlaying) {
                    tvChangeVideo.setText("");
                    isPlayed = true;
                    BaseApplication.getInstance().getHandler().removeCallbacks(r);
                } else if (isPlayed && currentPosition > 0) {
                    BaseApplication.getInstance().getHandler().postDelayed(r, 5000);
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        if (player != null) {
            player.stop();
            player.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        BaseApplication.getInstance().getHandler().removeCallbacks(r);
    }

    private void changeSmallerSource(long currentPosition) {
        Mp4Bean.FilesDTO.CHSDTOX.MP4DTO mp4DTO = mMp4Bean.files.CHS.MP4.get(--this.currentPosition);
        tvChangeVideo.setText(String.format("视频卡顿切换视频源%s", mp4DTO.frameHeight));
        MediaItem item = MediaItem.fromUri(Uri.parse(mp4DTO.file.url));
        MediaSource videoSource = new DefaultMediaSourceFactory(this).createMediaSource(item);
        player.setMediaSources(Collections.singletonList(videoSource));
        player.prepare();
        player.setPlayWhenReady(true);
        player.seekTo(currentPosition);
    }

    private String getMp4Url(Mp4Bean mp4Bean) {
        this.mMp4Bean = mp4Bean;
        this.currentPosition = mMp4Bean.files.CHS.MP4.size() - 1;
//        for (Mp4Bean.FilesDTO.CHSDTOX.MP4DTO mp4DTO : mp4Bean.files.CHS.MP4) {
//            int frameHeight = mp4DTO.frameHeight;
//            try {
//                long date = Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(mp4DTO.subtitles.modifiedDatetime)).getTime();
//                if (System.currentTimeMillis() - date > 4 * 365 * 24 * 60 * 60 * 1000L && frameHeight <= 240) {
//                    return Uri.parse(mp4DTO.file.url);
//                }
//                if (System.currentTimeMillis() - date > 2.5f * 365 * 24 * 60 * 60 * 1000L && frameHeight <= 320) {
//                    return Uri.parse(mp4DTO.file.url);
//                }
//                if (System.currentTimeMillis() - date > 365 * 24 * 60 * 60 * 1000L && frameHeight <= 540) {
//                    return Uri.parse(mp4DTO.file.url);
//                }
//                if (System.currentTimeMillis() - date < 365 * 24 * 60 * 60 * 1000L &&frameHeight > 540) {
//                    return Uri.parse(mp4DTO.file.url);
//                }
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//        }
        Mp4Bean.FilesDTO.CHSDTOX.MP4DTO mp4DTO = mp4Bean.files.CHS.MP4.get(Math.max(0, mp4Bean.files.CHS.MP4.size() - 1));
        System.out.printf("视频卡顿切换视频源%s%n", mp4DTO.frameHeight);
        return mp4DTO.file.url;
    }
}
