package com.example.androidmediademo.media.record

import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import com.example.androidmediademo.media.play.MediaPlayerActivity
import kotlinx.android.synthetic.main.activity_media_recorder.*
import java.io.File

/**
 * @author li.zhipeng
 *
 *      录制视频页面
 * */
class MediaRecorderActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val videoRecorderFile: String

    init {

        val fileDir = File("${Environment.getExternalStorageDirectory().absolutePath}/test")
        fileDir.mkdirs()
        // 创建要保存的录音文件的路径
        videoRecorderFile = "$fileDir/video_record.mp4"

    }

    private var camera: Camera? = null

    private var isRecording = false

    private var mediaRecorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_media_recorder)

        surface_view.holder.addCallback(this)
        // 新版本可以忽略设置下面的属性
//        surface_view.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        video_record.setOnClickListener {
            if (isRecording) {
                stopVideoRecord()
            } else {
                startVideoRecord()
            }

        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (camera == null) {
            camera = Camera.open()
        }
        camera!!.enableShutterSound(false)
        camera!!.setDisplayOrientation(90)
        // 绑定显示的SurfaceHolder
        camera!!.setPreviewDisplay(holder)
        // 开启预览
        camera!!.startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (holder.surface == null) {
            return
        }

        camera!!.stopPreview()
        camera!!.setPreviewDisplay(holder)
        camera!!.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }

    private fun startVideoRecord() {
        mediaRecorder = MediaRecorder().apply {
            camera!!.unlock()
            setCamera(camera!!)
            // 设置录制的角度，如果与摄像头不符，会出现视频角度不对的问题
            setOrientationHint(90)
            // 设置录音和录制视频的来源
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            // 还可以设置其他的信息
            // 输出的视频格式
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            // 设置编码的格式
//            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            // 设置视频的大小，必须要在设置视频格式之后设置，否则会报错
//            setVideoSize(width, height)
            //视频的帧率
//            setVideoFrameRate(25)
            // 设置录制的质量
            setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))
            // 设置文件的输出路径
            setOutputFile(videoRecorderFile)

            setPreviewDisplay(surface_view.holder.surface)
            prepare()
            // 开始录制
            start()
        }

        isRecording = true
        video_record.text = "停止录制"
    }

    private fun stopVideoRecord() {
        mediaRecorder?.let {
            it.stop()
            it.release()
            mediaRecorder = null
        }
        camera!!.lock()
        isRecording = false
        video_record.text = "开始录制"
        // 跳转播放
        MediaPlayerActivity.open(this, videoRecorderFile)
    }

}
