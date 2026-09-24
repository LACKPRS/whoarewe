package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirebaseConfig
import com.example.data.firebase.FirebaseStorageManager
import com.example.data.firebase.FirestoreService
import com.example.data.local.LocalChatSocketManager
import com.example.data.local.LocalNsdHelper
import com.example.data.local.LocalVoipEngine
import com.example.data.notifications.NotificationHelper
import com.example.data.repository.ChatRepository
import com.example.data.repository.FriendsRepository
import com.example.data.repository.GroupChatRepository
import com.example.data.repository.ModerationRepository
import com.example.model.ChatMessage
import com.example.model.Friend
import com.example.model.FriendRequest
import com.example.model.GroupChat
import com.example.model.GroupMessage
import com.example.model.LocalPeer
import com.example.model.Report
import com.example.model.ReportStatus
import com.example.model.ThemeMode
import com.example.model.User
import com.example.model.UserRole
import com.example.model.VoipCallState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    AUTH,
    CONVERSATIONS,
    CHAT,
    GROUP_CHAT,
    FRIENDS,
    MODERATION,
    SETTINGS,
    ACTIVE_CALL
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager: FirebaseAuthManager = FirebaseAuthManager(application)
    private val firestoreService: FirestoreService = FirestoreService(application)
    private val storageManager: FirebaseStorageManager = FirebaseStorageManager(application)
    private val nsdHelper: LocalNsdHelper = LocalNsdHelper(application)
    private val voipEngine: LocalVoipEngine = LocalVoipEngine(application)
    private val localSocketManager: LocalChatSocketManager = LocalChatSocketManager()
    private val chatRepository: ChatRepository = ChatRepository(firestoreService, localSocketManager, nsdHelper)
    private val friendsRepository: FriendsRepository = FriendsRepository(firestoreService, nsdHelper)
    private val groupChatRepository: GroupChatRepository = GroupChatRepository(firestoreService)
    private val moderationRepository: ModerationRepository = ModerationRepository(firestoreService)

    // UI States
    val currentUser: StateFlow<User?> = authManager.currentUser
    val voipCallState: StateFlow<VoipCallState> = voipEngine.callState
    val discoveredPeers: StateFlow<List<LocalPeer>> = nsdHelper.discoveredPeers

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isLocalWifiMode = MutableStateFlow(true)
    val isLocalWifiMode: StateFlow<Boolean> = _isLocalWifiMode.asStateFlow()

    private val _currentScreen = MutableStateFlow(
        if (authManager.currentUser.value != null) Screen.CONVERSATIONS else Screen.AUTH
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeChatPeer = MutableStateFlow<Friend?>(null)
    val activeChatPeer: StateFlow<Friend?> = _activeChatPeer.asStateFlow()

    private val _currentChatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentChatMessages: StateFlow<List<ChatMessage>> = _currentChatMessages.asStateFlow()

    // Group Chats State
    private val _groupChats = MutableStateFlow<List<GroupChat>>(emptyList())
    val groupChats: StateFlow<List<GroupChat>> = _groupChats.asStateFlow()

    private val _activeGroupChat = MutableStateFlow<GroupChat?>(null)
    val activeGroupChat: StateFlow<GroupChat?> = _activeGroupChat.asStateFlow()

    private val _activeGroupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val activeGroupMessages: StateFlow<List<GroupMessage>> = _activeGroupMessages.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _firebaseStatusText = MutableStateFlow(FirebaseConfig.getConfigurationStatus(application))
    val firebaseStatusText: StateFlow<String> = _firebaseStatusText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _actionStatusMessage = MutableStateFlow<String?>(null)
    val actionStatusMessage: StateFlow<String?> = _actionStatusMessage.asStateFlow()

    val localChatPort: Int get() = localSocketManager.localPort
    val localVoipPort: Int get() = voipEngine.voipPort

    private var messagesJob: Job? = null
    private var groupMessagesJob: Job? = null
    private var groupChatsJob: Job? = null
    private var friendsJob: Job? = null
    private var requestsJob: Job? = null

    init {
        // Initialize Notification Channels for FCM & system notifications
        NotificationHelper.initNotificationChannels(application)

        // Try getting FCM registration token
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainViewModel", "FCM Device Token: ${task.result}")
                }
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "FCM token registration fallback: ${e.message}")
        }

        localSocketManager.onMessageReceived = { receivedMsg ->
            chatRepository.onLocalMessageReceived(receivedMsg)
            // Trigger direct message notification if from another user
            val me = currentUser.value
            if (me != null && receivedMsg.senderId != me.uid) {
                NotificationHelper.showDirectMessageNotification(
                    context = application,
                    senderId = receivedMsg.senderId,
                    senderUsername = receivedMsg.senderUsername,
                    senderDisplayName = receivedMsg.senderDisplayName,
                    messageText = receivedMsg.text
                )
            }
        }

        // Start local TCP ServerSocket and VoIP UDP DatagramSocket
        val chatPort = localSocketManager.startServer()
        val vPort = voipEngine.startListening()

        // Observe user authentication state
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    if (_currentScreen.value == Screen.AUTH) {
                        _currentScreen.value = Screen.CONVERSATIONS
                    }
                    nsdHelper.startAdvertising(user, chatPort, vPort)
                    nsdHelper.startDiscovery(user.username)
                    observeUserData(user.uid)
                } else {
                    nsdHelper.stopAdvertising()
                    nsdHelper.stopDiscovery()
                    _currentScreen.value = Screen.AUTH
                }
            }
        }

        // Collect all users for moderation and admin dashboard
        viewModelScope.launch {
            moderationRepository.getAllUsers().collect { users ->
                _allUsers.value = users
            }
        }

        // Collect reports for moderation
        viewModelScope.launch {
            moderationRepository.getAllReports().collect { repList ->
                _reports.value = repList
            }
        }

        // Monitor VoIP call state changes
        viewModelScope.launch {
            voipEngine.callState.collect { vState ->
                if (vState.state == com.example.model.CallState.ENDED && _currentScreen.value == Screen.ACTIVE_CALL) {
                    kotlinx.coroutines.delay(1200)
                    if (_currentScreen.value == Screen.ACTIVE_CALL) {
                        _currentScreen.value = if (_activeChatPeer.value != null) Screen.CHAT else Screen.CONVERSATIONS
                    }
                }
            }
        }
    }

    private fun observeUserData(userId: String) {
        friendsJob?.cancel()
        friendsJob = viewModelScope.launch {
            friendsRepository.getFriendsWithLocalStatus(userId).collect { friendList ->
                _friends.value = friendList
            }
        }

        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            friendsRepository.getPendingRequests(userId).collect { reqList ->
                val prevSize = _pendingRequests.value.size
                _pendingRequests.value = reqList
                // Post notification for incoming friend requests
                if (reqList.size > prevSize && reqList.isNotEmpty()) {
                    val latest = reqList.last()
                    NotificationHelper.showFriendRequestNotification(
                        context = getApplication(),
                        fromUsername = latest.fromUsername,
                        fromDisplayName = latest.fromDisplayName
                    )
                }
            }
        }

        groupChatsJob?.cancel()
        groupChatsJob = viewModelScope.launch {
            groupChatRepository.getGroupChats(userId).collect { groups ->
                _groupChats.value = groups
            }
        }
    }

    // --- Profile Picture Upload (Firebase Storage) ---
    fun uploadProfilePicture(imageUri: Uri) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val uploadResult = storageManager.uploadProfilePicture(user.uid, imageUri)
            uploadResult.onSuccess { photoUrl ->
                authManager.updateProfilePhoto(photoUrl)
                firestoreService.updateUserProfilePhoto(user.uid, photoUrl)
                _actionStatusMessage.value = "Profile picture updated successfully!"
            }.onFailure { error ->
                _errorMessage.value = "Failed to upload photo: ${error.message}"
            }
            _isLoading.value = false
        }
    }

    // --- Navigation & Mode Switching ---
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _errorMessage.value = null
        _actionStatusMessage.value = null
    }

    fun toggleConnectionMode() {
        _isLocalWifiMode.value = !_isLocalWifiMode.value
    }

    fun toggleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    // --- Group Chat Actions ---
    fun openGroupChat(group: GroupChat) {
        _activeGroupChat.value = group
        _currentScreen.value = Screen.GROUP_CHAT

        groupMessagesJob?.cancel()
        groupMessagesJob = viewModelScope.launch {
            groupChatRepository.getGroupMessages(group.id).collect { messages ->
                _activeGroupMessages.value = messages
            }
        }
    }

    fun closeGroupChat() {
        groupMessagesJob?.cancel()
        _activeGroupChat.value = null
        _activeGroupMessages.value = emptyList()
        _currentScreen.value = Screen.CONVERSATIONS
    }

    fun createGroupChat(name: String, description: String, selectedFriends: List<Friend>) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = groupChatRepository.createGroupChat(user, name, description, selectedFriends)
            result.onSuccess { groupId ->
                _actionStatusMessage.value = "Group '$name' created!"
                val newGroup = _groupChats.value.find { it.id == groupId }
                if (newGroup != null) {
                    openGroupChat(newGroup)
                }
            }.onFailure { error ->
                _errorMessage.value = error.message
            }
            _isLoading.value = false
        }
    }

    fun sendGroupMessage(text: String) {
        val group = _activeGroupChat.value ?: return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            groupChatRepository.sendGroupMessage(group, user, text)
        }
    }

    // --- Direct Chat Actions ---
    fun openChat(friend: Friend) {
        _activeChatPeer.value = friend
        _currentScreen.value = Screen.CHAT

        val myUid = currentUser.value?.uid ?: return
        val chatId = FirestoreService.getChatId(myUid, friend.uid)
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.getChatMessages(chatId).collect { msgs ->
                _currentChatMessages.value = msgs
            }
        }
    }

    fun openChatWithLocalPeer(peer: LocalPeer) {
        val friendPeer = Friend(
            uid = "local_${peer.username}",
            username = peer.username,
            displayName = peer.displayName,
            statusText = "Active on Local Wi-Fi Router",
            photoUrl = "",
            role = UserRole.STANDARD,
            isOnline = true,
            isLocalWifiPeer = true,
            localIp = peer.hostAddress,
            localPort = peer.port,
            localVoipPort = peer.voipPort
        )
        openChat(friendPeer)
    }

    fun sendMessage(text: String, preferLocal: Boolean) {
        val peer = _activeChatPeer.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.sendMessage(sender, peer.uid, peer.username, text, preferLocal)
        }
    }

    // --- VoIP Calling Actions ---
    fun startVoipCall(peer: Friend) {
        val sender = currentUser.value ?: return
        val localIp = peer.localIp
        val localPort = peer.localVoipPort
        _currentScreen.value = Screen.ACTIVE_CALL
        if (localIp != null && localPort != null) {
            voipEngine.initiateCall(
                myUsername = sender.username,
                myDisplayName = sender.displayName,
                targetHost = localIp,
                targetPort = localPort,
                peerUsername = peer.username,
                peerDisplayName = peer.displayName
            )
        } else {
            val peerObj = discoveredPeers.value.find { it.username.equals(peer.username, ignoreCase = true) }
            if (peerObj != null) {
                voipEngine.initiateCall(
                    myUsername = sender.username,
                    myDisplayName = sender.displayName,
                    targetHost = peerObj.hostAddress,
                    targetPort = peerObj.voipPort,
                    peerUsername = peer.username,
                    peerDisplayName = peer.displayName
                )
            } else {
                // If peer is not yet discovered on LAN subnet, start local testing call
                voipEngine.initiateCall(
                    myUsername = sender.username,
                    myDisplayName = sender.displayName,
                    targetHost = "127.0.0.1",
                    targetPort = voipEngine.voipPort,
                    peerUsername = peer.username,
                    peerDisplayName = peer.displayName
                )
                _actionStatusMessage.value = "Starting local LAN audio call with @${peer.username}"
            }
        }
    }

    fun answerVoipCall() {
        voipEngine.acceptIncomingCall()
        _currentScreen.value = Screen.ACTIVE_CALL
    }

    fun endVoipCall() {
        voipEngine.endCall()
        if (_currentScreen.value == Screen.ACTIVE_CALL) {
            _currentScreen.value = if (_activeChatPeer.value != null) Screen.CHAT else Screen.CONVERSATIONS
        }
    }

    fun toggleVoipMute() = voipEngine.toggleMute()
    fun toggleVoipSpeaker() = voipEngine.toggleSpeaker()
    fun toggleVoipLoopback() = voipEngine.toggleLoopbackTesting()

    // --- Push Notifications & Diagnostics ---
    fun triggerTestPushNotification(type: String) {
        val context = getApplication<Application>()
        when (type) {
            "DIRECT_MESSAGE" -> {
                NotificationHelper.showDirectMessageNotification(
                    context = context,
                    senderId = "system_notice",
                    senderUsername = "textflow",
                    senderDisplayName = "TextFlow System",
                    messageText = "Notification channel verified."
                )
            }
            "FRIEND_REQUEST" -> {
                NotificationHelper.showFriendRequestNotification(
                    context = context,
                    fromUsername = "textflow_user",
                    fromDisplayName = "New Contact"
                )
            }
            "GROUP_MESSAGE" -> {
                NotificationHelper.showGroupMessageNotification(
                    context = context,
                    groupId = "textflow_channel",
                    groupName = "General",
                    senderUsername = "system",
                    messageText = "Group notification test."
                )
            }
            "FRIEND_ACCEPTED" -> {
                NotificationHelper.showFriendAcceptedNotification(
                    context = context,
                    fromUsername = "contact",
                    fromDisplayName = "Friend Contact"
                )
            }
        }
        _actionStatusMessage.value = "Sent test push notification for $type"
    }

    // --- Friends Actions ---
    fun searchUsers(query: String) {
        viewModelScope.launch {
            _searchResults.value = friendsRepository.searchUsers(query)
        }
    }

    fun sendFriendRequest(targetUsername: String) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            val result = friendsRepository.sendFriendRequest(me, targetUsername)
            result.onSuccess { msg ->
                _actionStatusMessage.value = msg
            }.onFailure { err ->
                _errorMessage.value = err.message
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            friendsRepository.respondToRequest(request, accept = true)
            _actionStatusMessage.value = "Accepted friend request from @${request.fromUsername}"
            NotificationHelper.showFriendAcceptedNotification(
                context = getApplication(),
                fromUsername = request.fromUsername,
                fromDisplayName = request.fromDisplayName
            )
        }
    }

    fun declineFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            friendsRepository.respondToRequest(request, accept = false)
            _actionStatusMessage.value = "Declined friend request from @${request.fromUsername}"
        }
    }

    // --- Authentication Actions ---
    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authManager.signInWithEmail(email, pass)
            result.onSuccess { user ->
                firestoreService.saveUserProfile(user)
                _currentScreen.value = Screen.CONVERSATIONS
            }.onFailure {
                _errorMessage.value = it.message ?: "Sign-in failed"
            }
            _isLoading.value = false
        }
    }

    fun signUpWithEmail(email: String, pass: String, username: String, displayName: String, status: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authManager.signUpWithEmail(email, pass, username, displayName, status)
            result.onSuccess { user ->
                firestoreService.saveUserProfile(user)
                _currentScreen.value = Screen.CONVERSATIONS
            }.onFailure {
                _errorMessage.value = it.message ?: "Sign-up failed"
            }
            _isLoading.value = false
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authManager.signInWithGoogle()
            result.onSuccess { user ->
                firestoreService.saveUserProfile(user)
                _currentScreen.value = Screen.CONVERSATIONS
            }.onFailure {
                _errorMessage.value = it.message ?: "Google Sign-In failed"
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(displayName: String, statusText: String) {
        authManager.updateProfile(displayName, statusText)
        val updated = currentUser.value ?: return
        viewModelScope.launch {
            firestoreService.saveUserProfile(updated)
        }
    }

    fun signOut() {
        authManager.signOut()
        _currentScreen.value = Screen.AUTH
    }

    // --- Moderation Actions ---
    fun submitReport(reason: String, excerpt: String) {
        val reporter = currentUser.value ?: return
        val reported = activeChatPeer.value ?: return
        val targetUser = User(
            uid = reported.uid,
            username = reported.username,
            displayName = reported.displayName,
            statusText = reported.statusText,
            photoUrl = reported.photoUrl,
            role = reported.role
        )
        viewModelScope.launch {
            moderationRepository.submitReport(
                reporter = reporter,
                targetUser = targetUser,
                reason = reason,
                messageExcerpt = excerpt
            )
            _actionStatusMessage.value = "Report submitted to moderators."
        }
    }

    fun resolveReport(reportId: String, status: ReportStatus, note: String) {
        viewModelScope.launch {
            moderationRepository.resolveReport(reportId, status, note)
            _actionStatusMessage.value = "Report resolved: ${status.name}"
        }
    }

    fun updateUserRole(uid: String, role: UserRole) {
        viewModelScope.launch {
            moderationRepository.updateUserRole(uid, role)
            _actionStatusMessage.value = "Role updated."
        }
    }

    fun warnUser(uid: String) {
        viewModelScope.launch {
            moderationRepository.warnUser(uid)
            _actionStatusMessage.value = "Warning issued to user."
        }
    }

    fun toggleUserBan(uid: String) {
        viewModelScope.launch {
            val result = moderationRepository.toggleUserBan(uid)
            result.onSuccess { isBanned ->
                _actionStatusMessage.value = if (isBanned) "User has been banned." else "User unbanned."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        localSocketManager.stopServer()
        voipEngine.shutdown()
        nsdHelper.stopAdvertising()
        nsdHelper.stopDiscovery()
    }
}
