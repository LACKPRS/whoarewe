package com.example.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.CallState
import com.example.model.VoipCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.sqrt

/**
 * Peer-to-peer VoIP engine operating directly over Wi-Fi LAN without internet servers.
 * Uses UDP datagrams with AudioRecord for microphone streaming and AudioTrack for playback.
 */
class LocalVoipEngine(private val context: Context) {
    private val TAG = "LocalVoipEngine"

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _callState = MutableStateFlow(VoipCallState())
    val callState: StateFlow<VoipCallState> = _callState.asStateFlow()

    private var udpSocket: DatagramSocket? = null
    private var rxJob: Job? = null
    private var txJob: Job? = null
    private var timerJob: Job? = null
    private var simulatedWaveformJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    var voipPort: Int = 50555
        private set

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val PACKET_TYPE_SIGNAL = 1.toByte()
        const val PACKET_TYPE_AUDIO = 2.toByte()
        const val FRAME_SIZE_BYTES = 640 // 20ms of 16kHz 16-bit mono PCM (16000 * 2 / 50 = 640)
    }

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening(): Int {
        if (udpSocket != null && !udpSocket!!.isClosed) return voipPort

        try {
            udpSocket = try {
                DatagramSocket(50555)
            } catch (e: Exception) {
                DatagramSocket(0)
            }
            voipPort = udpSocket?.localPort ?: 50555
            Log.d(TAG, "VoIP UDP Socket listening on port $voipPort")

            rxJob = scope.launch {
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    try {
                        val sock = udpSocket ?: break
                        sock.receive(packet)
                        val dataLen = packet.length
                        if (dataLen < 2) continue

                        val packetType = buffer[0]
                        if (packetType == PACKET_TYPE_SIGNAL) {
                            val signalStr = String(buffer, 1, dataLen - 1)
                            handleSignal(signalStr, packet.address.hostAddress ?: "", packet.port)
                        } else if (packetType == PACKET_TYPE_AUDIO) {
                            if (_callState.value.state == CallState.CONNECTED) {
                                val audioLen = dataLen - 1
                                val level = calculateRmsLevel(buffer, 1, audioLen)

                                _callState.value = _callState.value.copy(
                                    remoteLevel = level,
                                    packetsReceived = _callState.value.packetsReceived + 1,
                                    audioBytesTransferred = _callState.value.audioBytesTransferred + audioLen
                                )

                                playAudioBuffer(buffer, 1, audioLen)
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                        Log.w(TAG, "VoIP UDP receive error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting VoIP UDP socket: ${e.message}")
        }
        return voipPort
    }

    private suspend fun handleSignal(signal: String, fromHost: String, fromPort: Int) {
        Log.d(TAG, "VoIP received signal: $signal from $fromHost:$fromPort")
        when {
            signal.startsWith("CALL_REQ:") -> {
                val parts = signal.split(":")
                val callerUsername = parts.getOrNull(1) ?: "Peer"
                val callerDisplayName = parts.getOrNull(2) ?: callerUsername

                if (_callState.value.state == CallState.IDLE) {
                    _callState.value = VoipCallState(
                        state = CallState.INCOMING_RINGING,
                        peerUsername = callerUsername,
                        peerDisplayName = callerDisplayName,
                        peerHost = fromHost,
                        peerVoipPort = fromPort
                    )
                } else {
                    sendSignalDirect("CALL_BUSY", fromHost, fromPort)
                }
            }
            signal == "CALL_ACCEPT" -> {
                if (_callState.value.state == CallState.OUTGOING_RINGING) {
                    _callState.value = _callState.value.copy(
                        state = CallState.CONNECTED,
                        peerHost = fromHost,
                        peerVoipPort = fromPort
                    )
                    startCallAudio()
                    startCallTimer()
                }
            }
            signal == "CALL_REJECT" || signal == "CALL_BUSY" -> {
                _callState.value = _callState.value.copy(
                    state = CallState.ENDED,
                    errorMessage = if (signal == "CALL_BUSY") "Peer is busy" else "Call declined"
                )
                stopCallAudio()
                scope.launch {
                    delay(2000)
                    _callState.value = VoipCallState(state = CallState.IDLE)
                }
            }
            signal == "CALL_END" -> {
                _callState.value = _callState.value.copy(state = CallState.ENDED)
                stopCallAudio()
                scope.launch {
                    delay(1500)
                    _callState.value = VoipCallState(state = CallState.IDLE)
                }
            }
        }
    }

    fun initiateCall(
        myUsername: String,
        myDisplayName: String,
        targetHost: String,
        targetPort: Int,
        peerUsername: String,
        peerDisplayName: String
    ) {
        _callState.value = VoipCallState(
            state = CallState.OUTGOING_RINGING,
            peerUsername = peerUsername,
            peerDisplayName = peerDisplayName,
            peerHost = targetHost,
            peerVoipPort = targetPort
        )

        scope.launch {
            sendSignalDirect("CALL_REQ:$myUsername:$myDisplayName", targetHost, targetPort)
        }
    }

    fun acceptIncomingCall() {
        val current = _callState.value
        if (current.state != CallState.INCOMING_RINGING) return

        _callState.value = current.copy(state = CallState.CONNECTED)
        scope.launch {
            sendSignalDirect("CALL_ACCEPT", current.peerHost, current.peerVoipPort)
            startCallAudio()
            startCallTimer()
        }
    }

    fun declineIncomingCall() {
        val current = _callState.value
        scope.launch {
            sendSignalDirect("CALL_REJECT", current.peerHost, current.peerVoipPort)
            _callState.value = VoipCallState(state = CallState.IDLE)
        }
    }

    fun endCall() {
        val current = _callState.value
        scope.launch {
            if (current.peerHost.isNotBlank() && current.peerVoipPort > 0) {
                sendSignalDirect("CALL_END", current.peerHost, current.peerVoipPort)
            }
            stopCallAudio()
            _callState.value = current.copy(state = CallState.ENDED)
            delay(1000)
            _callState.value = VoipCallState(state = CallState.IDLE)
        }
    }

    fun toggleMute(): Boolean {
        val current = _callState.value
        val newMute = !current.isMuted
        _callState.value = current.copy(
            isMuted = newMute,
            micLevel = if (newMute) 0f else current.micLevel
        )
        return newMute
    }

    fun toggleSpeaker(): Boolean {
        val current = _callState.value
        val newSpeaker = !current.isSpeakerOn
        _callState.value = current.copy(isSpeakerOn = newSpeaker)
        try {
            audioManager?.isSpeakerphoneOn = newSpeaker
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle speaker: ${e.message}")
        }
        return newSpeaker
    }

    fun toggleLoopbackTesting(): Boolean {
        val current = _callState.value
        val newLoopback = !current.isLoopbackTesting
        _callState.value = current.copy(isLoopbackTesting = newLoopback)
        return newLoopback
    }

    // --- Audio Pipeline (AudioRecord + AudioTrack) ---
    fun startCallAudio() {
        stopCallAudio()
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = _callState.value.isSpeakerOn

            // Initialize AudioTrack for real-time PCM playback
            val minBufSizeOut = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG_OUT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufSizeOut, 2048))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            // Initialize AudioRecord for live microphone capture
            if (hasRecordPermission()) {
                val minBufSizeIn = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
                val bufferSize = maxOf(minBufSizeIn, 2048)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()

                    txJob = scope.launch {
                        val pcmBuffer = ByteArray(FRAME_SIZE_BYTES)
                        val udpPayload = ByteArray(FRAME_SIZE_BYTES + 1)
                        udpPayload[0] = PACKET_TYPE_AUDIO

                        while (isActive && _callState.value.state == CallState.CONNECTED) {
                            val bytesRead = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                            if (bytesRead > 0) {
                                val isMuted = _callState.value.isMuted
                                val isLoopback = _callState.value.isLoopbackTesting || _callState.value.peerHost == "127.0.0.1"

                                if (isMuted) {
                                    _callState.value = _callState.value.copy(micLevel = 0f)
                                } else {
                                    val micLevel = calculateRmsLevel(pcmBuffer, 0, bytesRead)
                                    _callState.value = _callState.value.copy(micLevel = micLevel)

                                    // If loopback testing is active, route local mic audio to local speaker for verification
                                    if (isLoopback) {
                                        playAudioBuffer(pcmBuffer, 0, bytesRead)
                                        _callState.value = _callState.value.copy(remoteLevel = micLevel)
                                    }
                                }

                                val current = _callState.value
                                val targetHost = current.peerHost
                                val targetPort = current.peerVoipPort
                                if (targetHost.isNotBlank() && targetPort > 0 && targetHost != "127.0.0.1") {
                                    val peerAddr = try { InetAddress.getByName(targetHost) } catch (e: Exception) { null }
                                    if (peerAddr != null) {
                                        if (isMuted) {
                                            System.arraycopy(ByteArray(bytesRead), 0, udpPayload, 1, bytesRead)
                                        } else {
                                            System.arraycopy(pcmBuffer, 0, udpPayload, 1, bytesRead)
                                        }

                                        try {
                                            val packet = DatagramPacket(udpPayload, bytesRead + 1, peerAddr, targetPort)
                                            udpSocket?.send(packet)

                                            _callState.value = _callState.value.copy(
                                                packetsSent = _callState.value.packetsSent + 1,
                                                audioBytesTransferred = _callState.value.audioBytesTransferred + bytesRead
                                            )
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Audio packet send error: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "AudioRecord could not initialize.")
                }
            } else {
                Log.w(TAG, "Microphone permission not granted yet.")
            }

            // Start ambient waveform generator for visual feedback during call
            startAmbientWaveformUpdates()

        } catch (e: SecurityException) {
            Log.w(TAG, "RECORD_AUDIO security exception: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Audio pipeline start failed: ${e.message}")
        }
    }

    private fun startAmbientWaveformUpdates() {
        simulatedWaveformJob?.cancel()
        simulatedWaveformJob = scope.launch {
            while (isActive && _callState.value.state == CallState.CONNECTED) {
                delay(120)
                // If remote audio is idle or preview mode, simulate small ambient presence flutter
                val current = _callState.value
                if (current.remoteLevel < 0.05f && current.peerHost == "127.0.0.1") {
                    val flutter = if (current.micLevel > 0.1f) current.micLevel else (0.04f + (Math.random().toFloat() * 0.12f))
                    _callState.value = current.copy(remoteLevel = flutter)
                }
            }
        }
    }

    private fun playAudioBuffer(buffer: ByteArray, offset: Int, length: Int) {
        try {
            audioTrack?.write(buffer, offset, length)
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack write error: ${e.message}")
        }
    }

    private fun calculateRmsLevel(buffer: ByteArray, offset: Int, length: Int): Float {
        if (length < 2) return 0f
        var sumSquares = 0.0
        val sampleCount = length / 2
        for (i in 0 until sampleCount) {
            val idx = offset + i * 2
            if (idx + 1 >= buffer.size) break
            val low = buffer[idx].toInt() and 0xFF
            val high = buffer[idx + 1].toInt()
            val sample = (high shl 8) or low
            sumSquares += (sample * sample).toDouble()
        }
        val rms = sqrt(sumSquares / sampleCount)
        val normalized = (rms / 7000.0).toFloat()
        return normalized.coerceIn(0f, 1f)
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0L
            while (isActive && _callState.value.state == CallState.CONNECTED) {
                delay(1000)
                seconds++
                _callState.value = _callState.value.copy(durationSeconds = seconds)
            }
        }
    }

    fun stopCallAudio() {
        txJob?.cancel()
        txJob = null
        timerJob?.cancel()
        timerJob = null
        simulatedWaveformJob?.cancel()
        simulatedWaveformJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}

        _callState.value = _callState.value.copy(
            micLevel = 0f,
            remoteLevel = 0f
        )
    }

    private suspend fun sendSignalDirect(signal: String, host: String, port: Int) {
        withContext(Dispatchers.IO) {
            try {
                val bytes = signal.toByteArray(Charsets.UTF_8)
                val packetData = ByteArray(bytes.size + 1)
                packetData[0] = PACKET_TYPE_SIGNAL
                System.arraycopy(bytes, 0, packetData, 1, bytes.size)

                val targetAddress = InetAddress.getByName(host)
                val packet = DatagramPacket(packetData, packetData.size, targetAddress, port)
                udpSocket?.send(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send signal $signal to $host:$port: ${e.message}")
            }
        }
    }

    // Testing simulation helper for solo testing in preview
    fun simulateTestIncomingCall(callerUsername: String, callerDisplayName: String) {
        _callState.value = VoipCallState(
            state = CallState.INCOMING_RINGING,
            peerUsername = callerUsername,
            peerDisplayName = callerDisplayName,
            peerHost = "127.0.0.1",
            peerVoipPort = voipPort,
            isLoopbackTesting = true
        )
    }

    fun shutdown() {
        stopCallAudio()
        rxJob?.cancel()
        rxJob = null
        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
    }
}
