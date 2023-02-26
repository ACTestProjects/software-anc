package com.ac.softwareanc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.concurrent.thread
import kotlin.experimental.inv
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var audioMan: AudioManager
    private lateinit var audioPlr: AudioTrack
    private lateinit var audioThread: Thread
    private var sampleRate by Delegates.notNull<Int>()
    private var bufferSize by Delegates.notNull<Int>()
    private var forceOldRec: Boolean = true
    private var isRecording: Boolean = false
    private var isInverting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioMan = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setContentView(R.layout.activity_main)
        checkAvailability()

        sampleRate = getOptimalSampleRate()
        bufferSize = getOptimalBufferSize()
        Toast.makeText(this, "Debug: Sample Rate = " + sampleRate + ", buffer size = " + bufferSize, Toast.LENGTH_SHORT).show()
        audioPlr = AudioTrack(
            AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO) .build(),
            bufferSize * 2, AudioTrack.MODE_STREAM, audioMan.generateAudioSessionId())

        findViewById<RadioGroup>(R.id.modeGroup).setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.modeSurrounding -> updateRecorder(record = true, invert = false);
                R.id.modeNoCancel -> updateRecorder(record = false, invert = false);
                R.id.modeNoiseCancellation -> updateRecorder(record = true, invert = true);
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.useoldrec_swc -> {
                if (forceOldRec)
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.warn_new_method_header))
                        .setMessage(getString(R.string.warn_new_method_text))
                        .setPositiveButton(android.R.string.yes) { _, _ -> }
                        .setNegativeButton(R.string.anyway) { _, _ ->
                            run {
                                item.isChecked = !item.isChecked;
                                forceOldRec = item.isChecked;
                                updateRecorder(isRecording, isInverting);
                            }
                        }
                        .show()
                else {
                    item.isChecked = !item.isChecked;
                    forceOldRec = item.isChecked;
                    updateRecorder(isRecording, isInverting);
                }
            }
        }
        return false
    }

    private external fun checkNativeLibrary() : Int;

    private fun checkAvailability() {
        if (!audioMan.isWiredHeadsetOn) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.err_no_wired_headphones_header))
                .setMessage(getString(R.string.err_no_wired_headphones_text))
                .setPositiveButton(android.R.string.yes) { _, _ -> finishAffinity(); }
                .setNegativeButton(R.string.anyway) { _, _ -> }
                .setCancelable(false)
                .show()
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.err_text))
                .setMessage(getString(R.string.err_no_permission))
                .setPositiveButton(android.R.string.yes) { _, _ -> finishAffinity(); }
                .setCancelable(false)
                .show()
        }
    }

    private fun getOptimalSampleRate() : Int {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRateStr: String? = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        var sampleRate: Int = sampleRateStr?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 44100
        return sampleRate
    }

    private fun getOptimalBufferSize() : Int {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val framesPerBuffer: String? = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        var framesPerBufferInt: Int = framesPerBuffer?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 256
        return framesPerBufferInt
    }

    private fun updateRecorder(record: Boolean, invert: Boolean) {
        isRecording = record
        isInverting = invert
        stopRecorderNew()
        if (forceOldRec) {
            updateRecorderOld(record, invert);
        }
        else if (checkNativeLibrary() != 8328){
            Toast.makeText(this, "Native library is not available. Falling back to Kotlin recorder...", Toast.LENGTH_SHORT).show();
            updateRecorderOld(record, invert);
        } else {
            // Toast.makeText(this, "Native library is available!", Toast.LENGTH_SHORT).show();
            if (!updateRecorderNew(sampleRate, bufferSize, record, invert)){
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.err_text))
                    .setMessage(getString(R.string.err_newmethodfailed))
                    .setPositiveButton(android.R.string.yes) { _, _ -> finishAffinity(); }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private external fun updateRecorderNew(sampleRate: Int, bufferSize: Int, record: Boolean, invert: Boolean) : Boolean
    private external fun stopRecorderNew() : Boolean

    private fun updateRecorderOld(record: Boolean, invert: Boolean) {
        audioThread = thread (start = true) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.err_text))
                    .setMessage(getString(R.string.err_no_permission))
                    .setPositiveButton(android.R.string.yes) { _, _ -> finishAffinity(); }
                    .show()
            } else {
                if(isRecording){
                    val sr = sampleRate
                    val cc = AudioFormat.CHANNEL_IN_STEREO
                    val af = AudioFormat.ENCODING_PCM_16BIT
                    val minBufferSize = AudioRecord.getMinBufferSize(sr, cc, af)
                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sr, cc, af,
                        minBufferSize
                    )

                    recorder.startRecording()
                    audioPlr.play()
                    val size = bufferSize
                    val buffer = ShortArray(size)
                    while (isRecording && audioThread.id == Thread.currentThread().id) {
                        recorder.read(buffer, 0, size)
                        if (isInverting)
                            repeat(size) {
                                buffer[it] = (-buffer[it]).toShort()
                            }
                        audioPlr.write(buffer, 0, size)
                    }
                    audioPlr.stop()
                    recorder.stop()
                    recorder.release()
                }
                else { }
            }
        }
    }
    companion object {
        init {
            System.loadLibrary("softwareanc")
        }
    }
}