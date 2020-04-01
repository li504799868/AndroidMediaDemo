package com.example.androidmediademo.media.convert

import android.app.ProgressDialog
import android.media.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import com.example.androidmediademo.media.Constants
import com.example.androidmediademo.media.Utils
import kotlinx.android.synthetic.main.activity_mono_to_stero.*
import kotlin.concurrent.thread


/**
 * 单声道转双声道
 * */
class MonoToStereoActivity : AppCompatActivity() {

    private var currentChannel = AudioFormat.CHANNEL_IN_MONO

    private val recordTime = 5 // 暂时录制5秒就够了

    private val monoByteList = arrayListOf<Byte>()

    private val stereoByteList = arrayListOf<Byte>()

    private val dialog by lazy {
        val dialog = ProgressDialog(this)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mono_to_stero)

        record_mono.setOnClickListener {
            showLoading()
            recordThread.start()
        }

        convert_mono_to_stereo.setOnClickListener {
            if (monoByteList.isEmpty()) {
                Utils.showToast(this, "请录制后，再转换")
                return@setOnClickListener
            }
            showLoading()
            convertMonoToStereoThread.start()
        }

        play_pcm.setOnClickListener {
            if (monoByteList.isEmpty()) {
                Utils.showToast(this, "请录制后，再播放")
                return@setOnClickListener
            }
            play(monoByteList, AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_OUT_MONO)
        }

        play_stereo_pcm.setOnClickListener {
            if (stereoByteList.isEmpty()) {
                Utils.showToast(this, "请先转换后再播放")
                return@setOnClickListener
            }
            play(stereoByteList, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_OUT_STEREO)
        }

    }

    private fun showLoading() {
        dialog.show()
    }

    private fun dismissLoading() {
        runOnUiThread {
            dialog.dismiss()
        }
    }

    private fun recordCallback() {
        runOnUiThread {
            dismissLoading()
        }
    }

    private fun convertCallback() {
        runOnUiThread {
            dismissLoading()
        }
    }

    private val recordThread = Thread(
        Runnable {
            val iMinBufferSize = AudioRecord.getMinBufferSize(
                Constants.SAMPLE_RATE,
                currentChannel,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                Constants.SAMPLE_RATE,
                currentChannel,
                AudioFormat.ENCODING_PCM_16BIT,
                iMinBufferSize
            )

            audioRecord.startRecording()
            monoByteList.clear()
            val recordBytes = ByteArray(iMinBufferSize)
            var lastTime = 0L
            var pcmSize = 0
            while (lastTime < recordTime * 1000000L) {
                val readSize = audioRecord.read(recordBytes, 0, recordBytes.size)
                monoByteList.addAll(recordBytes.asList())
                pcmSize += readSize
                lastTime = pcmSize * 1000000L / 2 / 2 / Constants.SAMPLE_RATE
            }

            audioRecord.stop()
            audioRecord.release()
            recordCallback()
        }
    )

    private val convertMonoToStereoThread = Thread(Runnable {
        // 单声道转双声道
        // 双声道的存储格式为 LRLRLR
        // 所以把左声道的内容拷贝到右声道即可
        for (index in 0 until monoByteList.size step 2) {
            // 目前保存的是16位的数据，所以要复制前两位
            stereoByteList.add(monoByteList[index])
            stereoByteList.add(monoByteList[index + 1])
            // 目前保存的是16位的数据，所以要复制前两位
            stereoByteList.add(monoByteList[index])
            stereoByteList.add(monoByteList[index + 1])
        }
        convertCallback()

    })

    private fun play(data: ArrayList<Byte>, channelIn: Int, channelOut: Int) {
        thread {
            val audioTrack = AudioTrack(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build(),
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Constants.SAMPLE_RATE)
                    .setChannelMask(channelOut).build(),
                AudioRecord.getMinBufferSize(
                    Constants.SAMPLE_RATE,
                    channelIn,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM,
                0
            )

            audioTrack.play()
            val byteArray = data.toByteArray()
            audioTrack.write(byteArray, 0, byteArray.size)

            audioTrack.flush()
            audioTrack.stop()
            audioTrack.release()

        }
    }

}
