package com.example.data.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TextFlowMessagingService : FirebaseMessagingService() {
    private val TAG = "TextFlowMessaging"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token registered: $token")
        // Can be stored in user Firestore profile if authenticated
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val type = data["type"] ?: "DIRECT_MESSAGE"
        val senderId = data["senderId"] ?: ""
        val senderUsername = data["senderUsername"] ?: (remoteMessage.notification?.title ?: "User")
        val senderDisplayName = data["senderDisplayName"] ?: senderUsername
        val messageText = data["text"] ?: (remoteMessage.notification?.body ?: "")
        val groupId = data["groupId"] ?: ""
        val groupName = data["groupName"] ?: "Group Chat"

        when (type) {
            "DIRECT_MESSAGE" -> {
                NotificationHelper.showDirectMessageNotification(
                    context = applicationContext,
                    senderId = senderId,
                    senderUsername = senderUsername,
                    senderDisplayName = senderDisplayName,
                    messageText = messageText
                )
            }
            "GROUP_MESSAGE" -> {
                NotificationHelper.showGroupMessageNotification(
                    context = applicationContext,
                    groupId = groupId,
                    groupName = groupName,
                    senderUsername = senderUsername,
                    messageText = messageText
                )
            }
            "FRIEND_REQUEST" -> {
                NotificationHelper.showFriendRequestNotification(
                    context = applicationContext,
                    fromUsername = senderUsername,
                    fromDisplayName = senderDisplayName
                )
            }
            "FRIEND_ACCEPTED" -> {
                NotificationHelper.showFriendAcceptedNotification(
                    context = applicationContext,
                    fromUsername = senderUsername,
                    fromDisplayName = senderDisplayName
                )
            }
            else -> {
                remoteMessage.notification?.let {
                    NotificationHelper.showDirectMessageNotification(
                        context = applicationContext,
                        senderId = senderId,
                        senderUsername = it.title ?: "TextFlow",
                        senderDisplayName = it.title ?: "TextFlow",
                        messageText = it.body ?: ""
                    )
                }
            }
        }
    }
}
