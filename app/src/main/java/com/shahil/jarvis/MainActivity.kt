package com.shahil.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var speech: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var listening = false
    private var awake = false
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 60, 30, 30)
            setBackgroundColor(0xFF050D1C.toInt())
        }

        val title = TextView(this).apply {
            text = "J.A.R.V.I.S."
            textSize = 32f
            setTextColor(0xFF42BFFF.toInt())
            gravity = 17
        }

        status = TextView(this).apply {
            text = "Initializing..."
            textSize = 18f
            setTextColor(-1)
            gravity = 17
            setPadding(0, 40, 0, 40)
        }

        val button = Button(this).apply {
            text = "START JARVIS"
            setOnClickListener {
                requestMicrophone()
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(button)
        setContentView(layout)

        tts = TextToSpeech(this, this)
    }

    private fun requestMicrophone() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                10
            )
        } else {
            startJarvis()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 10 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startJarvis()
        } else {
            status.text = "Microphone permission required"
        }
    }

    private fun startJarvis() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition unavailable"
            return
        }

        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this)

        speech?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                status.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                status.text = "Hearing you..."
            }

            override fun onEndOfSpeech() {
                status.text = "Processing..."
            }

            override fun onResults(results: Bundle?) {
                val words = results
                    ?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""

                processCommand(words)

                if (listening) {
                    status.postDelayed({ listen() }, 1000)
                }
            }

            override fun onError(error: Int) {
                if (listening) {
                    status.postDelayed({ listen() }, 1200)
                }
            }

            override fun onRmsChanged(value: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(bundle: Bundle?) {}
            override fun onEvent(type: Int, bundle: Bundle?) {}
        })

        listening = true
        listen()
    }

    private fun listen() {
        if (!listening) return

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN"
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speech?.startListening(intent)
        } catch (_: Exception) {
            status.text = "Please try again"
        }
    }

    private fun processCommand(command: String) {
        status.text = command

        if (!awake) {
            if (command.contains("hey jarvis") ||
                command.contains("jarvis")) {
                awake = true
                speak("Yes Sir. I am listening.")
            }
            return
        }

        val answer = when {
            command.contains("time") ->
                "The current time is " +
                java.text.SimpleDateFormat(
                    "h:mm a", Locale.getDefault()
                ).format(java.util.Date())

            command.contains("date") ->
                "Today is " +
                java.text.SimpleDateFormat(
                    "EEEE, d MMMM yyyy",
                    Locale.getDefault()
                ).format(java.util.Date())

            command.contains("hello") ||
            command.contains("how are you") ->
                "I am ready to assist you, Sir."

            command.contains("sleep") ->
                "Going to sleep, Sir."

            command.contains("open settings") -> {
                startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                )
                "Opening settings, Sir."
            }

            else ->
                "I heard you, Sir. This feature will be added soon."
        }

        if (command.contains("sleep")) {
            awake = false
        }

        speak(answer)
    }

    private fun speak(message: String) {
        status.text = message
        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_reply"
        )
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale("en", "IN"))
            tts?.setSpeechRate(0.9f)
            status.text = "JARVIS READY"
        }
    }

    override fun onDestroy() {
        listening = false
        speech?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
