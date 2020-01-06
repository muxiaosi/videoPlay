package com.mxs.videoPlay;

/**
 * @author mxs
 * @date 2020-01-04 09:57
 * @description 视频播放器管理器
 */
public class MxsVideoPlayerManager {

    private MxsVideoPlayer mVideoPlayer;

    private static MxsVideoPlayerManager sInstance;

    private MxsVideoPlayerManager() {

    }

    public static synchronized MxsVideoPlayerManager instance() {
        if (sInstance == null) {
            sInstance = new MxsVideoPlayerManager();
        }
        return sInstance;
    }

    public MxsVideoPlayer getVideoPlayer() {
        return mVideoPlayer;
    }

    public void setVideoPlayer(MxsVideoPlayer videoPlayer) {
        if (mVideoPlayer != videoPlayer) {
            releaseFsVideoPlayer();
            mVideoPlayer = videoPlayer;
        }
    }

    public void releaseFsVideoPlayer() {
        if (mVideoPlayer != null) {
            mVideoPlayer.release();
            mVideoPlayer = null;
        }
    }

    /**
     * 暂停
     */
    public void suspendFsVideoPlayer() {
        if (mVideoPlayer != null && (mVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PLAYING || mVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PLAYING)) {
            mVideoPlayer.pause();
        }
    }

    /**
     * 播放
     */
    public void resumeFsVideoPlayer() {
        if (mVideoPlayer != null && (mVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_PAUSED || mVideoPlayer.getPlayState() == MxsVideoPlayer.STATE_BUFFERING_PAUSED)) {
            mVideoPlayer.reStart();
        }
    }

    public boolean onBackPressd() {
        if (mVideoPlayer != null && mVideoPlayer.isFullScreen()) {
            return mVideoPlayer.exitFullScreen();
        }
        return false;
    }
}
