package com.example.androidmediademo.media.play.mediacodec

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.IOException

/**
 *   自定义解码的工作线程
 * */
class VideoMediaCodecWorker(private val surface: Surface, private val filePath: String) : Thread() {

    companion object {
        const val TAG = "VideoMediaCodecWorker"

    }

    private var mediaExtractor: MediaExtractor = MediaExtractor()
    private var mediaCodec: MediaCodec? = null

    override fun run() {
//            super.run()
        // 设置要解析的视频文件地址
        try {
            mediaExtractor.setDataSource(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 遍历数据视频轨道，创建指定格式的MediaCodec
        for (i in 0 until mediaExtractor.trackCount) {
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            Log.e(TAG, ">> format i $i : $mediaFormat")
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.e(TAG, ">> mime i $i : $mime")
            // 找到视频格式m，并创建对应的解码器
            if (mime.startsWith("video/")) {
                mediaExtractor.selectTrack(i)
                try {
                    mediaCodec = MediaCodec.createDecoderByType(mime)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                mediaCodec!!.configure(mediaFormat, surface, null, 0)

            }
        }
        // 没找到音频轨道，直接返回
        mediaCodec?.start() ?: return

        // 是否已经读到了结束的位置
        var isEOS = false
        // 用于对准视频的时间戳
        val startMs = System.currentTimeMillis()
        while (!interrupted()) {
            // 开始写入解码器
            if (!isEOS) {
                // 返回使用有效输出的Buffer索引，如果没有相关Buffer可用，就返回-1
                // 如果传入的timeoutUs为0， 将立马返回
                // 如果输入的buffer可用，就无限期等待，timeoutUs的单位是us
                val inIndex = mediaCodec!!.dequeueInputBuffer(10000)
                if (inIndex > 0) {
                    val buffer = mediaCodec!!.getInputBuffer(inIndex)?: continue
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
            when (val outIndex = mediaCodec!!.dequeueOutputBuffer(info, 10000)) {
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
//                    val buffer = outputBuffers[outIndex]
                    // 这里使用简单的时钟方式保持视频的fps，不然视频会播放的很快
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

    }

    private fun sleepRender(audioBufferInfo: MediaCodec.BufferInfo, startMs: Long) {
        // 这里的时间是 毫秒  presentationTimeUs 的时间是累加的 以微秒进行一帧一帧的累加
        val timeDifference = audioBufferInfo.presentationTimeUs / 1000 - (System.currentTimeMillis() - startMs)
        if (timeDifference > 0) {
            try {
                sleep(timeDifference)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

}