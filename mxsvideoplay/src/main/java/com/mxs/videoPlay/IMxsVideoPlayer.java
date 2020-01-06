package com.mxs.videoPlay;

import java.util.Map;

/**
 * @author mxs
 * @date 2020-01-03 11:59
 * @description FVideoPlayer的抽象接口
 */
public interface IMxsVideoPlayer {

    /**
     * 设置视频url，以及headers
     * @param url 视频地址，可以是本地，也可以是网络视频
     * @param headers 请求header
     */
    void setPlayUrl(String url, Map<String, String> headers);

    /**
     * 开始播放
     */
    void start();

    /**
     * 从指定的位置开始播放
     * @param position 播放位置
     */
    void start(long position);

    /**
     * 重新播放，播放器被暂停、播放错误、播放完成后，需要调用此方法重新播放
     */
    void reStart();

    /**
     * 暂停播放
     */
    void pause();

    /**
     * seek到指定位置继续播放
     * @param position 播放位置
     */
    void seekTo(long position);

    /**
     * 设置音量
     * @param volume 音量值
     */
    void setVolume(int volume);

    /**
     * 开始播放时，是否从上一次的位置继续播放
     * @param isPlayFromLast
     */
    void isPlayFromLastPosition(boolean isPlayFromLast);

    /**
     * 获取最大音量
     * @return 返回最大音量
     */
    int getMaxVolume();

    /**
     * 获取当前播放的位置，毫秒
     *
     * @return 当前播放位置，ms
     */
    long getCurrentPosition();

    /**
     * 获取视频缓冲百分比
     *
     * @return 缓冲白百分比
     */
    int getBufferPercentage();

    /**
     * 获取当前音量
     *
     * @return 当前音量值
     */
    int getVolume();

    /**
     * 获取办法给总时长，毫秒
     *
     * @return 视频总时长ms
     */
    long getDuration();

    /**
     * 进入全屏模式
     */
    void enterFullScreen();

    /**
     * 退出全屏模式
     *
     * @return true 退出
     */
    boolean exitFullScreen();

    /**
     * 此处只释放播放器（如果要释放播放器并恢复控制器状态需要调用{@link #release()}方法）
     * 不管是全屏还是Normal状态下控制器的UI都不恢复初始状态
     * 这样以便在当前播放器状态下可以方便的切换不同的清晰度的视频地址
     */
    void releasePlayer();

    /**
     * 释放IFVideoPlayer，释放后，内部的播放器被释放掉，同时如果在全屏模式下都会退出
     * 并且控制器的UI也应该恢复到最初始的状态.
     */
    void release();

    /*********************************
     * 以下2个方法是播放器的模式
     **********************************/
    /**
     * 是否全屏
     * @return true 全屏
     */
    boolean isFullScreen();

    /**
     * 是否是正常模式
     * @return true 正常模式
     */
    boolean isNormal();

    /**
     * 获取播放器的状态
     * @return 播放器的状态
     */
    int getPlayState();
}
