package com.example.domaassistente

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings

// Saídas de áudio do relógio (passos 2, 3 e 4 do roteiro)
class AudioHelper(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var callback: AudioDeviceCallback? = null

    // passo 2 (igual ao exemplo do roteiro)
    fun audioOutputAvailable(type: Int): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            return false
        }
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type == type }
    }

    fun temAltoFalante() = audioOutputAvailable(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

    fun temFoneBluetooth(): Boolean {
        if (audioOutputAvailable(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)) return true
        // Fones Bluetooth LE (Android 12+)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            audioOutputAvailable(AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    fun temSaidaDeAudio() = temAltoFalante() || temFoneBluetooth()

    // passo 3: registerAudioDeviceCallback
    fun observarFones(aoMudar: (conectado: Boolean) -> Unit) {
        pararDeObservar()
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesAdded(addedDevices)
                if (addedDevices.orEmpty().any { it.isSink && ehBluetooth(it.type) }) {
                    aoMudar(true)   // Um fone de ouvido Bluetooth acabou de ser conectado
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesRemoved(removedDevices)
                if (removedDevices.orEmpty().any { it.isSink && ehBluetooth(it.type) }) {
                    aoMudar(false)  // Um fone de ouvido Bluetooth não está mais conectado
                }
            }
        }
        audioManager.registerAudioDeviceCallback(cb, null)
        callback = cb
    }

    fun pararDeObservar() {
        callback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        callback = null
    }

    // passo 4: abre direto a tela de Bluetooth em vez de mostrar erro
    fun abrirConfiguracoesBluetooth() {
        val intent = with(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("EXTRA_CONNECTION_ONLY", true)
            putExtra("EXTRA_CLOSE_ON_CONNECT", true)
            putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1)
        }
        context.startActivity(intent)
    }

    private fun ehBluetooth(type: Int) =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET)
}
