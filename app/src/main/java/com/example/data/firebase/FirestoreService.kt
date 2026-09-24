package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.DeliveryMode
import com.example.model.Friend
import com.example.model.FriendRequest
import com.example.model.GroupChat
import com.example.model.GroupMessage
import com.example.model.MessageStatus
import com.example.model.Report
import com.example.model.ReportStatus
import com.example.model.RequestStatus
import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreService(private val context: Context) {
    private val TAG = "FirestoreService"

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseApp.initializeApp(context)
            if (FirebaseConfig.isFirebaseReady(context)) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore init error: ${e.message}")
            null
        }
    }

    // In-memory fallback stores for offline/preview mode (clean empty states)
    private val mockUsers = MutableStateFlow<Map<String, User>>(emptyMap())
    private val mockFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    private val mockFriends = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val mockMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val mockGroupChats = MutableStateFlow<List<GroupChat>>(emptyList())
    private val mockGroupMessages = MutableStateFlow<Map<String, List<GroupMessage>>>(emptyMap())
    private val mockReports = MutableStateFlow<List<Report>>(emptyList())

    companion object {
        fun getChatId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }

    // --- User Profile & Directory ---
    suspend fun saveUserProfile(user: User): Result<Unit> {
        val enforcedRole = User.roleForEmail(user.email)
        val cleanUser = user.copy(role = enforcedRole)
        return try {
            val firestore = db
            if (firestore != null) {
                val data = mapOf(
                    "uid" to cleanUser.uid,
                    "email" to cleanUser.email,
                    "username" to cleanUser.username,
                    "displayName" to cleanUser.displayName,
                    "statusText" to cleanUser.statusText,
                    "photoUrl" to cleanUser.photoUrl,
                    "role" to cleanUser.role.name,
                    "isOnline" to cleanUser.isOnline,
                    "lastSeen" to cleanUser.lastSeen,
                    "isBanned" to cleanUser.isBanned,
                    "warningCount" to cleanUser.warningCount,
                    "createdAt" to cleanUser.createdAt
                )
                firestore.collection("users").document(cleanUser.uid).set(data).await()
                firestore.collection("usernames").document(cleanUser.username).set(mapOf("uid" to cleanUser.uid)).await()
            }
            mockUsers.value = mockUsers.value + (cleanUser.uid to cleanUser)
            Result.success(Unit)
        } catch (e: Exception) {
            mockUsers.value = mockUsers.value + (cleanUser.uid to cleanUser)
            Result.success(Unit)
        }
    }

    suspend fun updateUserProfilePhoto(userId: String, photoUrl: String): Result<Unit> {
        return try {
            val firestore = db
            if (firestore != null) {
                firestore.collection("users").document(userId).update("photoUrl", photoUrl).await()
            }
            mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
                if (uid == userId) u.copy(photoUrl = photoUrl) else u
            }
            Result.success(Unit)
        } catch (e: Exception) {
            mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
                if (uid == userId) u.copy(photoUrl = photoUrl) else u
            }
            Result.success(Unit)
        }
    }

    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(mockUsers.value.values.toList())
                    return@addSnapshotListener
                }
                val users = snapshot.documents.mapNotNull { doc ->
                    try {
                        val email = doc.getString("email") ?: ""
                        val roleStr = doc.getString("role") ?: UserRole.STANDARD.name
                        val parsedRole = if (User.isDesignatedAdmin(email)) {
                            UserRole.ADMIN
                        } else {
                            val r = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STANDARD }
                            if (r == UserRole.ADMIN) UserRole.STANDARD else r
                        }
                        User(
                            uid = doc.getString("uid") ?: doc.id,
                            email = email,
                            username = doc.getString("username") ?: "",
                            displayName = doc.getString("displayName") ?: "",
                            statusText = doc.getString("statusText") ?: "",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            role = parsedRole,
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis(),
                            isBanned = doc.getBoolean("isBanned") ?: false,
                            warningCount = doc.getLong("warningCount")?.toInt() ?: 0,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(users)
            }
        } else {
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                mockUsers.collect { trySend(it.values.toList()) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }
        awaitClose { registration?.remove() }
    }

    suspend fun searchUserByUsername(query: String): List<User> {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return emptyList()
        val firestore = db
        if (firestore != null) {
            try {
                val snap = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", clean)
                    .whereLessThanOrEqualTo("username", clean + "\uf8ff")
                    .get().await()
                val list = snap.documents.mapNotNull { doc ->
                    val email = doc.getString("email") ?: ""
                    val roleStr = doc.getString("role") ?: UserRole.STANDARD.name
                    val parsedRole = if (User.isDesignatedAdmin(email)) {
                        UserRole.ADMIN
                    } else {
                        val r = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STANDARD }
                        if (r == UserRole.ADMIN) UserRole.STANDARD else r
                    }
                    User(
                        uid = doc.getString("uid") ?: doc.id,
                        email = email,
                        username = doc.getString("username") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        statusText = doc.getString("statusText") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        role = parsedRole,
                        isOnline = doc.getBoolean("isOnline") ?: false,
                        lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis()
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                Log.w(TAG, "Search query error: ${e.message}")
            }
        }
        return mockUsers.value.values.filter {
            it.username.contains(clean, ignoreCase = true) || it.displayName.contains(clean, ignoreCase = true)
        }
    }

    // --- Friends & Friend Requests ---
    suspend fun sendFriendRequest(fromUser: User, toUsername: String): Result<String> {
        val target = mockUsers.value.values.find { it.username.equals(toUsername, ignoreCase = true) }
            ?: return Result.failure(Exception("User @$toUsername not found"))

        if (target.uid == fromUser.uid) {
            return Result.failure(Exception("You cannot add yourself as a friend"))
        }

        val currentFriends = mockFriends.value[fromUser.uid] ?: emptySet()
        if (currentFriends.contains(target.uid)) {
            return Result.failure(Exception("Already friends with @$toUsername"))
        }

        val requestId = "req_${System.currentTimeMillis()}"
        val request = FriendRequest(
            id = requestId,
            fromUid = fromUser.uid,
            fromUsername = fromUser.username,
            fromDisplayName = fromUser.displayName,
            fromPhotoUrl = fromUser.photoUrl,
            toUid = target.uid,
            toUsername = target.username,
            timestamp = System.currentTimeMillis(),
            status = RequestStatus.PENDING
        )

        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("friend_requests").document(requestId).set(request).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore send request failed: ${e.message}")
            }
        }

        mockFriendRequests.value = mockFriendRequests.value + request
        return Result.success("Friend request sent to @$toUsername")
    }

    fun getPendingRequestsFlow(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("friend_requests")
                .whereEqualTo("toUid", userId)
                .whereEqualTo("status", RequestStatus.PENDING.name)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val reqs = snapshot.toObjects(FriendRequest::class.java)
                        trySend(reqs)
                    }
                }
        } else {
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                mockFriendRequests.collect { list ->
                    trySend(list.filter { it.toUid == userId && it.status == RequestStatus.PENDING })
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }
        awaitClose { registration?.remove() }
    }

    suspend fun respondToFriendRequest(request: FriendRequest, accept: Boolean): Result<Unit> {
        val newStatus = if (accept) RequestStatus.ACCEPTED else RequestStatus.DECLINED
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("friend_requests").document(request.id)
                    .update("status", newStatus.name).await()
                if (accept) {
                    firestore.collection("friends").document("${request.fromUid}_${request.toUid}")
                        .set(mapOf("userA" to request.fromUid, "userB" to request.toUid, "active" to true)).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Respond friend request error: ${e.message}")
            }
        }

        // Update in-memory fallback
        mockFriendRequests.value = mockFriendRequests.value.map {
            if (it.id == request.id) it.copy(status = newStatus) else it
        }

        if (accept) {
            val u1Friends = (mockFriends.value[request.fromUid] ?: emptySet()) + request.toUid
            val u2Friends = (mockFriends.value[request.toUid] ?: emptySet()) + request.fromUid
            mockFriends.value = mockFriends.value + (request.fromUid to u1Friends) + (request.toUid to u2Friends)
        }
        return Result.success(Unit)
    }

    fun getFriendsFlow(userId: String): Flow<List<Friend>> = callbackFlow {
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            mockFriends.collect { friendsMap ->
                val friendUids = friendsMap[userId] ?: emptySet()
                val allUsers = mockUsers.value
                val friendList = friendUids.mapNotNull { fUid ->
                    allUsers[fUid]?.let { u ->
                        Friend(
                            uid = u.uid,
                            username = u.username,
                            displayName = u.displayName,
                            statusText = u.statusText,
                            photoUrl = u.photoUrl,
                            role = u.role,
                            isOnline = u.isOnline,
                            lastSeen = u.lastSeen
                        )
                    }
                }
                trySend(friendList)
            }
        }
        awaitClose { job.cancel() }
    }

    // --- Direct Messaging ---
    suspend fun sendChatMessage(message: ChatMessage): Result<Unit> {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("chats")
                    .document(message.chatId)
                    .collection("messages")
                    .document(message.id)
                    .set(message)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore send msg error: ${e.message}")
            }
        }

        val existing = mockMessages.value[message.chatId] ?: emptyList()
        mockMessages.value = mockMessages.value + (message.chatId to (existing + message))
        return Result.success(Unit)
    }

    fun getChatMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(mockMessages.value[chatId] ?: emptyList())
                        return@addSnapshotListener
                    }
                    val msgs = snapshot.toObjects(ChatMessage::class.java)
                    trySend(msgs)
                }
        } else {
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                mockMessages.collect { map ->
                    trySend(map[chatId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }
        awaitClose { registration?.remove() }
    }

    // --- Group Chats ---
    suspend fun createGroupChat(group: GroupChat): Result<String> {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("groups").document(group.id).set(group).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore createGroupChat failed: ${e.message}")
            }
        }
        mockGroupChats.value = listOf(group) + mockGroupChats.value
        return Result.success(group.id)
    }

    fun getGroupChatsFlow(userId: String): Flow<List<GroupChat>> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("groups")
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(mockGroupChats.value.filter { it.memberIds.contains(userId) })
                        return@addSnapshotListener
                    }
                    val groups = snapshot.toObjects(GroupChat::class.java)
                    trySend(groups)
                }
        } else {
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                mockGroupChats.collect { list ->
                    trySend(list.filter { it.memberIds.contains(userId) })
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }
        awaitClose { registration?.remove() }
    }

    fun getGroupMessagesFlow(groupId: String): Flow<List<GroupMessage>> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("groups")
                .document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(mockGroupMessages.value[groupId] ?: emptyList())
                        return@addSnapshotListener
                    }
                    val msgs = snapshot.toObjects(GroupMessage::class.java)
                    trySend(msgs)
                }
        } else {
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                mockGroupMessages.collect { map ->
                    trySend(map[groupId] ?: emptyList())
                }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }
        awaitClose { registration?.remove() }
    }

    suspend fun sendGroupMessage(message: GroupMessage): Result<Unit> {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("groups")
                    .document(message.groupId)
                    .collection("messages")
                    .document(message.id)
                    .set(message)
                    .await()

                firestore.collection("groups")
                    .document(message.groupId)
                    .update(
                        mapOf(
                            "lastMessageText" to message.text,
                            "lastMessageSender" to message.senderUsername,
                            "lastMessageTimestamp" to message.timestamp
                        )
                    ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sendGroupMessage failed: ${e.message}")
            }
        }

        val existing = mockGroupMessages.value[message.groupId] ?: emptyList()
        mockGroupMessages.value = mockGroupMessages.value + (message.groupId to (existing + message))

        mockGroupChats.value = mockGroupChats.value.map { g ->
            if (g.id == message.groupId) {
                g.copy(
                    lastMessageText = message.text,
                    lastMessageSender = message.senderUsername,
                    lastMessageTimestamp = message.timestamp
                )
            } else g
        }
        return Result.success(Unit)
    }

    suspend fun addMemberToGroup(groupId: String, newMemberId: String, newMemberUsername: String): Result<Unit> {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("groups").document(groupId).update(
                    "memberIds", FieldValue.arrayUnion(newMemberId),
                    "memberUsernames", FieldValue.arrayUnion(newMemberUsername)
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore addMember error: ${e.message}")
            }
        }

        mockGroupChats.value = mockGroupChats.value.map { g ->
            if (g.id == groupId) {
                val updatedIds = (g.memberIds + newMemberId).distinct()
                val updatedUsernames = (g.memberUsernames + newMemberUsername).distinct()
                g.copy(memberIds = updatedIds, memberUsernames = updatedUsernames)
            } else g
        }
        return Result.success(Unit)
    }

    // --- Moderation & Reporting ---
    suspend fun submitReport(report: Report): Result<Unit> {
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("reports").document(report.id).set(report).await()
            } catch (e: Exception) {
                Log.w(TAG, "Report submit error: ${e.message}")
            }
        }
        mockReports.value = listOf(report) + mockReports.value
        return Result.success(Unit)
    }

    fun getReportsFlow(): Flow<List<Report>> = callbackFlow {
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            mockReports.collect { trySend(it) }
        }
        awaitClose { job.cancel() }
    }

    suspend fun resolveReport(reportId: String, status: ReportStatus, note: String): Result<Unit> {
        mockReports.value = mockReports.value.map {
            if (it.id == reportId) it.copy(status = status, resolutionNote = note) else it
        }
        return Result.success(Unit)
    }

    suspend fun updateUserRole(targetUid: String, newRole: UserRole): Result<Unit> {
        val target = mockUsers.value[targetUid]
        if (target != null && User.isDesignatedAdmin(target.email)) {
            // peterparkerm4178@gmail.com is permanently admin
            return Result.success(Unit)
        }
        val allowedRole = if (newRole == UserRole.ADMIN) UserRole.MODERATOR else newRole
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("users").document(targetUid).update("role", allowedRole.name).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore updateUserRole error: ${e.message}")
            }
        }
        mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
            if (uid == targetUid) u.copy(role = allowedRole) else u
        }
        return Result.success(Unit)
    }

    suspend fun warnUser(targetUid: String): Result<Unit> {
        val target = mockUsers.value[targetUid]
        if (target != null && User.isDesignatedAdmin(target.email)) {
            return Result.success(Unit)
        }
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("users").document(targetUid).update("warningCount", FieldValue.increment(1)).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore warnUser error: ${e.message}")
            }
        }
        mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
            if (uid == targetUid) u.copy(warningCount = u.warningCount + 1) else u
        }
        return Result.success(Unit)
    }

    suspend fun toggleUserBan(targetUid: String): Result<Boolean> {
        val target = mockUsers.value[targetUid]
        if (target != null && User.isDesignatedAdmin(target.email)) {
            // Cannot ban designated admin
            return Result.success(false)
        }
        var isNowBanned = false
        val firestore = db
        mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
            if (uid == targetUid) {
                isNowBanned = !u.isBanned
                if (firestore != null) {
                    try {
                        firestore.collection("users").document(targetUid).update("isBanned", isNowBanned)
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore toggleUserBan error: ${e.message}")
                    }
                }
                u.copy(isBanned = isNowBanned)
            } else u
        }
        return Result.success(isNowBanned)
    }
}
