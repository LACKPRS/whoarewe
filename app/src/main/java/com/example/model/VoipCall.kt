package com.example.model

data class LocalPeer(
    val serviceName: String,
    val username: String,
    val displayName: String,
    val hostAddress: String,
    val port: Int,
    val voipPort: Int,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    ENDED
}

data class VoipCallState(
    val state: CallState = CallState.IDLE,
    val peerUsername: String = "",
    val peerDisplayName: String = "",
    val peerHost: String = "",
    val peerVoipPort: Int = 0,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val errorMessage: String? = null,
    val micLevel: Float = 0f,
    val remoteLevel: Float = 0f,
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val audioBytesTransferred: Long = 0L,
    val isLoopbackTesting: Boolean = false,
    val codecDescription: String = "PCM 16-bit • 16 kHz • Low-latency UDP"
)
