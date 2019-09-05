package com.example.androidmediademo.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * @author li.zhipeng
 *
 *      视频合成器
 * */
object VideoMuxer {

    private const val MAX_BUFF_SIZE = 1048576

    private val mReadBuffer = ByteBuffer.allocate(MAX_BUFF_SIZE)

    @JvmStatic
    fun muxVideoList(videoList: Array<File>, outPath: String, finish: () -> Unit) {
        // 创建MediaMuxer
        val mediaMuxer = MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        mediaMuxer.setOrientationHint(90)
        // 开始遍历文件列表

        // 找到文件的视频格式和音频格式
        var findAudioFormat = false
        var findVideoFormat = false

        var audioFormat: MediaFormat? = null
        var videoFormat: MediaFormat? = null

        for (file in videoList) {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(file.absolutePath)

            if (!findAudioFormat) {
                audioFormat = findFormat(mediaExtractor, "audio/")
                findAudioFormat = audioFormat != null
            }

            if (!findVideoFormat) {
                videoFormat = findFormat(mediaExtractor, "video/")
                Log.e("lzp", videoFormat.toString())

                findVideoFormat = videoFormat != null
            }

            mediaExtractor.release()
            if (findAudioFormat && findVideoFormat) {
                break
            }

        }

        var mediaMuxerAudioTrackIndex = 0
        // 合成文件添加指定格式的音轨
        if (findAudioFormat) {
            mediaMuxerAudioTrackIndex = mediaMuxer.addTrack(audioFormat!!)
        }
        // 合成文件添加指定格式的视轨
        var mediaMuxerVideoTrackIndex = 0
        if (findVideoFormat) {
            mediaMuxerVideoTrackIndex =  mediaMuxer.addTrack(videoFormat!!)
        }

        // 开始合成
        mediaMuxer.start()

        // 音频的时间戳
        var audioPts = 0L
        var videoPts = 0L
        for (file in videoList) {

            var hasAudio = false
            var hasVideo = false


            // 文件的音轨
            val audioMediaExtractor = MediaExtractor()
            audioMediaExtractor.setDataSource(file.absolutePath)
            val audioTrackIndex = findTrackIndex(audioMediaExtractor, "audio/")
            if (audioTrackIndex >= 0) {
                audioMediaExtractor.selectTrack(audioTrackIndex)
                hasAudio = true
            }

            val videoMediaExtractor = MediaExtractor()
            videoMediaExtractor.setDataSource(file.absolutePath)
            val videoTrackIndex = findTrackIndex(videoMediaExtractor, "video/")
            if (videoTrackIndex >= 0) {
                videoMediaExtractor.selectTrack(videoTrackIndex)
                hasVideo = true
            }

            // 如果音频视频都没有，直接跳过该文件
            if (!hasAudio && !hasVideo) {
                audioMediaExtractor.release()
                videoMediaExtractor.release()
                continue
            }

            // 写入音轨
            if (hasAudio) {
                var hasDone = false
                var lastPts = 0L
                while (!hasDone) {
                    mReadBuffer.rewind()
                    val frameSize = audioMediaExtractor.readSampleData(mReadBuffer, 0)
                    if (frameSize < 0) {
                        hasDone = true
                    } else {
                        val bufferInfo = MediaCodec.BufferInfo()
                        bufferInfo.offset = 0
                        bufferInfo.size = frameSize
                        bufferInfo.presentationTimeUs = audioPts + audioMediaExtractor.sampleTime
                        if ((audioMediaExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }

                        mReadBuffer.rewind()
                        mediaMuxer.writeSampleData(mediaMuxerAudioTrackIndex, mReadBuffer, bufferInfo)
                        audioMediaExtractor.advance()
                        if (audioMediaExtractor.sampleTime > 0) {
                            lastPts = audioMediaExtractor.sampleTime
                        }
                    }
                }
                audioPts += lastPts
                audioMediaExtractor.release()
            }

            // 写入视频
            if (hasVideo) {
                var hasDone = false
                var lastPts = 0L
                while (!hasDone) {
                    mReadBuffer.rewind()
                    val frameSize = videoMediaExtractor.readSampleData(mReadBuffer, 0)
                    if (frameSize < 0) {
                        hasDone = true
                    } else {
                        val bufferInfo = MediaCodec.BufferInfo()
                        bufferInfo.offset = 0
                        bufferInfo.size = frameSize
                        bufferInfo.presentationTimeUs = videoPts + videoMediaExtractor.sampleTime
                        if ((videoMediaExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }

                        mReadBuffer.rewind()
                        mediaMuxer.writeSampleData(mediaMuxerVideoTrackIndex, mReadBuffer, bufferInfo)
                        videoMediaExtractor.advance()
                        if (videoMediaExtractor.sampleTime > 0) {
                            lastPts = videoMediaExtractor.sampleTime
                        }
                    }
                }
                videoPts += lastPts
                videoMediaExtractor.release()
            }

            // 录制完一个文件，时间戳对齐
            audioPts = max(audioPts, videoPts)
            videoPts =audioPts
        }

        mediaMuxer.stop()
        mediaMuxer.release()
        finish.invoke()

    }

    private fun findFormat(mediaExtractor: MediaExtractor, prefix: String): MediaFormat? {
        for (i in 0 until mediaExtractor.trackCount) {
            val format = mediaExtractor.getTrackFormat(i)
            val mime = format.getString("mime")
            if (mime.startsWith(prefix)) {
                return format
            }
        }
        return null
    }

    private fun findTrackIndex(mediaExtractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until mediaExtractor.trackCount) {
            val format = mediaExtractor.getTrackFormat(i)
            val mime = format.getString("mime")
            if (mime.startsWith(prefix)) {
                return i
            }
        }
        return -1
    }


}