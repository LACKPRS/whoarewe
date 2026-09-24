package com.example.model

enum class DeliveryMode {
    CLOUD_REALTIME,
    LOCAL_ROUTER_P2P
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val recipientId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryMode: DeliveryMode = DeliveryMode.CLOUD_REALTIME,
    val status: MessageStatus = MessageStatus.SENT
)
