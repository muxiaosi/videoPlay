package com.mxs.videoPlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mxs.videoPlay.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * @author mxs
 * @date 2020-01-04 10:15
 * @description 控制器
 */
public class MxsVideoPlayerController extends IMxsVideoPlayerController implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private Context mContext;
    private ImageView mCenterStart;
    private ImageView mImage;
    private LinearLayout mTop;
    private ImageView mBack;
    private TextView mTitle;
    private LinearLayout mBatteryTime;
    private ImageView mBattery;
    private TextView mTime;
    private LinearLayout mBottom;
    private ImageView mRestartPause;
    private TextView mPosition;
    private TextView mDuration;
    private SeekBar mSeek;
    private ImageView mFullScreen;
    private TextView mLength;
    private LinearLayout mLoading;
    private TextView mLoadText;
    private LinearLayout mChangePosition;
    private TextView mChangePositionCurrent;
    private ProgressBar mChangePositionProgress;
    private LinearLayout mChangeBrightness;
    private ProgressBar mChangeBrightnessProgress;
    private LinearLayout mChangeVolume;
    private ProgressBar mChangeVolumeProgress;
    private LinearLayout mError;
    private TextView mRetry;
    private LinearLayout mCompleted;
    private TextView mReplay;

    private CountDownTimer mDismissTopBottomCountDownTimer;
    private boolean topBottomVisible;
    /**
     * 是否已经注册了电池广播
     */
    private boolean hasRegisterBatteryReceiver;

    public MxsVideoPlayerController(@NonNull Context context) {
        super(context);
        mContext = context;
        init();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.fs_video_palyer_controller, this, true);

        mCenterStart = findViewById(R.id.center_start);
        mImage = findViewById(R.id.image);

        mTop = findViewById(R.id.top);
        mBack = findViewById(R.id.back);
        mTitle = findViewById(R.id.title);
        mBatteryTime = findViewById(R.id.battery_time);
        mBattery = findViewById(R.id.battery);
        mTime = findViewById(R.id.time);

        mBottom = findViewById(R.id.bottom);
        mRestartPause = findViewById(R.id.restart_or_pause);
        mPosition = findViewById(R.id.position);
        mDuration = findViewById(R.id.duration);
        mSeek = findViewById(R.id.seek);
        mFullScreen = findViewById(R.id.full_screen);
        mLength = findViewById(R.id.length);

        mLoading = findViewById(R.id.loading);
        mLoadText = findViewById(R.id.load_text);

        mChangePosition = findViewById(R.id.change_position);
        mChangePositionCurrent = findViewById(R.id.change_position_current);
        mChangePositionProgress = findViewById(R.id.change_position_progress);

        mChangeBrightness = findViewById(R.id.change_brightness);
        mChangeBrightnessProgress = findViewById(R.id.change_brightness_progress);

        mChangeVolume = findViewById(R.id.change_volume);
        mChangeVolumeProgress = findViewById(R.id.change_volume_progress);

        mError = findViewById(R.id.error);
        mRetry = findViewById(R.id.retry);

        mCompleted = findViewById(R.id.completed);
        mReplay = findViewById(R.id.replay);

        mCenterStart.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mRestartPause.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        mRetry.setOnClickListener(this);
        mReplay.setOnClickListener(this);
        mSeek.setOnSeekBarChangeListener(this);
        this.setOnClickListener(this);
    }


    @Override
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setStartImage(int resId) {
        mImage.setImageResource(resId);
    }

    @Override
    public ImageView imageView() {
        return mImage;
    }

    @Override
    public void setLength(int length) {
        mLength.setText(formatTime(length));
    }

    @Override
    protected void onPlayStateChanged(int playState) {
        switch (playState) {
            case MxsVideoPlayer.STATE_IDLE:
                break;
            case MxsVideoPlayer.STATE_PREPARING:
                mImage.setVisibility(GONE);
                mLoading.setVisibility(VISIBLE);
                mLoadText.setText("正在准备...");
                mError.setVisibility(GONE);
                mCompleted.setVisibility(GONE);
                mTop.setVisibility(GONE);
                mBottom.setVisibility(GONE);
                mCenterStart.setVisibility(GONE);
                mLength.setVisibility(GONE);
                break;
            case MxsVideoPlayer.STATE_PREPARED:
                startUpdateProgressTimer();
                break;
            case MxsVideoPlayer.STATE_PLAYING:
                mLoading.setVisibility(GONE);
                mRestartPause.setImageResource(R.mipmap.ic_player_pause);
                startDismissTopBottomTimer();
                break;
            case MxsVideoPlayer.STATE_PAUSED:
                mLoading.setVisibility(VISIBLE);
                mRestartPause.setImageResource(R.mipmap.ic_player_start);
                cancelDismissTopBottomTimer();
                break;
            case MxsVideoPlayer.STATE_BUFFERING_PLAYING:
                mLoading.setVisibility(VISIBLE);
                mRestartPause.setImageResource(R.mipmap.ic_player_pause);
                mLoadText.setText("正在缓冲...");
                startDismissTopBottomTimer();
                break;
            case MxsVideoPlayer.STATE_BUFFERING_PAUSED:
                mLoading.setVisibility(VISIBLE);
                mRestartPause.setImageResource(R.mipmap.ic_player_start);
                mLoadText.setText("正在缓冲...");
                cancelDismissTopBottomTimer();
                break;
            case MxsVideoPlayer.STATE_ERROR:
                cancelDismissTopBottomTimer();
                setTopBottomVisible(false);
                mTop.setVisibility(VISIBLE);
                mError.setVisibility(VISIBLE);
                break;
            case MxsVideoPlayer.STATE_COMPLETED:
                cancelDismissTopBottomTimer();
                setTopBottomVisible(false);
                mTop.setVisibility(VISIBLE);
                mImage.setVisibility(VISIBLE);
                mCompleted.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPlayModeChanged(int playMode) {
        switch (playMode) {
            case MxsVideoPlayer.MODE_FULL_SCREEN:
                mBack.setVisibility(VISIBLE);
                mFullScreen.setVisibility(GONE);
                mFullScreen.setImageResource(R.mipmap.ic_player_shrink);
                mBatteryTime.setVisibility(View.VISIBLE);
                if (!hasRegisterBatteryReceiver) {
                    mContext.registerReceiver(mBatterReceiver,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    hasRegisterBatteryReceiver = true;
                }
                break;
            case MxsVideoPlayer.MODE_NORMAL:
                mBack.setVisibility(GONE);
                mFullScreen.setImageResource(R.mipmap.ic_player_enlarge);
                mFullScreen.setVisibility(VISIBLE);
                mBatteryTime.setVisibility(GONE);
                if (hasRegisterBatteryReceiver) {
                    mContext.unregisterReceiver(mBatterReceiver);
                    hasRegisterBatteryReceiver = false;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 电池状态即电量变化广播接收器
     */
    private BroadcastReceiver mBatterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                // 充电中
                mBattery.setImageResource(R.mipmap.ic_battery_charging);
            } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                // 充电完成
                mBattery.setImageResource(R.mipmap.ic_battery_full);
            } else {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                int percentage = (int) (((float) level / scale) * 100);
                if (percentage <= 10) {
                    mBattery.setImageResource(R.mipmap.ic_battery_10);
                } else if (percentage <= 20) {
                    mBattery.setImageResource(R.mipmap.ic_battery_20);
                } else if (percentage <= 50) {
                    mBattery.setImageResource(R.mipmap.ic_battery_50);
                } else if (percentage <= 80) {
                    mBattery.setImageResource(R.mipmap.ic_battery_80);
                } else if (percentage <= 100) {
                    mBattery.setImageResource(R.mipmap.ic_battery_100);
                }
            }
        }
    };

    @Override
    protected void reset() {
        topBottomVisible = false;
        cancelUpdateProgressTimer();
        cancelDismissTopBottomTimer();
        mSeek.setProgress(0);
        mSeek.setSecondaryProgress(0);

        mCenterStart.setVisibility(VISIBLE);
        mImage.setVisibility(VISIBLE);

        mBottom.setVisibility(GONE);
        mFullScreen.setImageResource(R.mipmap.ic_player_enlarge);

        mLength.setVisibility(View.VISIBLE);

        mTop.setVisibility(View.VISIBLE);
        mBack.setVisibility(View.GONE);

        mLoading.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        mCompleted.setVisibility(View.GONE);
    }

    @Override
    protected void updateProgress() {
        long position = mIMxsVideoPlayer.getCurrentPosition();
        long duration = mIMxsVideoPlayer.getDuration();
        int bufferPercentage = mIMxsVideoPlayer.getBufferPercentage();
        mSeek.setSecondaryProgress(bufferPercentage);
        int progress = (int) (100f * position / duration);
        mSeek.setProgress(progress);
        mPosition.setText(formatTime(position));
        mDuration.setText(formatTime(duration));
        //更新时间
        mTime.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
    }

    @Override
    protected void showChangeVolume(int newVolumeProgress) {
        mChangeVolume.setVisibility(VISIBLE);
        mChangeVolumeProgress.setProgress(newVolumeProgress);
    }

    @Override
    protected void hideChangeVolume() {
        mChangeVolume.setVisibility(GONE);
    }

    @Override
    protected void showChangeBrightness(int newBrightnessProgress) {
        mChangeBrightness.setVisibility(VISIBLE);
        mChangeBrightnessProgress.setProgress(newBrightnessProgress);
    }

    @Override
    protected void hideChangeBrightness() {
        mChangeBrightness.setVisibility(GONE);
    }

    @Override
    protected void showChangePosition(long duration, int newPositionProgress) {
        mChangePosition.setVisibility(VISIBLE);
        long newPosition = (long) (duration * newPositionProgress / 100f);
        mChangePositionCurrent.setText(formatTime(newPosition));
        mChangePositionProgress.setProgress(newPositionProgress);
        mSeek.setProgress(newPositionProgress);
        mPosition.setText(formatTime(newPosition));
    }

    @Override
    protected void hideChangePosition() {
        mChangePosition.setVisibility(GONE);
    }

    @Override
    public void isAbleFullScreen(boolean isAble) {
        if (isAble) {
            mFullScreen.setVisibility(VISIBLE);
        } else {
            mFullScreen.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mCenterStart) {
            //播放
            if (mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_IDLE) {
                mIMxsVideoPlayer.start();
            }
        } else if (view == mBack) {
            //退出
            if (mIMxsVideoPlayer.isFullScreen()) {
                mIMxsVideoPlayer.exitFullScreen();
            }
        } else if (view == mRestartPause) {
            //暂停、播放
            if (mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PLAYING || mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PLAYING) {
                mIMxsVideoPlayer.pause();
            } else if (mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PAUSED || mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PAUSED) {
                mIMxsVideoPlayer.reStart();
            }
        } else if (view == mFullScreen) {
            //全屏
            if (mIMxsVideoPlayer.isNormal()) {
                mIMxsVideoPlayer.enterFullScreen();
            } else {
                mIMxsVideoPlayer.enterFullScreen();
            }
        } else if (view == mRetry) {
            //点击重试
            mIMxsVideoPlayer.reStart();
        } else if (view == mReplay) {
            //点击重试
            mRetry.performClick();
        } else if (view == this) {
            if (mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PLAYING
                    || mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PAUSED
                    || mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PAUSED
                    || mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PLAYING) {
                setTopBottomVisible(!topBottomVisible);
            }
        }
    }

    /**
     * 开启top、bottom自动消失的timer
     */
    private void startDismissTopBottomTimer() {
        cancelDismissTopBottomTimer();
        if (mDismissTopBottomCountDownTimer == null) {
            mDismissTopBottomCountDownTimer = new CountDownTimer(8000, 8000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    setTopBottomVisible(false);
                }
            };
        }
        mDismissTopBottomCountDownTimer.start();
    }

    /**
     * 取消top、bottom自动消失的timer
     */
    private void cancelDismissTopBottomTimer() {
        if (mDismissTopBottomCountDownTimer != null) {
            mDismissTopBottomCountDownTimer.cancel();
        }
    }

    /**
     * 设置top、bottom的显示和隐藏
     *
     * @param visible true显示，false隐藏.
     */
    private void setTopBottomVisible(boolean visible) {
        mTop.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
        topBottomVisible = visible;
        if (visible) {
            if (!(mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PAUSED) && !(mIMxsVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PAUSED)) {
                startDismissTopBottomTimer();
            }
        } else {
            cancelDismissTopBottomTimer();
        }
    }

    /**
     * 将毫秒数格式化为"##:##"的时间
     *
     * @param milliseconds 毫秒数
     * @return ##:##
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0 || milliseconds >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        long totalSeconds = milliseconds / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
