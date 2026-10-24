package com.example.domaassistente

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.util.Locale

// Tela principal do app: lista de avisos, leitura em voz alta e comandos de voz
class MainActivity : ComponentActivity() {

    private lateinit var audio: AudioHelper
    private lateinit var falador: Falador
    private lateinit var txtStatus: TextView

    private val avisos = Aviso.exemplos()
    private val handler = Handler(Looper.getMainLooper())

    // comandos aceitos (falando ou escolhendo na lista)
    private val comandos = listOf(
        "Ler tudo", "Ler mensagens", "Ler alertas", "Emergência", "Conectar fone", "Parar", "Ajuda",
    )

    private val reconhecerVoz =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            val frase = resultado.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            // no emulador o microfone não funciona, então mostra a lista de comandos.
            // sem esse atraso o mesmo "voltar" que fecha a tela de voz fechava a lista também
            // (descobri testando no emulador)
            if (resultado.resultCode == Activity.RESULT_OK && frase != null) executarComando(frase)
            else handler.postDelayed({ escolherComando() }, 400)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audio = AudioHelper(this)
        falador = Falador(this) { msg -> runOnUiThread { mostrar(msg) } }

        txtStatus = findViewById(R.id.txtStatus)
        txtStatus.setOnClickListener {
            if (audio.temSaidaDeAudio()) falar(textoStatus().replace(Regex("[🔊🔇🎧·]"), ",")) else oferecerBluetooth()
        }

        val lista = findViewById<ListView>(R.id.listaAvisos)
        lista.adapter = AvisoAdapter()
        lista.setOnItemClickListener { _, _, posicao, _ -> lerAviso(avisos[posicao]) }

        val btnVoz = findViewById<Button>(R.id.btnVoz)
        btnVoz.setOnClickListener { ouvirComando() }
        btnVoz.setOnLongClickListener { escolherComando(); true }

        atualizarStatus()
        val alertas = avisos.count { it.tipo == Aviso.Tipo.ALERTA }
        val totalAvisos = if (avisos.size == 1) "1 aviso" else "${avisos.size} avisos"
        val totalAlertas = if (alertas == 1) "1 alerta" else "$alertas alertas"
        falar("Doma Assistente pronto. Você tem $totalAvisos, sendo $totalAlertas de segurança.")
    }

    override fun onStart() {
        super.onStart()
        // passo 3: avisa quando o fone conecta ou desconecta
        audio.observarFones { conectado ->
            atualizarStatus()
            if (conectado) falar("Fone Bluetooth conectado.")
            else mostrar("Fone Bluetooth desconectado")
        }
        atualizarStatus()
    }

    override fun onStop() {
        audio.pararDeObservar()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        falador.desligar()
        super.onDestroy()
    }


    private fun textoStatus(): String {
        val falante = if (audio.temAltoFalante()) "🔊 Alto-falante" else "🔇 Sem alto-falante"
        val fone = if (audio.temFoneBluetooth()) "🎧 Fone conectado" else "🎧 Sem fone"
        return "$falante · $fone"
    }

    private fun atualizarStatus() {
        txtStatus.text = if (audio.temSaidaDeAudio()) textoStatus() else "🔇 Sem saída de som. Toque para conectar um fone"
        txtStatus.setTextColor(
            ContextCompat.getColor(this, if (audio.temSaidaDeAudio()) R.color.texto_secundario else R.color.texto_alerta)
        )
    }

    // só fala se tiver saída de som, senão oferece conectar um fone (passo 4)
    private fun falar(texto: String, alerta: Boolean = false) {
        if (!audio.temSaidaDeAudio()) {
            mostrar(texto)
            oferecerBluetooth()
            return
        }
        falador.falar(texto, alerta)
    }

    private fun oferecerBluetooth() {
        AlertDialog.Builder(this)
            .setTitle("Sem saída de som")
            .setMessage("Conecte um fone Bluetooth para ouvir os avisos.")
            .setPositiveButton("Conectar") { _, _ -> audio.abrirConfiguracoesBluetooth() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun lerAviso(aviso: Aviso) {
        if (aviso.tipo == Aviso.Tipo.ALERTA) {
            tocarAlarme()
            // espera o bipe acabar pra começar a falar
            handler.postDelayed({ falar(aviso.paraFala(), alerta = true) }, DURACAO_ALARME_MS + 200)
        } else {
            falar(aviso.paraFala())
        }
    }

    private fun lerVarios(filtro: (Aviso) -> Boolean, vazio: String) {
        val selecionados = avisos.filter(filtro)
        if (selecionados.isEmpty()) return falar(vazio)
        if (!audio.temSaidaDeAudio()) return oferecerBluetooth()
        val temAlerta = selecionados.any { it.tipo == Aviso.Tipo.ALERTA }
        if (temAlerta) tocarAlarme()
        handler.postDelayed(
            { falador.falarTodos(selecionados.map { it.paraFala() }, alerta = temAlerta) },
            if (temAlerta) DURACAO_ALARME_MS + 200 else 0,
        )
    }

    // bipe + vibração dos alertas de segurança
    private fun tocarAlarme() {
        try {
            val tom = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tom.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, DURACAO_ALARME_MS.toInt())
            handler.postDelayed({ tom.release() }, DURACAO_ALARME_MS + 100)
        } catch (e: RuntimeException) {
            // se não conseguir tocar o bipe, continua só com a voz e a vibração
        }
        vibrar()
    }

    private fun vibrar() {
        val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (vibrador?.hasVibrator() == true) {
            vibrador.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
        }
    }


    private fun ouvirComando() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voz_pergunta))
        }
        try {
            reconhecerVoz.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // relógio sem reconhecimento de voz
            mostrar("Reconhecimento de voz indisponível")
            escolherComando()
        }
    }

    // lista de comandos pra tocar (abre também com toque longo no botão)
    private fun escolherComando() {
        AlertDialog.Builder(this)
            .setTitle(R.string.escolha_comando)
            .setItems(comandos.toTypedArray()) { _, i -> executarComando(comandos[i]) }
            .show()
    }

    private fun executarComando(frase: String) {
        val f = normalizar(frase)
        when {
            "emergencia" in f || "socorro" in f || "ajuda urgente" in f -> emergencia()
            "parar" in f || "silencio" in f || "pare" in f -> falador.parar()
            "alerta" in f || "seguranca" in f ->
                lerVarios({ it.tipo == Aviso.Tipo.ALERTA }, "Nenhum alerta de segurança no momento.")
            "mensage" in f || "lembrete" in f || "recado" in f ->
                lerVarios({ it.tipo != Aviso.Tipo.ALERTA }, "Nenhuma mensagem nova.")
            "tudo" in f || "todos" in f || "ler" in f ->
                lerVarios({ true }, "Nenhum aviso.")
            "fone" in f || "bluetooth" in f -> audio.abrirConfiguracoesBluetooth()
            "ajuda" in f || "comando" in f ->
                falar("Você pode dizer: ler tudo, ler mensagens, ler alertas, emergência, conectar fone ou parar.")
            else -> falar("Não entendi \"$frase\". Diga ajuda para ouvir os comandos.")
        }
    }

    // simulação: toca o alarme e fala a instrução, não chama ninguém de verdade
    private fun emergencia() {
        tocarAlarme()
        handler.postDelayed({
            falar("Emergência acionada. Mantenha a calma e siga a sinalização até a saída mais próxima.", alerta = true)
        }, DURACAO_ALARME_MS + 200)
    }

    private fun normalizar(s: String) =
        Normalizer.normalize(s.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")

    private fun mostrar(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // alertas de segurança aparecem em laranja na lista
    private inner class AvisoAdapter : ArrayAdapter<Aviso>(this, R.layout.item_aviso, avisos) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = super.getView(position, convertView, parent) as TextView
            val alerta = getItem(position)?.tipo == Aviso.Tipo.ALERTA
            v.setTextColor(if (alerta) ContextCompat.getColor(context, R.color.texto_alerta) else Color.WHITE)
            return v
        }
    }

    companion object {
        private const val DURACAO_ALARME_MS = 1200L
    }
}
