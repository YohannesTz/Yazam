package com.github.yohannestz.yazam

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.yohannestz.yazam.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var recordingOn = false
    private lateinit var floatarray: FloatArray
    private var arcounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()

        PermissionX.init(this)
            .permissions(android.Manifest.permission.RECORD_AUDIO)
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "All permissions are granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "These permissions are denied: $deniedList", Toast.LENGTH_LONG).show()
                }
            }

        binding.button8.setOnClickListener {
            if (!recordingOn) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        Toast.makeText(this, "Now recording", Toast.LENGTH_SHORT).show()
        val SAMPLE_RATE = 8000
        recordingOn = true
        Thread(Runnable {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            var bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                bufferSize = SAMPLE_RATE * 2
            }
            val audioBuffer = ShortArray(bufferSize / 2)
            floatarray =
                FloatArray(9000000) // warning this a fixed size array adjust it to your needs
            arcounter = 0
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "Error in audio recorder state", Toast.LENGTH_SHORT)
                    .show()
                return@Runnable
            }
            record.startRecording()
            while (recordingOn) {
                // was needed to pass to the buffer.
                val numberOfShort = record.read(audioBuffer, 0, audioBuffer.size)
                for (i in audioBuffer.indices) {
                    floatarray[arcounter] = audioBuffer[i].toFloat()
                    arcounter++
                }
            }
            record.stop()
            record.release()
        }).start()
    }

    private fun stopRecording() {
        Toast.makeText(this, "stopped", Toast.LENGTH_SHORT).show()
        recordingOn = false

        Log.e("passedData: ", floatarray.size.toString() + ", " + arcounter.toString())
        val res = passingDataToJni(floatarray, arcounter)
        val textDisplayed = "res:\n\n $res"
        binding.sampleText.text = textDisplayed
    }
    /**
     * A native method that is implemented by the 'yazam' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    private external fun passingDataToJni(audio_data: FloatArray?, array_length: Int): String?

    companion object {
        init {
            System.loadLibrary("yazam")
        }
    }
}