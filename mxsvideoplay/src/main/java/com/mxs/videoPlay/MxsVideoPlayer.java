package com.mxs.videoPlay;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mxs.videoPlay.utils.MxsVideoUtils;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/**
 * @author mxs
 * @date 2020-01-03 15:08
 * @description 播放器
 */
public class MxsVideoPlayer extends FrameLayout implements IMxsVideoPlayer, TextureView.SurfaceTextureListener {

    private static final String TAG = "FsVideoPlayer";

    /**
     * 播放错误
     */
    public static final int STATE_ERROR = -1;
    /**
     * 播放未开始
     */
    public static final int STATE_IDLE = 0;
    /**
     * 播放准备中
     */
    public static final int STATE_PREPARING = 1;
    /**
     * 播放准备就绪
     */
    public static final int STATE_PREPARED = 2;
    /**
     * 正在播放
     */
    public static final int STATE_PLAYING = 3;
    /**
     * 暂停播放
     */
    public static final int STATE_PAUSED = 4;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，缓冲区数据足够后恢复播放)
     **/
    public static final int STATE_BUFFERING_PLAYING = 5;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，此时暂停播放器，继续缓冲，缓冲区数据足够后恢复暂停
     **/
    public static final int STATE_BUFFERING_PAUSED = 6;
    /**
     * 播放完成
     **/
    public static final int STATE_COMPLETED = 7;

    /**
     * 普通模式
     **/
    public static final int MODE_NORMAL = 10;
    /**
     * 全屏模式
     **/
    public static final int MODE_FULL_SCREEN = 11;


    /**
     * IjkPlayer
     **/
    public static final int TYPE_IJK = 111;
    /**
     * MediaPlayer
     **/
    public static final int TYPE_NATIVE = 222;
    /**
     * 采用哪一种播放模式
     * IjkPlayer 或者 MediaPlayer
     */
    private int mPlayerType = TYPE_IJK;
    /**
     * 当前播放器的状态
     */
    private int mCurrentState = STATE_IDLE;
    /**
     * 当前播放窗口播放模式
     * 全屏/正常模式
     */
    private int mCurrentMode = MODE_NORMAL;

    private Context mContext;
    /**
     * 根布局
     */
    private FrameLayout mContainer;
    /**
     * 加载网络的地址
     */
    private String mUrl;
    /**
     * 网络地址请求头
     */
    private Map<String, String> mHeaders;

    private AudioManager mAudioManager;

    private IMediaPlayer mMediaPlayer;

    private IMxsVideoPlayerController mController;

    private MxsTextureView mTextureView;
    private long skipToPosition;

    private boolean continueFromLastPosition = true;

    private int mBufferPercentage;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;

    /**
     * 全屏时是否全屏
     */
    private boolean isCanLandscape = true;

    public MxsVideoPlayer(Context context) {
        super(context, null);
    }

    public MxsVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mContainer = new FrameLayout(mContext);
        mContainer.setBackgroundColor(Color.BLACK);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);
    }

    /**
     * 设置播放器类型
     *
     * @param playerType IjkPlayer or MediaPlayer.
     */
    public void setPlayerType(int playerType) {
        mPlayerType = playerType;
    }

    /**
     * 全屏时是否横屏
     *
     * @param canLandscape true 可以横屏
     */
    public void setCanLandscape(boolean canLandscape) {
        isCanLandscape = canLandscape;
    }

    public void setController(IMxsVideoPlayerController controller) {
        mContainer.removeView(mController);
        mController = controller;
        mController.reset();
        mController.setIFsVideoPlayer(this);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mController, params);
    }

    @Override
    public void setPlayUrl(String url, Map<String, String> headers) {
        mUrl = url;
        mHeaders = headers;
    }

    @Override
    public void start() {
        if (mCurrentState == STATE_IDLE) {
            MxsVideoPlayerManager.instance().setVideoPlayer(this);
            initAudioManager();
            initMediaPlayer();
            initTextureView();
            addTextureView();
        } else {
            Log.d(TAG, "FsVideoPlayer只有在mCurrentState == STATE_IDLE时才能调用start方法.");
        }
    }

    private void addTextureView() {
        mContainer.removeView(mTextureView);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        mContainer.addView(mTextureView, 0, params);
    }

    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new MxsTextureView(mContext);
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            switch (mPlayerType) {
                case TYPE_NATIVE:
                    mMediaPlayer = new AndroidMediaPlayer();
                    break;
                case TYPE_IJK:
                default:
                    mMediaPlayer = new IjkMediaPlayer();
                    break;
            }
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }

    private void initAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    @Override
    public void start(long position) {
        skipToPosition = position;
        start();
    }

    @Override
    public void reStart() {
        if (mCurrentState == STATE_PAUSED) {
            //暂停
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "STATE_PLAYING");
        } else if (mCurrentState == STATE_BUFFERING_PAUSED) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "STATE_PLAYING");
        } else if (mCurrentState == STATE_COMPLETED || mCurrentState == STATE_ERROR) {
            mMediaPlayer.reset();
            openMediaPlayer();
        }
    }

    private void openMediaPlayer() {
        // 屏幕常亮
        mContainer.setKeepScreenOn(true);
        //设置监听
        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
        mMediaPlayer.setOnInfoListener(mOnInfoListener);
        mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        // 设置dataSource
        try {
            mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(mUrl), mHeaders);
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "STATE_PREPARING");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "打开播放器发生错误", e);
        }
    }

    @Override
    public void pause() {
        if (mCurrentState == STATE_PLAYING) {
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "STATE_PAUSED");
        }
        if (mCurrentState == STATE_BUFFERING_PLAYING) {
            mMediaPlayer.pause();
            mCurrentState = STATE_BUFFERING_PAUSED;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "STATE_BUFFERING_PAUSED");
        }
    }

    @Override
    public void seekTo(long position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    @Override
    public void setVolume(int volume) {
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }
    }

    /**
     * 是否从上一次的位置继续播放
     *
     * @param isPlayFromLast true从上一次的位置继续播放
     */
    @Override
    public void isPlayFromLastPosition(boolean isPlayFromLast) {
        continueFromLastPosition = isPlayFromLast;
    }

    @Override
    public int getMaxVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    @Override
    public int getVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer != null ? mMediaPlayer.getDuration() : 0;
    }

    /**
     * 全屏，将mContainer(内部包含mTextureView和mController)从当前容器中移除，并添加到android.R.content中.
     * 切换横屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期
     */
    @Override
    public void enterFullScreen() {
        if (mCurrentState == MODE_FULL_SCREEN) {
            return;
        }
        // 隐藏ActionBar、状态栏，并横屏
        MxsVideoUtils.hideActionBar(mContext);
        if (isCanLandscape) {
            MxsVideoUtils.scanForActivity(mContext)
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        ViewGroup contentView = MxsVideoUtils.scanForActivity(mContext)
                .findViewById(android.R.id.content);
        this.removeView(mContainer);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.addView(mContainer, params);
        mCurrentMode = MODE_FULL_SCREEN;
        mController.onPlayModeChanged(mCurrentMode);
        Log.d(TAG, "MODE_FULL_SCREEN");
    }

    /**
     * 退出全屏，移除mTextureView和mController，并添加到非全屏的容器中。
     * 切换竖屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期.
     *
     * @return true退出全屏.
     */
    @Override
    public boolean exitFullScreen() {
        if (mCurrentMode == MODE_FULL_SCREEN) {
            MxsVideoUtils.showActionBar(mContext);
            MxsVideoUtils.scanForActivity(mContext)
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ViewGroup contentView = MxsVideoUtils.scanForActivity(mContext)
                    .findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mCurrentMode = MODE_NORMAL;
            mController.onPlayModeChanged(mCurrentMode);
            Log.d(TAG, "MODE_NORMAL");
            return true;
        }
        return false;
    }

    @Override
    public void releasePlayer() {
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
            mAudioManager = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mContainer.removeView(mTextureView);
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mCurrentState = STATE_IDLE;
    }

    @Override
    public void release() {
        //保存播放位置
        if (mCurrentState == STATE_PLAYING
                || mCurrentState == STATE_BUFFERING_PLAYING
                || mCurrentState == STATE_BUFFERING_PAUSED
                || mCurrentState == STATE_PAUSED) {
            MxsVideoUtils.savePlayPosition(mContext, mUrl, getCurrentPosition());
        } else if (mCurrentState == STATE_COMPLETED) {
            MxsVideoUtils.savePlayPosition(mContext, mUrl, 0);
        }
        //退出全屏
        if (isFullScreen()) {
            exitFullScreen();
        }
        mCurrentMode = MODE_NORMAL;
        //释放播放器
        releasePlayer();
        // 恢复控制器
        if (mController != null) {
            mController.reset();
        }
        Runtime.getRuntime().gc();
    }

    @Override
    public boolean isFullScreen() {
        return mCurrentMode == MODE_FULL_SCREEN;
    }

    @Override
    public boolean isNormal() {
        return mCurrentMode == MODE_NORMAL;
    }

    @Override
    public int getPlayState() {
        return mCurrentState;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surfaceTexture;
            openMediaPlayer();
        } else {
            mTextureView.setSurfaceTexture(mSurfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return mSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public boolean isPaused() {
        return getPlayState() == STATE_PAUSED;
    }

    private IMediaPlayer.OnPreparedListener mOnPreparedListener
            = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "onPrepared ——> STATE_PREPARED");
            mp.start();
            // 从上次的保存位置播放
            if (continueFromLastPosition) {
                long savedPlayPosition = MxsVideoUtils.getSavedPlayPosition(mContext, mUrl);
                mp.seekTo(savedPlayPosition);
            }
            // 跳到指定位置播放
            if (skipToPosition != 0) {
                mp.seekTo(skipToPosition);
            }
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener
            = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            mTextureView.adaptVideoSize(width, height);
            Log.d(TAG, "onVideoSizeChanged ——> width：" + width + "， height：" + height);
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener
            = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            mCurrentState = STATE_COMPLETED;
            mController.onPlayStateChanged(mCurrentState);
            Log.d(TAG, "onCompletion ——> STATE_COMPLETED");
            // 清除屏幕常亮
            mContainer.setKeepScreenOn(false);
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener
            = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            // 直播流播放时去调用mediaPlayer.getDuration会导致-38和-2147483648错误，忽略该错误
            if (what != -38 && what != -2147483648 && extra != -38 && extra != -2147483648) {
                mCurrentState = STATE_ERROR;
                mController.onPlayStateChanged(mCurrentState);
                Log.d(TAG, "onError ——> STATE_ERROR ———— what：" + what + ", extra: " + extra);
            }
            return true;
        }
    };

    private IMediaPlayer.OnInfoListener mOnInfoListener
            = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                // 播放器开始渲染
                mCurrentState = STATE_PLAYING;
                mController.onPlayStateChanged(mCurrentState);
                Log.d(TAG, "onInfo ——> MEDIA_INFO_VIDEO_RENDERING_START：STATE_PLAYING");
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                // MediaPlayer暂时不播放，以缓冲更多的数据
                if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_BUFFERING_PAUSED) {
                    mCurrentState = STATE_BUFFERING_PAUSED;
                    Log.d(TAG, "onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PAUSED");
                } else {
                    mCurrentState = STATE_BUFFERING_PLAYING;
                    Log.d(TAG, "onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PLAYING");
                }
                mController.onPlayStateChanged(mCurrentState);
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                // 填充缓冲区后，MediaPlayer恢复播放/暂停
                if (mCurrentState == STATE_BUFFERING_PLAYING) {
                    mCurrentState = STATE_PLAYING;
                    mController.onPlayStateChanged(mCurrentState);
                    Log.d(TAG, "onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PLAYING");
                }
                if (mCurrentState == STATE_BUFFERING_PAUSED) {
                    mCurrentState = STATE_PAUSED;
                    mController.onPlayStateChanged(mCurrentState);
                    Log.d(TAG, "onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PAUSED");
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
                // 视频旋转了extra度，需要恢复
                if (mTextureView != null) {
                    mTextureView.setRotation(extra);
                    Log.d(TAG, "视频旋转角度：" + extra);
                }
            } else if (what == IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE) {
                Log.d(TAG, "视频不能seekTo，为直播视频");
            } else {
                Log.d(TAG, "onInfo ——> what：" + what);
            }
            return true;
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener
            = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mBufferPercentage = percent;
        }
    };
}
