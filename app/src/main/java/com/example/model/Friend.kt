package com.example.model

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromDisplayName: String = "",
    val fromPhotoUrl: String = "",
    val toUid: String = "",
    val toUsername: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING
)

data class Friend(
    val uid: String,
    val username: String,
    val displayName: String,
    val statusText: String,
    val role: UserRole,
    val isOnline: Boolean,
    val photoUrl: String = "",
    val isLocalWifiPeer: Boolean = false,
    val localIp: String? = null,
    val localPort: Int? = null,
    val localVoipPort: Int? = null,
    val lastSeen: Long = System.currentTimeMillis()
)
