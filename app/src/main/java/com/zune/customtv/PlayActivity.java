package com.zune.customtv;

import static com.google.android.exoplayer2.Player.STATE_ENDED;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.Gson;
import com.zune.customtv.base.BaseActivity;
import com.zune.customtv.base.BaseApplication;
import com.zune.customtv.bean.AiQingParser;
import com.zune.customtv.bean.AiQingResponse;
import com.zune.customtv.bean.Mp4Bean;
import com.zune.customtv.utils.SurfaceControllerView;
import com.zune.customtv.utils.YaoKongUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class PlayActivity extends BaseActivity {

    private SimpleExoPlayer player;
    private Mp4Bean mMp4Bean;
    private int currentPosition;
    private TextView tvChangeVideo;
    private ArrayList<String> mediaUrls;
    private int mCurrentPosition;
    private IjkMediaPlayer mediaPlayer;

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
        findViewById(R.id.back).setOnClickListener(v -> YaoKongUtils.back());
        findViewById(R.id.play).setOnClickListener(v -> YaoKongUtils.playOrPause());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tvChangeVideo = findViewById(R.id.tv_change_video);
        mediaUrls = getIntent().getStringArrayListExtra("mediaUrl");
        if (mediaUrls != null && mediaUrls.size() == 1 && !mediaUrls.get(0).contains("app.jw")) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        //1.创建一个okhttpclient对象
                        OkHttpClient okHttpClient = new OkHttpClient();
                        //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
                        Request request = new Request.Builder()
                                .url("https://1717yun.com.zh188.net/0526/?url=" + mediaUrls.get(0))
                                .build();
                        //3.创建一个Call对象，参数是request对象，发送请求
                        Call call = okHttpClient.newCall(request);
                        Response response = call.execute();
                        if (response != null && response.body() != null) {
                            String string = response.body().string();
                            for (String s : string.split("\n")) {
                                if (s.contains("api.php?url=")) {
                                    String realWebUrl = s.split("src=\"")[1];
                                    //1.创建一个okhttpclient对象
                                    OkHttpClient okHttpClient2 = new OkHttpClient.Builder()
                                            .followRedirects(false)
                                            .build();
                                    //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
                                    String url = "https://1717yun.com.zh188.net" + realWebUrl.trim().substring(0, realWebUrl.length() - 2);
                                    Request request2 = new Request.Builder()
                                            .url(url)
                                            .build();
                                    //3.创建一个Call对象，参数是request对象，发送请求
                                    Call call2 = okHttpClient2.newCall(request2);
                                    Response response2 = call2.execute();
                                    if (response2 != null && response2.body() != null) {
                                        String string2 = response2.body().string();
                                        String[] split = string2.split("\n");
                                        for (String s1 : split) {
                                            if (s1.contains("skin")) {
                                                //("..index..php",{'url':'https://v.qq.com/x/cover/mzc00200nu3kdtu.html','referer':'','ref':form,'time':'1659159576','type':'','other':y.encode(other_l),'ref':form,'ios':''},function(data)
                                                //{'url':'https://v.qq.com/x/cover/mzc00200nu3kdtu.html','referer':'','ref':form,'time':'1659159576','type':'','other':y.encode(other_l),'ref':form,'ios':''}
                                                String s2 = s1.split("\\.\\.index\\.\\.php\",")[1].split(",function\\(data\\)\\{if\\(data.code==\"200\"\\)")[0];
                                                String paramsJson = s2.replaceAll("\'", "\"").replaceAll("form", "\"0\"")
                                                        .replaceAll("y\\.encode\\(other_l\\)", "\"\"");
                                                AiQingParser aiQingParser = BaseApplication.getInstance().getGson().fromJson(paramsJson, AiQingParser.class);
                                                //1.创建一个okhttpclient对象
                                                OkHttpClient okHttpClient3 = new OkHttpClient.Builder()
                                                        .followRedirects(false)
                                                        .build();
                                                //2.创建Request.Builder对象，设置参数，请求方式如果是Get，就不用设置，默认就是Get
                                                RequestBody body = new FormBody.Builder()
                                                        .add("ios", aiQingParser.ios)
                                                        .add("other", aiQingParser.other)
                                                        .add("ref", aiQingParser.ref)
                                                        .add("referer", aiQingParser.referer)
                                                        .add("time", aiQingParser.time)
                                                        .add("type", aiQingParser.type)
                                                        .add("url", aiQingParser.url)
                                                        .build();
                                                Request request3 = new Request.Builder()
                                                        .url("https://1717yun.com.zh188.net/20220722/..index..php")
                                                        .post(body)
                                                        .build();
                                                //3.创建一个Call对象，参数是request对象，发送请求
                                                Call call3 = okHttpClient3.newCall(request3);
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
                                                break;
                                            }
                                        }
                                        System.out.println(string2);
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(BaseApplication.getInstance(), "加载出错：" + BaseApplication.getInstance().getGson().toJson(e), Toast.LENGTH_SHORT).show();
                    }
                }
            }.start();
            return;
        }
        playWithUrl(mCurrentPosition);
    }

    private void startPlayByVideoView(String url) {
        TextureView videoView = findViewById(R.id.video_view);
        SurfaceControllerView videoController = findViewById(R.id.video_controller);
        videoView.setVisibility(View.VISIBLE);
        videoController.setVisibility(View.VISIBLE);
        mediaPlayer = new IjkMediaPlayer();
        videoController.setIjkMediaPlayer(mediaPlayer);
        mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setOnPreparedListener(new IjkMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mp.setSurface(new Surface(videoView.getSurfaceTexture()));
                mp.start();
                videoController.startPlay();
                refreshVideoSize(videoView, (IjkMediaPlayer) mp);
            }
        });
    }


    public void refreshVideoSize(TextureView textureView, IjkMediaPlayer ijkMediaPlayer) {
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
        settings.setAppCacheEnabled(true);
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
            view.loadUrl(
                    "javascript:(       function() {\n" +
                            "       document.getElementsByClassName('panel')[0].style.marginLeft='20px';\n" +
                            "       document.getElementsByClassName('slide')[0].style.marginLeft='20px';\n" +
                            "       document.getElementsByClassName('panel')[0].style.marginTop='20px';\n" +
                            "       document.getElementsByClassName('slide')[0].style.marginTop='20px';\n" +
                            "       document.getElementsByClassName('OK-jiexi')[0].style.backgroundColor='#0000';\n" +
                            "       document.getElementsByClassName('OK-jiexi')[0].style.color ='#FFF';\n" +
                            "       document.getElementsByClassName('OK-jiexi')[0].style.opacity ='0.4';\n" +
                            "       document.body.getElementsByTagName('div')[1].style.marginTop='-56px';\n" +
                            "       document.getElementById('bofang').click();\n" +
                            "       })()"
            );
        }
    }

    private void playWithUrl(int position) {
        if (position >= mediaUrls.size()) {
            return;
        }
        //1.创建一个okhttpclient对象
        OkHttpClient okHttpClient = new OkHttpClient();
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
                            startPlayByVideoView(mp4VideoUri);
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

    private void startPlay(String mp4VideoUri) {
        PlayerView playerView = findViewById(R.id.player_view);
        player = new SimpleExoPlayer.Builder(BaseApplication.getInstance()).build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(mp4VideoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            private boolean isPlayed;
            private boolean isPlayEnd;

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == STATE_ENDED) {
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
                if (isPlaying) {
                    tvChangeVideo.setText("");
                    isPlayed = true;
                    BaseApplication.getInstance().getHandler().removeCallbacks(r);
                } else if (isPlayed && currentPosition > 0) {
                    BaseApplication.getInstance().getHandler().postDelayed(r, 1500);
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.stop();
            player.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}
