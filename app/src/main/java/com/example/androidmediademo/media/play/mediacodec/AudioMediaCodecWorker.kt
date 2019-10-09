package com.example.androidmediademo.media.play.mediacodec

import android.media.*
import android.media.AudioFormat.*
import android.util.Log
import java.io.IOException


/**
 *   自定义解码的工作线程
 * */
class AudioMediaCodecWorker(private val filePath: String) : Thread() {

    companion object {
        const val TAG = "AudioMediaCodecWorker"

    }

    private var mediaExtractor: MediaExtractor = MediaExtractor()
    private var mediaCodec: MediaCodec? = null

    override fun run() {
//            super.run()
        try {
            mediaExtractor.setDataSource(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var audioTrack: AudioTrack? = null

        for (i in 0 until mediaExtractor.trackCount) {
            // 遍历数据音视频轨迹
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            Log.e(TAG, ">> format i $i : $mediaFormat")
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.e(TAG, ">> mime i $i : $mime")
            if (mime.startsWith("audio/")) {
                mediaExtractor.selectTrack(i)
                try {
                    mediaCodec = MediaCodec.createDecoderByType(mime)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                mediaCodec!!.configure(mediaFormat, null, null, 0)
                val audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val mSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                // 创建音轨
                audioTrack = AudioTrack(
                    AudioAttributes.Builder()
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build(),
                    Builder()
                        .setChannelMask(if (audioChannels == 1) CHANNEL_OUT_MONO else CHANNEL_OUT_STEREO)
                        .setEncoding(ENCODING_PCM_16BIT)
                        .setSampleRate(mSampleRate)
                        .build(),
                    AudioRecord.getMinBufferSize(
                        mSampleRate,
                        if (audioChannels == 1) CHANNEL_IN_MONO else CHANNEL_IN_STEREO,
                        ENCODING_PCM_16BIT
                    ),
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
            }
        }

        mediaCodec?.start() ?: return
        audioTrack?.play()

        // 是否已经读到了结束的位置
        var isEOS = false
        val startMs = System.currentTimeMillis()
        loop@ while (!interrupted()) {
            if (!isEOS) {
                // 返回使用有效输出的Buffer索引，如果没有相关Buffer可用，就返回-1
                // 如果传入的timeoutUs为0， 将立马返回
                // 如果输入的buffer可用，就无限期等待，timeoutUs的单位是us
                val inIndex = mediaCodec!!.dequeueInputBuffer(0)
                if (inIndex > 0) {
                    val buffer = mediaCodec!!.getInputBuffer(inIndex) ?: continue
                    Log.e(TAG, ">> buffer $buffer")
                    val sampleSize = mediaExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        Log.e(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                        mediaCodec!!.queueInputBuffer(
                            inIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        isEOS = true
                    } else {
                        mediaCodec!!.queueInputBuffer(
                            inIndex,
                            0,
                            sampleSize,
                            mediaExtractor.sampleTime,
                            0
                        )
                        mediaExtractor.advance()
                    }

                }
            }

            // 每个buffer的元数据包括具体范围的偏移及大小，以及有效数据中相关的解码的buffer
            val info = MediaCodec.BufferInfo()
            when (val outIndex = mediaCodec!!.dequeueOutputBuffer(info, 0)) {
                // 此类型已经废弃，如果使用的是getOutputBuffer（）可以忽略此状态
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    // 当buffer的格式发生改变，须指向新的buffer格式
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // 当buffer的格式发生改变，须指向新的buffer格式
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // 当dequeueOutputBuffer超时时，会到达此case
                    Log.e(TAG, ">> dequeueOutputBuffer timeout")
                }
                else -> {
                    val buffer = mediaCodec!!.getOutputBuffer(outIndex)?: continue@loop
                    //用来保存解码后的数据
                    buffer.position(0)
                    val outData = ByteArray(info.size)
                    buffer.get(outData)
                    //清空缓存
                    buffer.clear()

                    audioTrack?.write(outData, 0, outData.size)
                    sleepRender(info, startMs)
                    mediaCodec!!.releaseOutputBuffer(outIndex, true)
                }
            }

            // 在所有解码后的帧都被渲染后，就可以停止播放了
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                break
            }

        }

        mediaCodec!!.stop()
        mediaCodec!!.release()
        mediaExtractor.release()
        audioTrack?.stop()
        audioTrack?.release()

    }

    private fun sleepRender(audioBufferInfo: MediaCodec.BufferInfo, startMs: Long) {
        // 这里的时间是 毫秒  presentationTimeUs 的时间是累加的 以微秒进行一帧一帧的累加
        while (audioBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
            try {
                // 10 毫秒
                sleep(16)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                break
            }

        }
    }

}