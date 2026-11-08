package com.arborecence.core.data.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsHelper @Inject constructor(@ApplicationContext context: Context) : TextToSpeech.OnInitListener {
    private val tts: TextToSpeech = TextToSpeech(context, this)
    @Volatile private var ready = false
    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale("pt", "BR")
    }
    fun speak(text: String) { if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arborecence") }
    fun stop() = tts.stop()
    fun shutdown() = tts.shutdown()
    fun isReady() = ready
}
