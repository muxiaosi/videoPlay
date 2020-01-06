package com.mxs.videoPlay;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.mxs.videoPlay.utils.MxsVideoUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author mxs
 * @date 2020-01-03 15:30
 * @description 控制器抽象类
 */
public abstract class IMxsVideoPlayerController extends FrameLayout implements View.OnTouchListener {


    private Context mContext;
    /**
     * 播放器
     */
    protected IMxsVideoPlayer mIMxsVideoPlayer;
    /**
     * 更新进度定时器
     */
    private Timer mUpdateProgressTimer;
    /**
     * 定时器任务
     */
    private TimerTask mUpdateProgressTimerTask;

    private float mDownX;
    private float mDownY;
    /**
     * 是否更改进度
     */
    private boolean mNeedChangePosition;
    /**
     * 是否更改声音
     */
    private boolean mNeedChangeVolume;
    /**
     * 是否更改亮度
     */
    private boolean mNeedChangeBrightness;
    /**
     * 设置最小滑动距离
     */
    private static final int THRESHOLD = 80;
    /**
     * 手势滑动到的位置
     */
    private long mGestureDownPosition;
    /**
     * 滑动之后的亮度
     */
    private float mGestureDownBrightness;
    /**
     * 滑动之后的音量
     */
    private int mGestureDownVolume;
    private long mNewPosition;

    public IMxsVideoPlayerController(@NonNull Context context) {
        super(context);
        mContext = context;
        this.setOnTouchListener(this);
    }

    /**
     * 设置播放器
     *
     * @param iMxsVideoPlayer 播放器
     */
    public void setIFsVideoPlayer(IMxsVideoPlayer iMxsVideoPlayer) {
        mIMxsVideoPlayer = iMxsVideoPlayer;
    }

    /**
     * 设置播放的视频的标题
     *
     * @param title 视频标题
     */
    public abstract void setTitle(String title);

    /**
     * 设置视频底图~第一张展示的图
     *
     * @param resId
     */
    public abstract void setStartImage(@DrawableRes int resId);

    /**
     * 视频底图ImageView控件，提供给外部用图片加载工具来加载网络图片
     *
     * @return 底图ImageView
     */
    public abstract ImageView imageView();

    /**
     * 设置总时长
     *
     * @param length 时长
     */
    public abstract void setLength(int length);

    /**
     * 当播放器的播放状态发生变化，在此方法中更新不同的播放状态的UI
     *
     * @param playState 播放状态
     *                  <ul>
     *                  <li>{@link MxsVideoPlayer#STATE_IDLE}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_PREPARING}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_PREPARED}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_PLAYING}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_PAUSED}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_BUFFERING_PLAYING}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_BUFFERING_PAUSED}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_ERROR}</li>
     *                  <li>{@link MxsVideoPlayer#STATE_COMPLETED}</li>
     *                  </ul>
     */
    protected abstract void onPlayStateChanged(int playState);

    /**
     * 当播放器的播放窗体模式发生变化，在此方法中更新不同模式下的控制器界面
     *
     * @param playMode 播放模式
     *                 <ul>
     *                 <li>{@link MxsVideoPlayer#MODE_NORMAL}</li>
     *                 <li>{@link MxsVideoPlayer#MODE_FULL_SCREEN}</li>
     *                 </ul>
     */
    protected abstract void onPlayModeChanged(int playMode);

    /**
     * 重置控制器，将控制器恢复到初始状态。
     */
    protected abstract void reset();

    /**
     * 开启更新进度的计时器
     */
    protected void startUpdateProgressTimer() {
        cancelUpdateProgressTimer();
        if (mUpdateProgressTimer == null) {
            mUpdateProgressTimer = new Timer();
        }
        if (mUpdateProgressTimerTask == null) {
            mUpdateProgressTimerTask = new TimerTask() {
                @Override
                public void run() {
                    IMxsVideoPlayerController.this.post(() -> updateProgress());
                }
            };
        }
        mUpdateProgressTimer.schedule(mUpdateProgressTimerTask, 0, 1000);
    }

    protected void cancelUpdateProgressTimer() {
        if (mUpdateProgressTimerTask != null) {
            mUpdateProgressTimerTask.cancel();
            mUpdateProgressTimerTask = null;
        }
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer.cancel();
            mUpdateProgressTimer = null;
        }
    }

    /**
     * 更新进度，包括进度条进度，展示的当前播放位置时长，总时长等。
     */
    protected abstract void updateProgress();

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!mIMxsVideoPlayer.isFullScreen()) {
            return false;
        }
        switch (mIMxsVideoPlayer.getPlayState()) {
            case MxsVideoPlayer.STATE_IDLE:
            case MxsVideoPlayer.STATE_ERROR:
            case MxsVideoPlayer.STATE_PREPARING:
            case MxsVideoPlayer.STATE_PREPARED:
            case MxsVideoPlayer.STATE_COMPLETED:
                hideChangePosition();
                hideChangeBrightness();
                hideChangeVolume();
                return false;
            default:
                break;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mNeedChangeBrightness = false;
                mNeedChangePosition = false;
                mNeedChangeVolume = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - mDownX;
                float deltaY = y - mDownY;
                float absDeltaX = Math.abs(deltaX);
                float absDeltaY = Math.abs(deltaY);
                if (!mNeedChangePosition && !mNeedChangeVolume && !mNeedChangeBrightness) {
                    // 只有在播放、暂停、缓冲的时候能够拖动改变位置、亮度和声音
                    if (absDeltaX >= THRESHOLD) {
                        cancelUpdateProgressTimer();
                        mNeedChangePosition = true;
                        mGestureDownPosition = mIMxsVideoPlayer.getCurrentPosition();
                    } else if (absDeltaY >= THRESHOLD) {
                        if (mDownX < getWidth() * 0.5f) {
                            // 左侧改变亮度
                            mNeedChangeBrightness = true;
                            mGestureDownBrightness = MxsVideoUtils.scanForActivity(mContext).getWindow().getAttributes().screenBrightness;

                        } else {
                            //右侧改变声音
                            mNeedChangeVolume = true;
                            mGestureDownVolume = mIMxsVideoPlayer.getVolume();
                        }
                    }
                }
                if (mNeedChangePosition) {
                    long duration = mIMxsVideoPlayer.getDuration();
                    long toPosition = (long) (mGestureDownPosition + duration * deltaX / getWidth());
                    mNewPosition = Math.max(0, Math.min(duration, toPosition));
                    int newPositionProgress = (int) (100f * mNewPosition / duration);
                    showChangePosition(duration, newPositionProgress);
                }
                if (mNeedChangeBrightness) {
                    deltaY = -deltaY;
                    float deltaBrightness = deltaY * 3 / getHeight();
                    float newBrightness = mGestureDownBrightness + deltaBrightness;
                    newBrightness = Math.max(0, Math.min(newBrightness, 1));
                    float newBrightnessPercentage = newBrightness;
                    WindowManager.LayoutParams params = MxsVideoUtils.scanForActivity(mContext).getWindow().getAttributes();
                    params.screenBrightness = newBrightnessPercentage;
                    MxsVideoUtils.scanForActivity(mContext).getWindow().setAttributes(params);
                    int newBrightnessProgress = (int) (100f * newBrightnessPercentage);
                    showChangeBrightness(newBrightnessProgress);
                }
                if (mNeedChangeVolume) {
                    deltaY = -deltaY;
                    int maxVolume = mIMxsVideoPlayer.getMaxVolume();
                    int deltaVolume = (int) (maxVolume * deltaY * 3 / getHeight());
                    int newVolume = mGestureDownVolume + deltaVolume;
                    newVolume = Math.max(0, Math.min(maxVolume, newVolume));
                    mIMxsVideoPlayer.setVolume(newVolume);
                    int newVolumeProgress = (int) (100f * newVolume / maxVolume);
                    showChangeVolume(newVolumeProgress);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mNeedChangePosition){
                    mIMxsVideoPlayer.seekTo(mNewPosition);
                    hideChangePosition();
                    startUpdateProgressTimer();
                    return true;
                }
                if (mNeedChangeBrightness){
                    hideChangeBrightness();
                    return true;
                }
                if (mNeedChangeVolume){
                    hideChangeVolume();
                    return true;
                }
                break;
            default:
        }
        return false;
    }

    /**
     * 手势在右侧上下滑动改变音量时，显示控制器中间的音量变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newVolumeProgress 新的音量进度，取值1到100。
     */
    protected abstract void showChangeVolume(int newVolumeProgress);

    /**
     * 手势在左侧上下滑动改变音量后，手势up或者cancel时，隐藏控制器中间的音量变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected abstract void hideChangeVolume();

    /**
     * 手势在左侧上下滑动改变亮度时，显示控制器中间的亮度变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param newBrightnessProgress 新的亮度进度，取值1到100。
     */
    protected abstract void showChangeBrightness(int newBrightnessProgress);

    /**
     * 手势在左侧上下滑动改变亮度后，手势up或者cancel时，隐藏控制器中间的亮度变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected abstract void hideChangeBrightness();

    /**
     * 手势左右滑动改变播放位置时，显示控制器中间的播放位置变化视图，
     * 在手势滑动ACTION_MOVE的过程中，会不断调用此方法。
     *
     * @param duration            视频总时长ms
     * @param newPositionProgress 新的位置进度，取值0到100。
     */
    protected abstract void showChangePosition(long duration, int newPositionProgress);

    /**
     * 手势左右滑动改变播放位置后，手势up或者cancel时，隐藏控制器中间的播放位置变化视图，
     * 在手势ACTION_UP或ACTION_CANCEL时调用。
     */
    protected abstract void hideChangePosition();

    /**
     * 在默认页面的时候，控制是否可以全屏，如果不能全屏的话，控制器中隐藏全屏按钮
     * @param isAble true 可以全屏
     */
    public abstract void isAbleFullScreen(boolean isAble);
}
