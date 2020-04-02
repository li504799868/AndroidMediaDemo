package com.example.androidmediademo.media.convert

import android.app.ProgressDialog
import android.media.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import com.example.androidmediademo.media.Constants
import com.example.androidmediademo.media.Utils
import kotlinx.android.synthetic.main.activity_mono_to_stero.play_pcm
import kotlinx.android.synthetic.main.activity_stero_mono.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread


/**
 * 双声道转单声道
 * */
class StereoToMonoActivity : AppCompatActivity() {

    private var currentChannel = AudioFormat.CHANNEL_IN_STEREO

    private val recordTime = 5 // 暂时录制5秒就够了

    private val monoByteList = arrayListOf<Short>()

    private val stereoByteList = arrayListOf<Byte>()

    private val dialog by lazy {
        val dialog = ProgressDialog(this)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stero_mono)

        record_stereo.setOnClickListener {
            showLoading()
            startRecord()
        }

        convert_stereo_to_mono.setOnClickListener {
            if (stereoByteList.isEmpty()) {
                Utils.showToast(this, "请录制后，再转换")
                return@setOnClickListener
            }
            showLoading()
            convertStereoToMono()
        }

        play_pcm.setOnClickListener {
            if (stereoByteList.isEmpty()) {
                Utils.showToast(this, "请录制后，再播放")
                return@setOnClickListener
            }
            play(stereoByteList, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_OUT_STEREO)
        }

        play_mono_pcm.setOnClickListener {
            if (monoByteList.isEmpty()) {
                Utils.showToast(this, "请先转换后再播放")
                return@setOnClickListener
            }
            play(
                monoByteList.toShortArray(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.CHANNEL_OUT_MONO
            )
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

    private fun startRecord() {
        thread {
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
            stereoByteList.clear()
            val recordBytes = ByteArray(iMinBufferSize)
            var lastTime = 0L
            var pcmSize = 0
            while (lastTime < recordTime * 1000000L) {
                val readSize = audioRecord.read(recordBytes, 0, recordBytes.size)
                stereoByteList.addAll(recordBytes.asList())
                pcmSize += readSize
                lastTime = pcmSize * 1000000L / 2 / 2 / Constants.SAMPLE_RATE
            }

            audioRecord.stop()
            audioRecord.release()
            recordCallback()
        }
    }

    private fun convertStereoToMono() {
        thread {
            // 双声道转单声道
            // 方案1：丢掉一路数据，此方法最简单
            // 这里只取左声道的声音
            monoByteList.clear()
            // ByteOrder.LITTLE_ENDIAN 从小到大 ，高位在后
            // ByteOrder.BIG_ENDIAN 从大到小，高位在前，默认
            val shortBuffer = ByteBuffer.wrap(stereoByteList.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
//            for (index in 0 until shortBuffer.capacity() step 2) {
//                monoByteList.add(shortBuffer.get(index))
//            }
            // 方案二：把左右声道的声音相加，取平均值

            // 使用kotlin的位运算 and shl等，无法得到正确的byte转short，short转init

            for (index in 0 until shortBuffer.capacity() step 2) {
                monoByteList.add((shortBuffer.get(index) + shortBuffer.get(index + 1)).toShort())
            }

            convertCallback()
        }

    }

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

    private fun play(data: ShortArray, channelIn: Int, channelOut: Int) {
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
            audioTrack.write(data, 0, data.size)

            audioTrack.flush()
            audioTrack.stop()
            audioTrack.release()

        }
    }

}
