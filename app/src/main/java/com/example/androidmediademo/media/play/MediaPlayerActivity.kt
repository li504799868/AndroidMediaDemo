package com.example.androidmediademo.media.play

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import kotlinx.android.synthetic.main.activity_media_player.*

/**
 *  @author li.zhipeng
 *
 *          MediaPlayer播放视频
 * */
class MediaPlayerActivity : AppCompatActivity(), MediaPlayer.OnPreparedListener {

    companion object{

        fun open(context: Context, filePath:String){
            val intent = Intent(context, MediaPlayerActivity::class.java)
            intent.putExtra("filePath", filePath)
            context.startActivity(intent)
        }

    }

    // 创建要保存的录音文件的路径
    private lateinit var videoRecorderFile: String

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        videoRecorderFile = intent.getStringExtra("filePath")
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
