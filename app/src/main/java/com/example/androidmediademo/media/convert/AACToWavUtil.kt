package com.example.androidmediademo.media.convert

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.concurrent.thread

object AACToWavUtil {

    fun convertAACToWav(filePath: String, destPath: String, callback: (Boolean) -> Unit) {
        thread {

            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(filePath)
            val trackIndex = Util.selectTrack(mediaExtractor, true)
            // 遍历数据音视频轨迹
            val mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
            mediaExtractor.selectTrack(trackIndex)
            val mWavFile = File(destPath)
            val mWriter: FileChannel?
            try {
                if (mWavFile.exists()) {
                    mWavFile.delete()
                }
                mWavFile.createNewFile()
                mWriter = FileOutputStream(mWavFile).channel
            } catch (e: Exception) {
                e.printStackTrace()
                callback.invoke(false)
                return@thread
            }

            // 创建编码器
            val mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaCodec.configure(mediaFormat, null, null, 0)
            mediaCodec.start()

            // 先写入wav头
            val fakehead = ByteArray(44)
            mWriter?.write(ByteBuffer.wrap(fakehead))

            var hasDone = false
            val mReadBuffer = ByteBuffer.wrap(ByteArray(4096))
            val bufferInfo = MediaCodec.BufferInfo()

            while (!hasDone) {
                mReadBuffer.rewind()
                val frameSize = mediaExtractor.readSampleData(mReadBuffer, 0)
                val inputBufferIndex = mediaCodec.dequeueInputBuffer(1000)
                if (inputBufferIndex >= 0) {
                    if (frameSize < 0) {
                        // 请注意，参数不能小于0，否则会报错
                        mediaCodec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    } else {
                        val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                        inputBuffer?.let {
                            inputBuffer.clear()
                            // 此处会报错
                            inputBuffer.put(mReadBuffer)
                            mediaCodec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                frameSize,
                                mediaExtractor.sampleTime,
                                0
                            )
                        }
                        mediaExtractor.advance()
                    }
                }

                val outBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000)
                if (outBufferIndex >= 0) {
                    val outBuffer = mediaCodec.getOutputBuffer(outBufferIndex)
                    outBuffer.let {
                        if (bufferInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            mWriter.write(outBuffer)
                        } else {
                            hasDone = true
                        }
                        mediaCodec.releaseOutputBuffer(outBufferIndex, false)
                    }
                }
            }

            mediaCodec.stop()
            mediaCodec.release()
            val head = Util.getWaveFileHeader(
                mWriter.size() - 44,
                mWriter.size() - 44 + 36,
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).toLong(),
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
            )
            val headBuffer = ByteBuffer.wrap(head)
            mWriter!!.write(headBuffer, 0)
            mWriter.force(true)

            callback.invoke(true)


        }


    }
}