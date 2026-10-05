package com.digitalminds.grow

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    var enabled = true

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.US)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(0.95f)
            }
        }
    }

    fun say(text: String, flush: Boolean = true) {
        if (!enabled || !ready) return
        tts?.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "grow")
    }

    fun stop() { tts?.stop() }
    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }
}
