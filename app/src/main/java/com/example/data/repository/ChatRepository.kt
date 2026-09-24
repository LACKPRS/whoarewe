package com.example.data.repository

import com.example.data.firebase.FirestoreService
import com.example.data.local.LocalChatSocketManager
import com.example.data.local.LocalNsdHelper
import com.example.model.ChatMessage
import com.example.model.DeliveryMode
import com.example.model.MessageStatus
import com.example.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChatRepository(
    private val firestoreService: FirestoreService,
    private val localSocketManager: LocalChatSocketManager,
    private val nsdHelper: LocalNsdHelper
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val localMessagesByChat = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    init {
        // Local socket server listener will post received messages here
    }

    fun onLocalMessageReceived(message: ChatMessage) {
        val chatId = message.chatId
        val current = localMessagesByChat.value[chatId] ?: emptyList()
        localMessagesByChat.value = localMessagesByChat.value + (chatId to (current + message))
    }

    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> {
        val cloudFlow: Flow<List<ChatMessage>> = firestoreService.getChatMessagesFlow(chatId)
        val localFlow = localMessagesByChat.asStateFlow()

        return combine(cloudFlow, localFlow) { cloudList: List<ChatMessage>, localMap: Map<String, List<ChatMessage>> ->
            val localList = localMap[chatId] ?: emptyList()
            // Merge by ID to avoid duplicates, ordered by timestamp
            val combined = (cloudList + localList)
                .distinctBy { it.id }
                .sortedBy { it.timestamp }
            combined
        }
    }

    suspend fun sendMessage(
        currentUser: User,
        recipientId: String,
        recipientUsername: String,
        text: String,
        preferLocalRouter: Boolean
    ): Result<ChatMessage> {
        val chatId = FirestoreService.getChatId(currentUser.uid, recipientId)
        val msgId = "msg_${System.currentTimeMillis()}_${(100..999).random()}"

        // Check if recipient is active on local Wi-Fi router via NSD
        val localPeer = nsdHelper.discoveredPeers.value.find {
            it.username.equals(recipientUsername, ignoreCase = true)
        }

        if (preferLocalRouter && localPeer != null) {
            // Direct P2P Local Router Transmission
            val localMsg = ChatMessage(
                id = msgId,
                chatId = chatId,
                senderId = currentUser.uid,
                senderUsername = currentUser.username,
                senderDisplayName = currentUser.displayName,
                recipientId = recipientId,
                text = text,
                timestamp = System.currentTimeMillis(),
                deliveryMode = DeliveryMode.LOCAL_ROUTER_P2P,
                status = MessageStatus.SENT
            )

            // Send via socket to peer IP
            val sendResult = localSocketManager.sendMessageToPeer(
                peerHost = localPeer.hostAddress,
                peerPort = localPeer.port,
                message = localMsg
            )

            val current = localMessagesByChat.value[chatId] ?: emptyList()
            localMessagesByChat.value = localMessagesByChat.value + (chatId to (current + localMsg))
            return Result.success(localMsg)
        } else {
            // Cloud Realtime Firestore Transmission
            val cloudMsg = ChatMessage(
                id = msgId,
                chatId = chatId,
                senderId = currentUser.uid,
                senderUsername = currentUser.username,
                senderDisplayName = currentUser.displayName,
                recipientId = recipientId,
                text = text,
                timestamp = System.currentTimeMillis(),
                deliveryMode = DeliveryMode.CLOUD_REALTIME,
                status = MessageStatus.SENT
            )
            firestoreService.sendChatMessage(cloudMsg)
            return Result.success(cloudMsg)
        }
    }
}
