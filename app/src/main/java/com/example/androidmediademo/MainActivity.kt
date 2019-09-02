package com.example.androidmediademo

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.androidmediademo.media.record.AudioRecorderActivity
import com.example.androidmediademo.media.record.MediaRecorderActivity
import com.example.androidmediademo.media.record.br.BreakRecordActivity
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        media_recorder.setOnClickListener {
            startMediaRecorderWithPermissionCheck()
        }

        audio_recorder.setOnClickListener {
            startAudioRecorderWithPermissionCheck()
        }

        break_media_recorder.setOnClickListener {
            startBreakMediaRecorderWithPermissionCheck()
        }
    }

    @NeedsPermission(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )
    fun startMediaRecorder() {
        startActivity(Intent(this@MainActivity, MediaRecorderActivity::class.java))
    }

    @NeedsPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )
    fun startAudioRecorder() {
        startActivity(Intent(this@MainActivity, AudioRecorderActivity::class.java))
    }

    @NeedsPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    fun startBreakMediaRecorder() {
        startActivity(Intent(this@MainActivity, BreakRecordActivity::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
}
