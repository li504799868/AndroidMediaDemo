package com.example.androidmediademo.media.play

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import com.example.androidmediademo.R
import kotlinx.android.synthetic.main.activity_media_player.*
import java.io.File

/**
 *  @author li.zhipeng
 *
 *          MediaPlayer播放视频
 * */
class MediaPlayerActivity : AppCompatActivity(), MediaPlayer.OnPreparedListener {

    private val videoRecorderFile: String

    init {

        val fileDir = File("${Environment.getExternalStorageDirectory().absolutePath}/test")
        fileDir.mkdirs()
        // 创建要保存的录音文件的路径
        videoRecorderFile = "$fileDir/video_record.mp4"

    }

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
    }

    override fun onResume() {
        super.onResume()

        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.start()
        } else {
            createMediaPlayer()
        }
    }

    private fun createMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(videoRecorderFile)
            setOnPreparedListener(this@MediaPlayerActivity)
            prepare()
            start()
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mediaPlayer.setDisplay(surface_view.holder)
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }
}
