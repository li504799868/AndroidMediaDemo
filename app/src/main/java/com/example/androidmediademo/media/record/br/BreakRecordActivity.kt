package com.example.androidmediademo.media.record.br

import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import com.example.androidmediademo.media.VideoMuxer
import com.example.androidmediademo.media.play.MediaPlayerActivity
import kotlinx.android.synthetic.main.activity_break_record.*
import java.io.File

/**
 * @author li.zhipeng
 *
 *      断点录制并合成播放
 * */
class BreakRecordActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var mediaRecorder: MediaRecorder? = null

    private var camera: Camera? = null

    private var isRecording = false

    /**
     *  最多录制10s
     * */
    private val maxDuration = 5

    private val saveDir: File =
        File("${Environment.getExternalStorageDirectory().absolutePath}/test/break")

    init {
        saveDir.mkdirs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_break_record)

        val surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceView.holder.addCallback(this)

        record_button.setOnClickListener {
            if (isRecording) {
                stopRecord()
            } else {
                startRecord()
            }
        }
        record_time.max = maxDuration
    }

    private fun startRecord() {
        if (mediaRecorder == null) {
            mediaRecorder = MediaRecorder()
            mediaRecorder!!.setCamera(camera)
            mediaRecorder!!.setOrientationHint(90)
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)

            // 如果需要设置指定的格式，一定要注意以下API的调用顺序
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            // 此配置不能和setOutputFormat一起使用
//            mediaRecorder!!.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P))
            mediaRecorder!!.setOutputFile("${saveDir.absoluteFile}/record_${System.currentTimeMillis()}.mp4")
            mediaRecorder!!.setPreviewDisplay(surface_view.holder.surface)
            mediaRecorder!!.prepare()
        }
        camera!!.unlock()
        mediaRecorder!!.start()
        isRecording = true
        record_button.text = "结束录制"
        startTimeTask()
    }

    private fun stopRecord() {
        mediaRecorder?.let {
            it.stop()
            it.reset()
            it.release()
        }
        mediaRecorder = null
        camera!!.lock()
        isRecording = false
        record_button.text = "开始录制"
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        openCamera()
        camera!!.setPreviewDisplay(holder)
        camera!!.startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        camera!!.stopPreview()
        camera!!.setPreviewDisplay(holder)
        camera!!.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        releaseCamera()
    }

    private fun openCamera() {
        if (camera == null) {
            camera = Camera.open()
        }
        camera!!.setDisplayOrientation(90)
    }

    private fun releaseCamera() {
        camera?.let {
            it.stopPreview()
            camera = null
        }
    }

    private fun startTimeTask() {
        record_time.postDelayed(runnable, 1000)
    }

    private fun startVideoMuxer() {
        VideoMuxer.muxVideoList(
            saveDir.listFiles { file -> file.name.startsWith("record_") },
            "${saveDir.absoluteFile}/result.mp4"
        ) {
            Toast.makeText(this, "视频合成成功", Toast.LENGTH_SHORT).show()
            MediaPlayerActivity.open(this, "${saveDir.absoluteFile}/result.mp4")
        }
    }

    private val runnable = Runnable {
        if (!isRecording) {
            return@Runnable
        }
        record_time.progress = record_time.progress + 1
        if (record_time.progress == maxDuration) {
            stopRecord()
            // 开始视频合成
            startVideoMuxer()
        } else {
            startTimeTask()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
    }


}
