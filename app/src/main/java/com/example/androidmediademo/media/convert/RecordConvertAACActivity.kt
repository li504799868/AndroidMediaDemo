package com.example.androidmediademo.media.convert

import android.media.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import com.example.androidmediademo.media.play.mediacodec.AudioMediaCodecWorker
import kotlinx.android.synthetic.main.activity_record_convert_a_a_c.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * @author li.zhipeng
 *
 *      录制音频直接输出aac文件
 * */
class RecordConvertAACActivity : AppCompatActivity() {

    private var recorder: AudioRecord? = null

    private var recordFile: File? = null

    private var isRecording = false

    private val mediaPlayer = MediaPlayer()

    companion object {
        const val SAMPLE_RATE = 44100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_convert_a_a_c)

        start_record.setOnClickListener {
            RecordThread().start()
        }

        stop_record.setOnClickListener {
            stopRecord()
        }

        player.setOnClickListener {
            play()
        }
    }

    private var startUs = 0L

    private fun stopRecord() {
        isRecording = false
    }

    private fun play() {
        recordFile?.let {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.setDataSource(it.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
        }
    }

    private inner class RecordThread : Thread() {

        override fun run() {
            super.run()
            name = "RecordThread"
            startRecord()
        }

        private fun startRecord() {
            isRecording = true
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
            )

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize*5
            )

            recorder!!.startRecording()
            startUs = System.nanoTime()
            // 创建文件
            recordFile = File("${filesDir}/${System.currentTimeMillis()}.aac")
            recordFile!!.parentFile.mkdirs()
            val adts = ByteArray(7)
            val fous = FileOutputStream(recordFile).channel
            // 创建编码器
            val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)

            val audioFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                SAMPLE_RATE,
                2
            )
            audioFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            audioFormat.setInteger(
                MediaFormat.KEY_CHANNEL_MASK,
                AudioFormat.CHANNEL_IN_STEREO
            )
            // 不能设置这个属性
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4100)
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
            mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()

            var pcmSize = 0
            val recordedBytes = ByteArray(minBufferSize)
            loop@ while (isRecording) {

                // 方案1
                val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
                if (inputBufferIndex >= 0) {
                    val buffer = mediaCodec.getInputBuffer(inputBufferIndex) ?: continue
                    buffer.clear()
                    val readSize = recorder!!.read(buffer, buffer.capacity())

                    // byte是8位，因为是要求16位的长度，所以要/2
                    // 除以sampleRate（1秒采集的样本数量）
                    // 除以声道数
                    // 时间单位秒秒
                    val presentationTimeUs = pcmSize * 1000000L / 2 / SAMPLE_RATE / 2
                    pcmSize += readSize
                    Log.e("lzp", "inputBufferIndex: $inputBufferIndex, readSize: $readSize, pcmSize:$pcmSize, presentationTimeUs:$presentationTimeUs")
//                    val presentationTimeUs = (System.nanoTime() - startUs) / 1000L

                    mediaCodec.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        readSize,
                        presentationTimeUs,
                        0
                    )
                }

                // 方案2
//                val resultRead = recorder!!.read(recordedBytes, 0, recordedBytes.size)
//                if (resultRead == AudioRecord.ERROR_BAD_VALUE || resultRead == AudioRecord.ERROR_INVALID_OPERATION) {
//                    break
//                }
//
//                val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
//                if (inputBufferIndex >= 0) {
//                    val buffer = mediaCodec.getInputBuffer(inputBufferIndex) ?: continue
//                    buffer.clear()
                // 此处会报错
//                    buffer.put(recordedBytes, 0, resultRead)
//                    mediaCodec.queueInputBuffer(
//                        inputBufferIndex,
//                        0,
//                        buffer.limit(),
//                        (System.nanoTime() - startUs) / 1000L,
//                        0
//                    )
//                }

                // 得到编码的结果
                val bufferInfo = MediaCodec.BufferInfo()
                when (val outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)) {
                    // 此类型已经废弃，如果使用的是getOutputBuffer（）可以忽略此状态
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Log.e("lzp", "outIndex:$outIndex")
                        // 当buffer的格式发生改变，须指向新的buffer格式
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Log.e("lzp", "outIndex:$outIndex")
                        // 当buffer的格式发生改变，须指向新的buffer格式
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Log.e("lzp", "outIndex:$outIndex")
                        // 当dequeueOutputBuffer超时时，会到达此case
                        Log.e(AudioMediaCodecWorker.TAG, ">> dequeueOutputBuffer timeout")
                    }
                    else -> {
                        Log.e("lzp", "outIndex:$outIndex")
                        val outBuffer = mediaCodec.getOutputBuffer(outIndex) ?: continue@loop

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            // The codec config data was pulled out and fed
                            // to the muxer when we got
                            // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore
                            // it.
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            fillADTS(
                                adts,
                                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                                SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_STEREO,
                                bufferInfo.size + 7
                            )
                            fous.write(ByteBuffer.wrap(adts))
                            fous.write(outBuffer)

                        }
                        mediaCodec.releaseOutputBuffer(outIndex, false)
                    }
                }

            }
            recorder?.stop()
            recorder?.release()
            mediaCodec.stop()
            mediaCodec.release()
            fous.close()
            recorder = null
        }


        fun fillADTS(
            packet: ByteArray,
            level: Int,
            audio_sample_rates: Int,
            chanel: Int,
            packetLen: Int
        ) {
            var profile = 2 //AAC LC，MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            var freqIdx = 4 //32K, 见后面注释avpriv_mpeg4audio_sample_rates中32000对应的数组下标，来自ffmpeg源码
            var chanCfg = 1 //见后面注释channel_configuration，Stero双声道立体声
            when (level) {
                MediaCodecInfo.CodecProfileLevel.AACObjectLC -> profile = 2
                else -> {
                }
            }
            for (i in SIMPLE_RATE_TABLE.indices) {
                if (audio_sample_rates == SIMPLE_RATE_TABLE[i]
                ) {
                    freqIdx = i
                }
            }
            when (chanel) {
                AudioFormat.CHANNEL_IN_MONO -> chanCfg = 1
                AudioFormat.CHANNEL_IN_STEREO -> chanCfg = 2
                else -> {
                }
            }
            packet[0] = 0xFF.toByte()
            packet[1] = 0xF9.toByte()
            packet[2] =
                ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
            packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
            packet[4] = (packetLen and 0x7FF shr 3).toByte()
            packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
            packet[6] = 0xFC.toByte()
        }

        private val SIMPLE_RATE_TABLE = intArrayOf(
            96000, 88200, 64000, 48000, 44100, 32000,
            24000, 22050, 16000, 12000, 11025, 8000, 7350
        )
    }


}
