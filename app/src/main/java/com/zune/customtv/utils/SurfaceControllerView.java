package com.zune.customtv.utils;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zune.customtv.R;
import com.zune.customtv.base.BaseApplication;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class SurfaceControllerView extends FrameLayout {

    private ImageView mPreBtn;
    private ImageView mPlayBtn;
    private ImageView mNextBtn;
    private TextView mTvProgress;
    private TextView mTvTotal;
    private SeekBar mSeekBar;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private View mVideoBack;
    private TextView mVideoTitle;
    private ViewGroup mBottomLay;
    private View mTopLay;

    private int mBigLength;
    private int mSmallLength;
    private IjkMediaPlayer ijkMediaPlayer;

    public void setIjkMediaPlayer(IjkMediaPlayer ijkMediaPlayer) {
        this.ijkMediaPlayer = ijkMediaPlayer;
    }

    public SurfaceControllerView(Context context) {
        super(context);
        inflate();
    }

    public SurfaceControllerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate();
    }

    public SurfaceControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate();
    }

    private void inflate() {
        LayoutInflater.from(getContext()).inflate(R.layout.surface_controller_view, this, true);
        TextView videoDuration = findViewById(R.id.video_duration);
        mPreBtn = findViewById(R.id.pre_btn);
        mPlayBtn = findViewById(R.id.play_btn);
        mNextBtn = findViewById(R.id.next_btn);
        mTvProgress = findViewById(R.id.tv_progress_time);
        mTvTotal = findViewById(R.id.tv_total_time);
        mSeekBar = findViewById(R.id.seek_bar);
        ((ViewGroup) mSeekBar.getParent()).setOnTouchListener((view, event) -> expansionSeekBar(event));
        mVideoBack = findViewById(R.id.video_back);
        mVideoBack.setOnClickListener(v -> ((Activity) getContext()).finish());
        mVideoTitle = findViewById(R.id.video_title);
        mTopLay = findViewById(R.id.top_lay);
        findViewById(R.id.change_orientation).setOnClickListener(this::changeOrientation);
        mTopLay.setPadding(0, 64, 32, 0);
        mBottomLay = findViewById(R.id.bottom_lay);
        findViewById(R.id.bg_view).setOnTouchListener(new OnTouchListener() {
            float downX = 0;
            float downY = 0;
            float totalProgress = 10000;
            long downTime = System.currentTimeMillis();
            boolean isAway = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int scaledTouchSlop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isAway = false;
                        totalProgress = ijkMediaPlayer.getDuration() / 3f;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        downTime = System.currentTimeMillis();
                        v.postDelayed(() -> {
                            if (isAway) {
                                return;
                            }
                            if (Math.abs(event.getRawX() - downX) < scaledTouchSlop && Math.abs(event.getRawY() - downY) < scaledTouchSlop) {
                                onLongClick(v);
                            }
                        }, ViewConfiguration.getDoubleTapTimeout());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float mX = event.getRawX() - downX;
                        float mSecond = (mX / v.getMeasuredWidth()) * totalProgress;
                        String text = getTotalUsTime((long) (ijkMediaPlayer.getCurrentPosition() + mSecond), false)
                                + ":" + getTotalUsTime(ijkMediaPlayer.getDuration(), true);
                        if (Math.abs(event.getRawX() - downX) > scaledTouchSlop) {
                            videoDuration.setText(text);
                            getParent().requestDisallowInterceptTouchEvent(true);
                        } else {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        getParent().requestDisallowInterceptTouchEvent(false);
                        isAway = true;
                        videoDuration.setText("");
                        ijkMediaPlayer.start();
                        if (Math.abs(event.getRawX() - downX) < scaledTouchSlop && Math.abs(event.getRawY() - downY) < scaledTouchSlop) {
                            if (System.currentTimeMillis() - downTime < ViewConfiguration.getDoubleTapTimeout()) {
                                changeController();
                            }
                        } else {
                            if (Math.abs(event.getRawX() - downX) > scaledTouchSlop) {
                                float offsetX = event.getRawX() - downX;
                                float progressSecond = (offsetX / v.getMeasuredWidth()) * totalProgress;
                                float currentProgress = mSeekBar.getProgress();
                                ijkMediaPlayer.seekTo((long) (progressSecond + currentProgress));
                            }
                        }
                        break;
                }
                return true;
            }
        });
        mPlayBtn.setOnClickListener(v -> {
            if (ijkMediaPlayer.isPlaying()) {
                ijkMediaPlayer.pause();
            } else {
                ijkMediaPlayer.start();
            }
            refreshPlayIcon();
            dismissControllerDelay();
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                dismissControllerDelay();
                ijkMediaPlayer.seekTo(seekBar.getProgress());
                startPlay();
                refreshPlayIcon();
            }
        });
        dismissControllerDelay();
    }

    private boolean onLongClick(View v) {
        if (onOrientationChangeListener != null) {
            onOrientationChangeListener.onRootViewLongClick();
        }
        return false;
    }

    private void changeOrientation(View v) {
        int mOrientation = LinearLayout.VERTICAL;
        if (mBigLength == 0) {
            mBigLength = Math.max(getWidth(), getHeight());
        }
        if (mSmallLength == 0) {
            mSmallLength = Math.min(getWidth(), getHeight());
        }
        if (onOrientationChangeListener != null) {
//            switch (mOrientation) {
//                case LinearLayout.HORIZONTAL:
//                    mTopLay.setTranslationY(mBigLength - mSmallLength - BarUtils.getStatusBarHeight());
//                    getLayoutParams().width = mBigLength;
//                    setRotation(90);
//                    break;
//                case LinearLayout.VERTICAL:
//                    mTopLay.setTranslationY(0);
//                    getLayoutParams().width = mSmallLength;
//                    setRotation(0);
//                default:
//                    break;
//            }
            onOrientationChangeListener.onOrientationChange(mOrientation);
        }
    }

    private boolean expansionSeekBar(MotionEvent event) {
        Rect seekRect = new Rect();
        mSeekBar.getHitRect(seekRect);
        if ((event.getRawY() >= (seekRect.top - 500)) && (event.getRawY() <= (seekRect.bottom + 500))) {
            float y = seekRect.top + seekRect.height() / 2f;
            float x = event.getRawX() - seekRect.left;
            if (x < 0) {
                x = 0;
            } else if (x > seekRect.width()) {
                x = seekRect.width();
            }
            MotionEvent me = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                    event.getAction(), x, y, event.getMetaState());
            return mSeekBar.onTouchEvent(me);
        }
        return false;
    }

    private boolean isPlayingIcon;

    private void refreshPlayIcon() {
        if (!ijkMediaPlayer.isPlaying()) {
            if (!isPlayingIcon) {
                isPlayingIcon = true;
                mPlayBtn.setImageResource(android.R.drawable.ic_media_play);
            }
        } else {
            if (isPlayingIcon) {
                isPlayingIcon = false;
                mPlayBtn.setImageResource(android.R.drawable.ic_media_pause);
            }
        }
    }

    public void startPlay() {
        if (mHandler == null) {
            return;
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ijkMediaPlayer.isPlaying()) {
                    mTvProgress.setText(getTotalUsTime(ijkMediaPlayer.getCurrentPosition(), false));
                    mSeekBar.setProgress((int) ijkMediaPlayer.getCurrentPosition());
                }
                refreshPlayIcon();
                startPlay();
            }
        }, 1000);
        mTvTotal.setText(getTotalUsTime(ijkMediaPlayer.getDuration(), true));
        mSeekBar.setMax((int) ijkMediaPlayer.getDuration());
    }

    public static String getTotalUsTime(long mill, boolean roundDown) {
        if (mill <= 0) {
            return "00:00";
        }
        long second = mill / 1000 + (roundDown ? 0 : 1);
        long minute = second / 60;
        long hour = minute / 60;
        if (hour == 0) {
            if (minute < 10 && second % 60 < 10) {
                return String.format("0%s:0%s", minute, second % 60);
            } else if (minute < 10) {
                return String.format("0%s:%s", minute, second % 60);
            } else if (second % 60 < 10) {
                return String.format("%s:0%s", minute, second % 60);
            } else {
                return String.format("%s:%s", minute, second % 60);
            }
        } else {
            if (minute % 60 < 10 && second % 60 < 10) {
                return String.format("%s:0%s:0%s", hour, minute % 60, second % 60);
            } else if (minute % 60 < 10) {
                return String.format("%s:0%s:%s", hour, minute % 60, second % 60);
            } else if (second % 60 < 10) {
                return String.format("%s:%s:0%s", hour, minute % 60, second % 60);
            } else {
                return String.format("%s:%s:%s", hour, minute % 60, second % 60);
            }
        }
    }

    public void release() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public void setTitle(@NonNull String title) {
        mVideoTitle.setText(title);
    }

    public Handler mChangeHandler = new Handler(Looper.getMainLooper());


    private void dismissControllerDelay() {
        mChangeHandler.removeCallbacksAndMessages(null);
        mChangeHandler.postDelayed(this::changeController, 7000);
    }

    boolean isControllerHide;


    void changeController() {
        if (!(getContext() instanceof Activity)) {
            return;
        }
        if (getContext() == null || ((Activity) getContext()).isDestroyed() || ((Activity) getContext()).isFinishing()) {
            return;
        }
        mChangeHandler.removeCallbacksAndMessages(null);
        int topHeight = -mTopLay.getMeasuredHeight() - 64;
        if (!isControllerHide) {
            for (int i = 0; i < mBottomLay.getChildCount(); i++) {
                ObjectAnimator.ofFloat(mBottomLay.getChildAt(i), "translationY", 0, i == 0 ? mBottomLay.getMeasuredHeight()
                        : dp2px(10))
                        .setDuration(300)
                        .start();
            }
            ObjectAnimator.ofFloat(mBottomLay, "translationY", 0, dp2px(20))
                    .setDuration(300)
                    .start();
            ObjectAnimator.ofFloat(mTopLay, "translationY", 0, topHeight)
                    .setDuration(300)
                    .start();
        } else {
            dismissControllerDelay();
            for (int i = 0; i < mBottomLay.getChildCount(); i++) {
                ObjectAnimator.ofFloat(mBottomLay.getChildAt(i), "translationY", i == 0 ? mBottomLay.getMeasuredHeight()
                        : dp2px(10), 0)
                        .setDuration(300)
                        .start();
            }
            ObjectAnimator.ofFloat(mBottomLay, "translationY", dp2px(20), 0)
                    .setDuration(300)
                    .start();
            ObjectAnimator.ofFloat(mTopLay, "translationY", topHeight, 0)
                    .setDuration(300)
                    .start();
        }
        isControllerHide = !isControllerHide;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(float dpValue) {
        final float scale = BaseApplication.getInstance().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public interface OnOrientationChangeListener {
        void onOrientationChange(int orientation);

        void onRootViewLongClick();
    }

    private OnOrientationChangeListener onOrientationChangeListener;

    public void setOnOrientationChangeListener(OnOrientationChangeListener onOrientationChangeListener) {
        this.onOrientationChangeListener = onOrientationChangeListener;
    }
}
