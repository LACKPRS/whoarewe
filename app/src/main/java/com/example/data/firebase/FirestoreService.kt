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
    private val mockUsers = MutableStateFlow<Map<String, User>>(
        listOf(
            User(
                uid = "user_alex",
                email = "alex@kaiser.internal",
                username = "alex",
                displayName = "Alex Rivera",
                statusText = "Building next-gen mobile apps 🚀",
                photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                role = UserRole.STANDARD,
                isOnline = true,
                bitmojiHair = "fade",
                bitmojiOutfit = "snap_jacket",
                snapScore = 1420,
                snapStreaks = 18
            ),
            User(
                uid = "user_sam",
                email = "sam@kaiser.internal",
                username = "sam",
                displayName = "Sam Taylor",
                statusText = "Coffee, music & chats ☕🎧",
                photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                role = UserRole.STANDARD,
                isOnline = true,
                bitmojiHair = "curly",
                bitmojiOutfit = "casual_tee",
                snapScore = 890,
                snapStreaks = 7
            ),
            User(
                uid = "user_jordan",
                email = "jordan@kaiser.internal",
                username = "jordan",
                displayName = "Jordan Lee",
                statusText = "Catch me on Kaiser! 💬",
                photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                role = UserRole.STANDARD,
                isOnline = true,
                bitmojiHair = "bob",
                bitmojiOutfit = "sporty",
                snapScore = 2100,
                snapStreaks = 32
            ),
            User(
                uid = "user_elena",
                email = "elena@kaiser.internal",
                username = "elena",
                displayName = "Elena Vance",
                statusText = "Living in the moment ✨",
                photoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                role = UserRole.STANDARD,
                isOnline = false,
                bitmojiHair = "ponytail",
                bitmojiOutfit = "hoodie",
                snapScore = 530,
                snapStreaks = 3
            )
        ).associateBy { it.uid }
    )
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
                    "createdAt" to cleanUser.createdAt,
                    "snapScore" to cleanUser.snapScore,
                    "snapStreaks" to cleanUser.snapStreaks,
                    "zodiacSign" to cleanUser.zodiacSign,
                    "bitmojiSkin" to cleanUser.bitmojiSkin,
                    "bitmojiHair" to cleanUser.bitmojiHair,
                    "bitmojiHairColor" to cleanUser.bitmojiHairColor,
                    "bitmojiOutfit" to cleanUser.bitmojiOutfit,
                    "bitmojiOutfitColor" to cleanUser.bitmojiOutfitColor,
                    "bitmojiMood" to cleanUser.bitmojiMood,
                    "bitmojiAccessory" to cleanUser.bitmojiAccessory,
                    "bitmojiBackground" to cleanUser.bitmojiBackground,
                    "bitmojiPose" to cleanUser.bitmojiPose,
                    "hasCustomBitmoji" to cleanUser.hasCustomBitmoji
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

    suspend fun updateUserSnapProfile(
        userId: String,
        displayName: String,
        statusText: String,
        zodiacSign: String,
        bitmojiSkin: String,
        bitmojiHair: String,
        bitmojiHairColor: String,
        bitmojiOutfit: String,
        bitmojiOutfitColor: String,
        bitmojiMood: String,
        bitmojiAccessory: String,
        bitmojiBackground: String,
        bitmojiPose: String
    ): Result<Unit> {
        val updates = mapOf(
            "displayName" to displayName,
            "statusText" to statusText,
            "zodiacSign" to zodiacSign,
            "bitmojiSkin" to bitmojiSkin,
            "bitmojiHair" to bitmojiHair,
            "bitmojiHairColor" to bitmojiHairColor,
            "bitmojiOutfit" to bitmojiOutfit,
            "bitmojiOutfitColor" to bitmojiOutfitColor,
            "bitmojiMood" to bitmojiMood,
            "bitmojiAccessory" to bitmojiAccessory,
            "bitmojiBackground" to bitmojiBackground,
            "bitmojiPose" to bitmojiPose,
            "hasCustomBitmoji" to true
        )
        return try {
            val firestore = db
            if (firestore != null) {
                firestore.collection("users").document(userId).update(updates).await()
            }
            mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
                if (uid == userId) {
                    u.copy(
                        displayName = displayName.ifBlank { u.displayName },
                        statusText = statusText.ifBlank { u.statusText },
                        zodiacSign = zodiacSign,
                        bitmojiSkin = bitmojiSkin,
                        bitmojiHair = bitmojiHair,
                        bitmojiHairColor = bitmojiHairColor,
                        bitmojiOutfit = bitmojiOutfit,
                        bitmojiOutfitColor = bitmojiOutfitColor,
                        bitmojiMood = bitmojiMood,
                        bitmojiAccessory = bitmojiAccessory,
                        bitmojiBackground = bitmojiBackground,
                        bitmojiPose = bitmojiPose,
                        hasCustomBitmoji = true
                    )
                } else u
            }
            Result.success(Unit)
        } catch (e: Exception) {
            mockUsers.value = mockUsers.value.mapValues { (uid, u) ->
                if (uid == userId) {
                    u.copy(
                        displayName = displayName.ifBlank { u.displayName },
                        statusText = statusText.ifBlank { u.statusText },
                        zodiacSign = zodiacSign,
                        bitmojiSkin = bitmojiSkin,
                        bitmojiHair = bitmojiHair,
                        bitmojiHairColor = bitmojiHairColor,
                        bitmojiOutfit = bitmojiOutfit,
                        bitmojiOutfitColor = bitmojiOutfitColor,
                        bitmojiMood = bitmojiMood,
                        bitmojiAccessory = bitmojiAccessory,
                        bitmojiBackground = bitmojiBackground,
                        bitmojiPose = bitmojiPose,
                        hasCustomBitmoji = true
                    )
                } else u
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
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            snapScore = doc.getLong("snapScore")?.toInt() ?: 1420,
                            snapStreaks = doc.getLong("snapStreaks")?.toInt() ?: 7,
                            zodiacSign = doc.getString("zodiacSign") ?: "Aries ♈",
                            bitmojiSkin = doc.getString("bitmojiSkin") ?: "light",
                            bitmojiHair = doc.getString("bitmojiHair") ?: "fade",
                            bitmojiHairColor = doc.getString("bitmojiHairColor") ?: "black",
                            bitmojiOutfit = doc.getString("bitmojiOutfit") ?: "snap_hoodie",
                            bitmojiOutfitColor = doc.getString("bitmojiOutfitColor") ?: "yellow",
                            bitmojiMood = doc.getString("bitmojiMood") ?: "smile",
                            bitmojiAccessory = doc.getString("bitmojiAccessory") ?: "none",
                            bitmojiBackground = doc.getString("bitmojiBackground") ?: "sunset",
                            bitmojiPose = doc.getString("bitmojiPose") ?: "peace",
                            hasCustomBitmoji = doc.getBoolean("hasCustomBitmoji") ?: true
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
        val clean = query.trim().lowercase().removePrefix("@")
        val firestore = db
        if (firestore != null) {
            try {
                val q = if (clean.isBlank()) {
                    firestore.collection("users").limit(10)
                } else {
                    firestore.collection("users")
                        .whereGreaterThanOrEqualTo("username", clean)
                        .whereLessThanOrEqualTo("username", clean + "\uf8ff")
                        .limit(20)
                }
                val snap = q.get().await()
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
        return if (clean.isBlank()) {
            mockUsers.value.values.toList()
        } else {
            mockUsers.value.values.filter {
                it.username.contains(clean, ignoreCase = true) || it.displayName.contains(clean, ignoreCase = true)
            }
        }
    }

    // --- Friends & Friend Requests ---
    suspend fun sendFriendRequest(fromUser: User, toUsername: String): Result<String> {
        val cleanToUsername = toUsername.trim().lowercase().removePrefix("@")
        var target = mockUsers.value.values.find { it.username.equals(cleanToUsername, ignoreCase = true) }
        
        val firestore = db
        if (target == null && firestore != null) {
            try {
                val querySnap = firestore.collection("users")
                    .whereEqualTo("username", cleanToUsername)
                    .limit(1)
                    .get()
                    .await()
                val doc = querySnap.documents.firstOrNull()
                if (doc != null) {
                    val email = doc.getString("email") ?: ""
                    val roleStr = doc.getString("role") ?: UserRole.STANDARD.name
                    val parsedRole = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STANDARD }
                    target = User(
                        uid = doc.getString("uid") ?: doc.id,
                        email = email,
                        username = doc.getString("username") ?: cleanToUsername,
                        displayName = doc.getString("displayName") ?: cleanToUsername,
                        statusText = doc.getString("statusText") ?: "Active on TextFlow",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        role = parsedRole,
                        isOnline = doc.getBoolean("isOnline") ?: false,
                        lastSeen = doc.getLong("lastSeen") ?: System.currentTimeMillis()
                    )
                    mockUsers.value = mockUsers.value + (target.uid to target)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query user by username from Firestore: ${e.message}")
            }
        }

        if (target == null) {
            return Result.failure(Exception("User @$cleanToUsername not found"))
        }

        if (target.uid == fromUser.uid) {
            return Result.failure(Exception("You cannot add yourself as a friend"))
        }

        val currentFriends = mockFriends.value[fromUser.uid] ?: emptySet()
        if (currentFriends.contains(target.uid)) {
            return Result.failure(Exception("Already friends with @$cleanToUsername"))
        }

        val requestId = "req_${System.currentTimeMillis()}_${(100..999).random()}"
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

        if (firestore != null) {
            try {
                val reqMap = mapOf(
                    "id" to request.id,
                    "fromUid" to request.fromUid,
                    "fromUsername" to request.fromUsername,
                    "fromDisplayName" to request.fromDisplayName,
                    "fromPhotoUrl" to request.fromPhotoUrl,
                    "toUid" to request.toUid,
                    "toUsername" to request.toUsername,
                    "timestamp" to request.timestamp,
                    "status" to request.status.name
                )
                firestore.collection("friend_requests").document(requestId).set(reqMap).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore send request failed: ${e.message}")
            }
        }

        mockFriendRequests.value = mockFriendRequests.value + request
        return Result.success("Friend request sent to @$cleanToUsername")
    }

    fun getPendingRequestsFlow(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        if (mockFriendRequests.value.none { it.toUid == userId }) {
            val seedReq = FriendRequest(
                id = "req_seed_alex_${userId}",
                fromUid = "user_alex",
                fromUsername = "alex",
                fromDisplayName = "Alex Rivera",
                fromPhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                toUid = userId,
                toUsername = "",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 30,
                status = RequestStatus.PENDING
            )
            mockFriendRequests.value = mockFriendRequests.value + seedReq
        }
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("friend_requests")
                .whereEqualTo("toUid", userId)
                .whereEqualTo("status", RequestStatus.PENDING.name)
                .addSnapshotListener { snapshot, error ->
                    val memReqs = mockFriendRequests.value.filter { it.toUid == userId && it.status == RequestStatus.PENDING }
                    if (error != null || snapshot == null) {
                        trySend(memReqs)
                        return@addSnapshotListener
                    }
                    val cloudReqs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val fUid = doc.getString("fromUid") ?: ""
                            val fUser = doc.getString("fromUsername") ?: ""
                            val fName = doc.getString("fromDisplayName") ?: fUser
                            val fPhoto = doc.getString("fromPhotoUrl") ?: ""
                            val tUid = doc.getString("toUid") ?: ""
                            val tUser = doc.getString("toUsername") ?: ""
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val stStr = doc.getString("status") ?: RequestStatus.PENDING.name
                            val st = try { RequestStatus.valueOf(stStr) } catch (_: Exception) { RequestStatus.PENDING }
                            FriendRequest(id, fUid, fUser, fName, fPhoto, tUid, tUser, ts, st)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val merged = (cloudReqs + memReqs).distinctBy { it.id }.sortedByDescending { it.timestamp }
                    trySend(merged)
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
                    val userA = request.fromUid
                    val userB = request.toUid

                    val friendDataForA = mapOf(
                        "uid" to userB,
                        "username" to request.toUsername,
                        "displayName" to (mockUsers.value[userB]?.displayName ?: request.toUsername),
                        "photoUrl" to (mockUsers.value[userB]?.photoUrl ?: ""),
                        "statusText" to (mockUsers.value[userB]?.statusText ?: "Active on TextFlow"),
                        "role" to (mockUsers.value[userB]?.role?.name ?: UserRole.STANDARD.name),
                        "addedAt" to System.currentTimeMillis()
                    )
                    val friendDataForB = mapOf(
                        "uid" to userA,
                        "username" to request.fromUsername,
                        "displayName" to request.fromDisplayName,
                        "photoUrl" to request.fromPhotoUrl,
                        "statusText" to (mockUsers.value[userA]?.statusText ?: "Active on TextFlow"),
                        "role" to UserRole.STANDARD.name,
                        "addedAt" to System.currentTimeMillis()
                    )

                    firestore.collection("users").document(userA).collection("friends").document(userB).set(friendDataForA).await()
                    firestore.collection("users").document(userB).collection("friends").document(userA).set(friendDataForB).await()
                    firestore.collection("friends").document("${userA}_${userB}")
                        .set(mapOf("userA" to userA, "userB" to userB, "active" to true)).await()
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
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("users")
                .document(userId)
                .collection("friends")
                .addSnapshotListener { snapshot, error ->
                    val memFriends = run {
                        val friendUids = mockFriends.value[userId] ?: emptySet()
                        val allUsers = mockUsers.value
                        friendUids.mapNotNull { fUid ->
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
                    }

                    if (error != null || snapshot == null) {
                        trySend(memFriends)
                        return@addSnapshotListener
                    }

                    val cloudFriends = snapshot.documents.mapNotNull { doc ->
                        try {
                            val uid = doc.getString("uid") ?: doc.id
                            val username = doc.getString("username") ?: ""
                            val displayName = doc.getString("displayName") ?: username
                            val statusText = doc.getString("statusText") ?: "Active on TextFlow"
                            val photoUrl = doc.getString("photoUrl") ?: ""
                            val roleStr = doc.getString("role") ?: UserRole.STANDARD.name
                            val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.STANDARD }
                            Friend(
                                uid = uid,
                                username = username,
                                displayName = displayName,
                                statusText = statusText,
                                role = role,
                                isOnline = true,
                                photoUrl = photoUrl
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val merged = (cloudFriends + memFriends).distinctBy { it.uid }
                    trySend(merged)
                }
        } else {
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
            return@callbackFlow
        }
        awaitClose { registration?.remove() }
    }

    // --- Direct Messaging ---
    suspend fun sendChatMessage(message: ChatMessage): Result<Unit> {
        val firestore = db
        if (firestore != null) {
            try {
                val data = mapOf(
                    "id" to message.id,
                    "chatId" to message.chatId,
                    "senderId" to message.senderId,
                    "senderUsername" to message.senderUsername,
                    "senderDisplayName" to message.senderDisplayName,
                    "recipientId" to message.recipientId,
                    "text" to message.text,
                    "timestamp" to message.timestamp,
                    "deliveryMode" to message.deliveryMode.name,
                    "status" to message.status.name,
                    "upvotedBy" to message.upvotedBy,
                    "downvotedBy" to message.downvotedBy
                )
                firestore.collection("chats")
                    .document(message.chatId)
                    .collection("messages")
                    .document(message.id)
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore send msg error: ${e.message}")
            }
        }

        val existing = mockMessages.value[message.chatId] ?: emptyList()
        mockMessages.value = mockMessages.value + (message.chatId to (existing + message))

        // Community friends simulated reply for interactive Facebook-style conversation
        if (message.recipientId == "user_alex" || message.recipientId == "user_sam" || message.recipientId == "user_jordan" || message.recipientId == "user_elena") {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                kotlinx.coroutines.delay(1200)
                val peerUser = mockUsers.value[message.recipientId]
                val replyText = when {
                    message.text.contains("hi", ignoreCase = true) || message.text.contains("hello", ignoreCase = true) || message.text.contains("hey", ignoreCase = true) ->
                        "Hey there! Great to connect with you on Kaiser! How's your day going? 😊"
                    message.text.contains("how are you", ignoreCase = true) ->
                        "I'm doing great, thanks for asking! Loving the new friend & group features here on Kaiser 🚀"
                    message.text.contains("photo", ignoreCase = true) || message.text.contains("snap", ignoreCase = true) || message.text.contains("kicon", ignoreCase = true) ->
                        "Your Kicon avatar and card look super clean! ✨"
                    else ->
                        "Got your message! So awesome we can talk freely here just like Facebook friends! 🔥"
                }
                val replyMsg = ChatMessage(
                    id = "reply_${System.currentTimeMillis()}_${(100..999).random()}",
                    chatId = message.chatId,
                    senderId = message.recipientId,
                    senderUsername = peerUser?.username ?: "friend",
                    senderDisplayName = peerUser?.displayName ?: "Friend",
                    recipientId = message.senderId,
                    text = replyText,
                    timestamp = System.currentTimeMillis(),
                    deliveryMode = DeliveryMode.CLOUD_REALTIME,
                    status = MessageStatus.SENT
                )
                val current = mockMessages.value[message.chatId] ?: emptyList()
                mockMessages.value = mockMessages.value + (message.chatId to (current + replyMsg))
            }
        }

        return Result.success(Unit)
    }

    suspend fun voteChatMessage(chatId: String, messageId: String, userId: String, isUpvote: Boolean): Result<Unit> {
        val currentMsgs = mockMessages.value[chatId] ?: emptyList()
        val targetMsg = currentMsgs.find { it.id == messageId }

        var newUpvoted = targetMsg?.upvotedBy ?: emptyList()
        var newDownvoted = targetMsg?.downvotedBy ?: emptyList()

        if (isUpvote) {
            if (newUpvoted.contains(userId)) {
                newUpvoted = newUpvoted - userId
            } else {
                newUpvoted = newUpvoted + userId
                newDownvoted = newDownvoted - userId
            }
        } else {
            if (newDownvoted.contains(userId)) {
                newDownvoted = newDownvoted - userId
            } else {
                newDownvoted = newDownvoted + userId
                newUpvoted = newUpvoted - userId
            }
        }

        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .update(mapOf("upvotedBy" to newUpvoted, "downvotedBy" to newDownvoted))
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore voteChatMessage error: ${e.message}")
            }
        }

        mockMessages.value = mockMessages.value.mapValues { (cId, list) ->
            if (cId == chatId) {
                list.map { m ->
                    if (m.id == messageId) m.copy(upvotedBy = newUpvoted, downvotedBy = newDownvoted) else m
                }
            } else list
        }
        return Result.success(Unit)
    }

    suspend fun clearChatMessages(chatId: String): Result<Unit> {
        val firestore = db
        if (firestore != null) {
            try {
                val snapshot = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .get()
                    .await()
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing Firestore messages: ${e.message}")
            }
        }
        mockMessages.value = mockMessages.value - chatId
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
                    val memMsgs = mockMessages.value[chatId] ?: emptyList()
                    if (error != null || snapshot == null) {
                        trySend(memMsgs)
                        return@addSnapshotListener
                    }
                    val cloudMsgs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val sId = doc.getString("senderId") ?: ""
                            val sUser = doc.getString("senderUsername") ?: ""
                            val sName = doc.getString("senderDisplayName") ?: sUser
                            val rId = doc.getString("recipientId") ?: ""
                            val text = doc.getString("text") ?: ""
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val dModeStr = doc.getString("deliveryMode") ?: DeliveryMode.CLOUD_REALTIME.name
                            val dMode = try { DeliveryMode.valueOf(dModeStr) } catch (_: Exception) { DeliveryMode.CLOUD_REALTIME }
                            val stStr = doc.getString("status") ?: MessageStatus.SENT.name
                            val st = try { MessageStatus.valueOf(stStr) } catch (_: Exception) { MessageStatus.SENT }
                            val upvoted = (doc.get("upvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val downvoted = (doc.get("downvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            ChatMessage(id, chatId, sId, sUser, sName, rId, text, ts, dMode, st, upvoted, downvoted)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val merged = (cloudMsgs + memMsgs).distinctBy { it.id }.sortedBy { it.timestamp }
                    trySend(merged)
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
                val groupMap = mapOf(
                    "id" to group.id,
                    "name" to group.name,
                    "description" to group.description,
                    "creatorId" to group.creatorId,
                    "creatorUsername" to group.creatorUsername,
                    "memberIds" to group.memberIds,
                    "memberUsernames" to group.memberUsernames,
                    "iconUrl" to group.iconUrl,
                    "lastMessageText" to group.lastMessageText,
                    "lastMessageSender" to group.lastMessageSender,
                    "lastMessageTimestamp" to group.lastMessageTimestamp,
                    "createdAt" to group.createdAt
                )
                firestore.collection("groups").document(group.id).set(groupMap).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore createGroupChat failed: ${e.message}")
            }
        }
        mockGroupChats.value = listOf(group) + mockGroupChats.value
        return Result.success(group.id)
    }

    suspend fun addMembersToGroup(groupId: String, newMembers: List<Friend>): Result<Unit> {
        val newUids = newMembers.map { it.uid }
        val newNames = newMembers.map { it.username }
        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("groups").document(groupId)
                    .update(
                        "memberIds", FieldValue.arrayUnion(*newUids.toTypedArray()),
                        "memberUsernames", FieldValue.arrayUnion(*newNames.toTypedArray())
                    ).await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore addMembersToGroup failed: ${e.message}")
            }
        }
        mockGroupChats.value = mockGroupChats.value.map { g ->
            if (g.id == groupId) {
                val updatedIds = (g.memberIds + newUids).distinct()
                val updatedNames = (g.memberUsernames + newNames).distinct()
                g.copy(memberIds = updatedIds, memberUsernames = updatedNames)
            } else g
        }
        return Result.success(Unit)
    }

    fun getGroupChatsFlow(userId: String): Flow<List<GroupChat>> = callbackFlow {
        val firestore = db
        var registration: ListenerRegistration? = null
        if (firestore != null) {
            registration = firestore.collection("groups")
                .whereArrayContains("memberIds", userId)
                .addSnapshotListener { snapshot, error ->
                    val memGroups = mockGroupChats.value.filter { it.memberIds.contains(userId) }
                    if (error != null || snapshot == null) {
                        trySend(memGroups)
                        return@addSnapshotListener
                    }
                    val cloudGroups = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val name = doc.getString("name") ?: ""
                            val desc = doc.getString("description") ?: ""
                            val cId = doc.getString("creatorId") ?: ""
                            val cUser = doc.getString("creatorUsername") ?: ""
                            @Suppress("UNCHECKED_CAST")
                            val mIds = (doc.get("memberIds") as? List<String>) ?: emptyList()
                            @Suppress("UNCHECKED_CAST")
                            val mUsers = (doc.get("memberUsernames") as? List<String>) ?: emptyList()
                            val icon = doc.getString("iconUrl") ?: ""
                            val lastMsg = doc.getString("lastMessageText") ?: ""
                            val lastSender = doc.getString("lastMessageSender") ?: ""
                            val lastTs = doc.getLong("lastMessageTimestamp") ?: 0L
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            GroupChat(id, name, desc, cId, cUser, mIds, mUsers, icon, lastMsg, lastSender, lastTs, createdAt)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val merged = (cloudGroups + memGroups).distinctBy { it.id }.sortedByDescending { it.lastMessageTimestamp }
                    trySend(merged)
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
                    val memMsgs = mockGroupMessages.value[groupId] ?: emptyList()
                    if (error != null || snapshot == null) {
                        trySend(memMsgs)
                        return@addSnapshotListener
                    }
                    val cloudMsgs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val gId = doc.getString("groupId") ?: groupId
                            val sId = doc.getString("senderId") ?: ""
                            val sUser = doc.getString("senderUsername") ?: ""
                            val sName = doc.getString("senderDisplayName") ?: sUser
                            val sPhoto = doc.getString("senderPhotoUrl") ?: ""
                            val text = doc.getString("text") ?: ""
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val upvoted = (doc.get("upvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            val downvoted = (doc.get("downvotedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            GroupMessage(id, gId, sId, sUser, sName, sPhoto, text, ts, upvoted, downvoted)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val merged = (cloudMsgs + memMsgs).distinctBy { it.id }.sortedBy { it.timestamp }
                    trySend(merged)
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
                val data = mapOf(
                    "id" to message.id,
                    "groupId" to message.groupId,
                    "senderId" to message.senderId,
                    "senderUsername" to message.senderUsername,
                    "senderDisplayName" to message.senderDisplayName,
                    "senderPhotoUrl" to message.senderPhotoUrl,
                    "text" to message.text,
                    "timestamp" to message.timestamp,
                    "upvotedBy" to message.upvotedBy,
                    "downvotedBy" to message.downvotedBy
                )
                firestore.collection("groups")
                    .document(message.groupId)
                    .collection("messages")
                    .document(message.id)
                    .set(data)
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

    suspend fun voteGroupMessage(groupId: String, messageId: String, userId: String, isUpvote: Boolean): Result<Unit> {
        val currentMsgs = mockGroupMessages.value[groupId] ?: emptyList()
        val targetMsg = currentMsgs.find { it.id == messageId }

        var newUpvoted = targetMsg?.upvotedBy ?: emptyList()
        var newDownvoted = targetMsg?.downvotedBy ?: emptyList()

        if (isUpvote) {
            if (newUpvoted.contains(userId)) {
                newUpvoted = newUpvoted - userId
            } else {
                newUpvoted = newUpvoted + userId
                newDownvoted = newDownvoted - userId
            }
        } else {
            if (newDownvoted.contains(userId)) {
                newDownvoted = newDownvoted - userId
            } else {
                newDownvoted = newDownvoted + userId
                newUpvoted = newUpvoted - userId
            }
        }

        val firestore = db
        if (firestore != null) {
            try {
                firestore.collection("groups")
                    .document(groupId)
                    .collection("messages")
                    .document(messageId)
                    .update(mapOf("upvotedBy" to newUpvoted, "downvotedBy" to newDownvoted))
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore voteGroupMessage error: ${e.message}")
            }
        }

        mockGroupMessages.value = mockGroupMessages.value.mapValues { (gId, list) ->
            if (gId == groupId) {
                list.map { m ->
                    if (m.id == messageId) m.copy(upvotedBy = newUpvoted, downvotedBy = newDownvoted) else m
                }
            } else list
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
