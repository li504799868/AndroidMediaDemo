package com.example.androidmediademo.media.play.mediacodec

import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity


/**
 *  使用MediaCodec解码视频文件，播放在SurfaceView
 * */
class MediaCodecVideoPlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val TAG = "MediaCodecVideoPlayer"

    private val filePath =
        "${Environment.getExternalStorageDirectory()}/DCIM/Camera/test.mp4"

    private var workerThread: VideoMediaCodecWorker? = null

    private var audioMediaCodecWorker: AudioMediaCodecWorker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_media_codec_video_player)

        val surfaceView = SurfaceView(this)
        // 设置Surface不维护自己的缓冲区，等待屏幕的渲染引擎将内容推送到用户面前
        // 该api已经废弃，这个编辑会自动设置
//        surfaceView.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surfaceView.holder.addCallback(this)
        setContentView(surfaceView)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (workerThread == null) {
            workerThread = VideoMediaCodecWorker(holder!!.surface, filePath)
            workerThread!!.start()
        }

        if (audioMediaCodecWorker == null) {
            audioMediaCodecWorker = AudioMediaCodecWorker(filePath)
            audioMediaCodecWorker!!.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        if (workerThread != null) {
            workerThread!!.interrupt()
            workerThread = null
        }
        if (audioMediaCodecWorker != null) {
            audioMediaCodecWorker!!.interrupt()
            audioMediaCodecWorker = null
        }
    }




}
