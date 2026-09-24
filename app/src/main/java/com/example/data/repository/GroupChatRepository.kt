package com.example.data.repository

import com.example.data.firebase.FirestoreService
import com.example.model.Friend
import com.example.model.GroupChat
import com.example.model.GroupMessage
import com.example.model.User
import kotlinx.coroutines.flow.Flow

class GroupChatRepository(
    private val firestoreService: FirestoreService
) {
    fun getGroupChats(userId: String): Flow<List<GroupChat>> {
        return firestoreService.getGroupChatsFlow(userId)
    }

    fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> {
        return firestoreService.getGroupMessagesFlow(groupId)
    }

    suspend fun createGroupChat(
        creator: User,
        name: String,
        description: String,
        selectedFriends: List<Friend>
    ): Result<String> {
        val memberIds = (listOf(creator.uid) + selectedFriends.map { it.uid }).distinct()
        val memberUsernames = (listOf(creator.username) + selectedFriends.map { it.username }).distinct()
        val groupId = "group_${System.currentTimeMillis()}"

        val group = GroupChat(
            id = groupId,
            name = name.trim(),
            description = description.trim(),
            creatorId = creator.uid,
            creatorUsername = creator.username,
            memberIds = memberIds,
            memberUsernames = memberUsernames,
            iconUrl = "",
            lastMessageText = "Group created by @${creator.username}",
            lastMessageSender = creator.username,
            lastMessageTimestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        return firestoreService.createGroupChat(group)
    }

    suspend fun sendGroupMessage(
        group: GroupChat,
        sender: User,
        text: String
    ): Result<Unit> {
        val message = GroupMessage(
            id = "gmsg_${System.currentTimeMillis()}_${sender.uid.take(4)}",
            groupId = group.id,
            senderId = sender.uid,
            senderUsername = sender.username,
            senderDisplayName = sender.displayName,
            senderPhotoUrl = sender.photoUrl,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        return firestoreService.sendGroupMessage(message)
    }
}
