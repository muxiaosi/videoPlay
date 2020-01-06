package com.mxs.video

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mxs.videoPlay.MxsVideoPlayer
import com.mxs.videoPlay.MxsVideoPlayerController
import com.mxs.videoPlay.MxsVideoPlayerManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
    }

    private fun initView() {
        val videoUrl = "http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-33-30.mp4"
        video_player.setPlayUrl(videoUrl, null)
        video_player.setPlayerType(MxsVideoPlayer.TYPE_IJK)
        video_player.setCanLandscape(false)
        val controller = MxsVideoPlayerController(this)
        controller.setTitle("测试阿年 】")
        controller.setLength(98000)
        controller.isAbleFullScreen(false)
        video_player.setController(controller)
        video_player.isPlayFromLastPosition(false)
    }

    override fun onStop() {
        super.onStop()
        MxsVideoPlayerManager.instance().releaseFsVideoPlayer()
    }

    override fun onBackPressed() {
        if (MxsVideoPlayerManager.instance().onBackPressd()) {
            return
        }
        super.onBackPressed()
    }

}
