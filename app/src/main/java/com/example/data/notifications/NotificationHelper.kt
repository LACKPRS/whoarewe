package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_DIRECT_MESSAGES = "channel_direct_messages"
    const val CHANNEL_GROUP_MESSAGES = "channel_group_messages"
    const val CHANNEL_FRIEND_UPDATES = "channel_friend_updates"

    const val EXTRA_SCREEN = "extra_screen"
    const val EXTRA_PEER_ID = "extra_peer_id"
    const val EXTRA_PEER_USERNAME = "extra_peer_username"
    const val EXTRA_GROUP_ID = "extra_group_id"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val directChannel = NotificationChannel(
                CHANNEL_DIRECT_MESSAGES,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming 1-on-1 chat messages"
                enableVibration(true)
            }

            val groupChannel = NotificationChannel(
                CHANNEL_GROUP_MESSAGES,
                "Group Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming group chat messages"
                enableVibration(true)
            }

            val friendChannel = NotificationChannel(
                CHANNEL_FRIEND_UPDATES,
                "Friend Requests & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friend requests and accepted connections"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(directChannel, groupChannel, friendChannel))
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showDirectMessageNotification(
        context: Context,
        senderId: String,
        senderUsername: String,
        senderDisplayName: String,
        messageText: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, "CHAT")
            putExtra(EXTRA_PEER_ID, senderId)
            putExtra(EXTRA_PEER_USERNAME, senderUsername)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderDisplayName.ifBlank { "@$senderUsername" })
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(senderId.hashCode(), notification)
    }

    fun showGroupMessageNotification(
        context: Context,
        groupId: String,
        groupName: String,
        senderUsername: String,
        messageText: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, "GROUP_CHAT")
            putExtra(EXTRA_GROUP_ID, groupId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            groupId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GROUP_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(groupName)
            .setContentText("@$senderUsername: $messageText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("@$senderUsername: $messageText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(groupId.hashCode(), notification)
    }

    fun showFriendRequestNotification(
        context: Context,
        fromUsername: String,
        fromDisplayName: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, "FRIENDS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            fromUsername.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Friend Request")
            .setContentText("$fromDisplayName (@$fromUsername) sent you a friend request.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(fromUsername.hashCode() + 1000, notification)
    }

    fun showFriendAcceptedNotification(
        context: Context,
        fromUsername: String,
        fromDisplayName: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, "FRIENDS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            fromUsername.hashCode() + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Friend Request Accepted")
            .setContentText("$fromDisplayName (@$fromUsername) accepted your friend request!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(fromUsername.hashCode() + 2000, notification)
    }
}
