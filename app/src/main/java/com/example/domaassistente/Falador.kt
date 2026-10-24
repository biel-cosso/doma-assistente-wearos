package com.example.domaassistente

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

// Leitura em voz alta (TextToSpeech) em português - passo 5 do roteiro
class Falador(context: Context, private val aoFalhar: (String) -> Unit) {

    private var pronto = false
    private val pendentes = mutableListOf<Pair<String, Boolean>>()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status != TextToSpeech.SUCCESS) {
            aoFalhar("Leitura em voz alta indisponível neste relógio")
            return@TextToSpeech
        }
        configurarIdioma()
        pronto = true
        pendentes.forEach { (texto, alerta) -> falar(texto, alerta, interromper = false) }
        pendentes.clear()
    }

    private fun configurarIdioma() {
        val r = tts.setLanguage(Locale("pt", "BR"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            aoFalhar("Voz em português não instalada; usando a voz padrão")
        }
    }

    // alerta = true usa o volume de alarme, pra ser ouvido mesmo com o volume de mídia baixo
    fun falar(texto: String, alerta: Boolean = false, interromper: Boolean = true) {
        if (!pronto) {
            pendentes += texto to alerta
            return
        }
        val params = Bundle()
        if (alerta) params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
        val modo = if (interromper) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(texto, modo, params, texto.hashCode().toString())
    }

    fun falarTodos(textos: List<String>, alerta: Boolean = false) {
        textos.forEachIndexed { i, t -> falar(t, alerta, interromper = i == 0) }
    }

    fun parar() = tts.stop()

    fun desligar() {
        tts.stop()
        tts.shutdown()
    }
}
