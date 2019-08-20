package com.example.androidmediademo.media.record

import android.media.*
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.example.androidmediademo.R
import kotlinx.android.synthetic.main.activity_audio_recorder.*
import java.io.*

/**
 * @author li
 *
 *      使用AudioRecorder录制pcm原始音频
 * */
class AudioRecorderActivity : AppCompatActivity() {

    companion object {
        const val ENCODER = AudioFormat.ENCODING_PCM_8BIT
    }

    private var isRecording = false

    /**
     *  录音文件的位置
     * */
    private val filePath = "${Environment.getExternalStorageDirectory()}/test/audio_record.pcm"

    private var recordTask: RecordTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_recorder)

        File(filePath).parentFile.mkdirs()

        // 开始录音
        start_record.setOnClickListener {
            startRecord()
        }

        // 播放录音
        play_record.setOnClickListener {
            playRecord()
        }
    }

    private fun startRecord() {
        if (isRecording) {
            isRecording = false
            recordTask!!.stop()
            start_record.text = "开始录音"
        } else {
            isRecording = true
            recordTask = RecordTask()
            recordTask!!.execute()
            start_record.text = "停止录音"
        }
    }

    private fun playRecord() {
        PlayTaks().execute()
    }

    private fun getMinBufferSize(): Int {
        return AudioRecord.getMinBufferSize(
            11025,
            AudioFormat.CHANNEL_IN_MONO,
            ENCODER
        )
    }

    inner class RecordTask : AsyncTask<Unit, Unit, Unit>() {

        private val audioRecord = createAudioRecord()

        /**
         * 创建AudioRecord对象
         * */
        private fun createAudioRecord(): AudioRecord {
            return AudioRecord(
                MediaRecorder.AudioSource.MIC,
                11025,
                AudioFormat.CHANNEL_IN_MONO,
                ENCODER,
                getMinBufferSize()
            )
        }

        override fun doInBackground(vararg params: Unit) {
            // 开始录音
            audioRecord.startRecording()
            // 通过io流，把录制的音频内容保存到文件中‘
            val dataInputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(filePath)))
            // 16位需要使用short来保存
            if (ENCODER == AudioFormat.ENCODING_PCM_16BIT) {
                saveBy16Bit(dataInputStream)
            }
            // 8位使用byte
            else if (ENCODER == AudioFormat.ENCODING_PCM_8BIT) {
                saveBy8Bit(dataInputStream)
            }
            dataInputStream.flush()
            dataInputStream.close()
        }

        private fun saveBy16Bit(dataInputStream: DataOutputStream) {
            val byteArray = ShortArray(getMinBufferSize())
            while (isRecording) {
                val result = audioRecord.read(byteArray, 0, byteArray.size)
                for (i in 0 until result) {
                    dataInputStream.writeShort(byteArray[i].toInt())
                }
            }
        }

        private fun saveBy8Bit(dataInputStream: DataOutputStream) {
            val byteArray = ByteArray(getMinBufferSize())
            while (isRecording) {
                audioRecord.read(byteArray, 0, byteArray.size)
                dataInputStream.write(
                    byteArray,
                    0,
                    byteArray.size
                )
            }
        }

        fun stop() {
            audioRecord.stop()
            audioRecord.release()
        }
    }

    inner class PlayTaks : AsyncTask<Unit, Unit, Unit>() {

        override fun doInBackground(vararg params: Unit?) {
            val audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(ENCODER)
                    .setSampleRate(11025)
                    .build(),
                getMinBufferSize(),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack.play()

            val dataInputStream = DataInputStream(BufferedInputStream(FileInputStream(filePath)))

            // 读取数据也是一样，16位要读取short
            if (ENCODER == AudioFormat.ENCODING_PCM_16BIT) {
                readBy16Bit(dataInputStream, audioTrack)
            }
            // 8位可以直接读取byte
            else if (ENCODER == AudioFormat.ENCODING_PCM_8BIT) {
                readBy8Bit(dataInputStream, audioTrack)
            }

            dataInputStream.close()
            audioTrack.stop()
            audioTrack.release()
        }

        private fun readBy8Bit(dataInputStream: DataInputStream, audioTrack: AudioTrack) {
            val byteArray = ByteArray(getMinBufferSize())
            while (dataInputStream.available() > 0) {
                dataInputStream.read(byteArray, 0, byteArray.size)
                audioTrack.write(byteArray, 0, byteArray.size)
            }
        }

        private fun readBy16Bit(dataInputStream: DataInputStream, audioTrack: AudioTrack) {
            val byteArray = ShortArray(getMinBufferSize())
            while (dataInputStream.available() > 0) {
                var i = 0
                while (i < byteArray.size) {
                    byteArray[i] = dataInputStream.readShort()
                    i++
                }
                audioTrack.write(byteArray, 0, byteArray.size)
            }
        }

    }

}
