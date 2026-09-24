package com.example.model

data class GroupChat(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorUsername: String = "",
    val memberIds: List<String> = emptyList(),
    val memberUsernames: List<String> = emptyList(),
    val iconUrl: String = "",
    val lastMessageText: String = "",
    val lastMessageSender: String = "",
    val lastMessageTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
