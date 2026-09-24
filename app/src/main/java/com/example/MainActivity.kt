package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.notifications.NotificationHelper
import com.example.model.CallState
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.ActiveCallFloatingBar
import com.example.ui.components.AddContactDialog
import com.example.ui.components.AppFloatingNavBar
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.FastStartChatDialog
import com.example.ui.components.IncomingCallBanner
import com.example.ui.components.MinimalTopBar
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GroupChatScreen
import com.example.ui.screens.MengobrolActionBottomSheet
import com.example.ui.screens.ModerationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.TextFlowTheme

class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            mainViewModel = viewModel
            val themeMode by viewModel.themeMode.collectAsState()

            // Request permissions for VoIP and Push Notifications (Android 13+)
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handled gracefully */ }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                val missing = neededPermissions.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isNotEmpty()) {
                    permissionsLauncher.launch(missing.toTypedArray())
                }
            }

            // Handle notification deep links on launch
            LaunchedEffect(intent) {
                handleNotificationIntent(intent, viewModel)
            }

            TextFlowTheme(themeMode = themeMode) {
                TextFlowApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel?.let { handleNotificationIntent(intent, it) }
    }

    private fun handleNotificationIntent(intent: Intent?, viewModel: MainViewModel) {
        if (intent == null) return
        val targetScreen = intent.getStringExtra(NotificationHelper.EXTRA_SCREEN)
        val peerId = intent.getStringExtra(NotificationHelper.EXTRA_PEER_ID)
        val groupId = intent.getStringExtra(NotificationHelper.EXTRA_GROUP_ID)

        when (targetScreen) {
            "CHAT" -> {
                if (!peerId.isNullOrBlank()) {
                    val matchingFriend = viewModel.friends.value.find { it.uid == peerId }
                    if (matchingFriend != null) {
                        viewModel.openChat(matchingFriend)
                    } else {
                        viewModel.navigateTo(Screen.CONVERSATIONS)
                    }
                } else {
                    viewModel.navigateTo(Screen.CONVERSATIONS)
                }
            }
            "GROUP_CHAT" -> {
                if (!groupId.isNullOrBlank()) {
                    val matchingGroup = viewModel.groupChats.value.find { it.id == groupId }
                    if (matchingGroup != null) {
                        viewModel.openGroupChat(matchingGroup)
                    } else {
                        viewModel.navigateTo(Screen.CONVERSATIONS)
                    }
                } else {
                    viewModel.navigateTo(Screen.CONVERSATIONS)
                }
            }
            "FRIENDS" -> {
                viewModel.navigateTo(Screen.FRIENDS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFlowApp(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isLocalWifiMode by viewModel.isLocalWifiMode.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val voipCallState by viewModel.voipCallState.collectAsState()

    val activeChatPeer by viewModel.activeChatPeer.collectAsState()
    val currentChatMessages by viewModel.currentChatMessages.collectAsState()

    val groupChats by viewModel.groupChats.collectAsState()
    val activeGroupChat by viewModel.activeGroupChat.collectAsState()
    val activeGroupMessages by viewModel.activeGroupMessages.collectAsState()

    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val reports by viewModel.reports.collectAsState()

    val firebaseStatusText by viewModel.firebaseStatusText.collectAsState()
    val actionStatusMessage by viewModel.actionStatusMessage.collectAsState()

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showStartChatDialog by remember { mutableStateOf(false) }
    var showQuickActionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFullScreenMode = currentScreen == Screen.CHAT || currentScreen == Screen.GROUP_CHAT || currentScreen == Screen.ACTIVE_CALL

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (currentUser != null && !isFullScreenMode && currentScreen != Screen.AUTH) {
                    MinimalTopBar(
                        title = when (currentScreen) {
                            Screen.CONVERSATIONS -> "Kaiser"
                            Screen.FRIENDS -> "Contacts"
                            Screen.MODERATION -> "Moderation"
                            Screen.SETTINGS -> "Settings"
                            else -> "Kaiser"
                        },
                        currentUser = currentUser,
                        isLocalWifiMode = isLocalWifiMode,
                        onToggleConnectionMode = { viewModel.toggleConnectionMode() },
                        localPeersCount = discoveredPeers.size,
                        themeMode = themeMode,
                        onToggleTheme = { viewModel.toggleThemeMode() },
                        onOpenModeration = if (currentUser?.role?.canModerate() == true) {
                            { viewModel.navigateTo(Screen.MODERATION) }
                        } else null,
                        onBack = if (currentScreen != Screen.CONVERSATIONS) {
                            { viewModel.navigateTo(Screen.CONVERSATIONS) }
                        } else null
                    )
                }
            },
            bottomBar = {
                if (currentUser != null && !isFullScreenMode && currentScreen != Screen.AUTH) {
                    AppFloatingNavBar(
                        currentScreen = currentScreen,
                        unreadChatsCount = 0,
                        pendingFriendsCount = pendingRequests.size,
                        hasActiveCall = voipCallState.state != CallState.IDLE,
                        pendingReportsCount = reports.count { it.status == com.example.model.ReportStatus.PENDING },
                        canModerate = currentUser?.role?.canModerate() == true,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onOpenQuickAction = { showQuickActionSheet = true }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Persistent In-Call Floating Bar across screens when call is active but minimized
                if (currentScreen != Screen.ACTIVE_CALL && currentScreen != Screen.AUTH) {
                    ActiveCallFloatingBar(
                        callState = voipCallState,
                        onExpandCall = { viewModel.navigateTo(Screen.ACTIVE_CALL) },
                        onToggleMute = { viewModel.toggleVoipMute() },
                        onEndCall = { viewModel.endVoipCall() }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        Screen.AUTH -> {
                            AuthScreen(
                                onSignInEmail = { email, pass -> viewModel.signInWithEmail(email, pass) },
                                onSignUpEmail = { email, pass, username, displayName, status ->
                                    viewModel.signUpWithEmail(email, pass, username, displayName, status)
                                },
                                onSignInGoogle = { viewModel.signInWithGoogle() },
                                firebaseStatusText = firebaseStatusText,
                                isLoading = viewModel.isLoading.collectAsState().value,
                                errorMessage = viewModel.errorMessage.collectAsState().value
                            )
                        }

                        Screen.CONVERSATIONS -> {
                            if (currentUser != null) {
                                ConversationsScreen(
                                    currentUser = currentUser!!,
                                    friends = friends,
                                    groupChats = groupChats,
                                    discoveredPeers = discoveredPeers,
                                    isLocalWifiMode = isLocalWifiMode,
                                    onSelectFriend = { friend -> viewModel.openChat(friend) },
                                    onSelectGroupChat = { group -> viewModel.openGroupChat(group) },
                                    onSelectLocalPeer = { peer -> viewModel.openChatWithLocalPeer(peer) },
                                    onNavigateFriends = { viewModel.navigateTo(Screen.FRIENDS) },
                                    onCreateGroupClick = { showCreateGroupDialog = true },
                                    onStartVoipCall = { friend -> viewModel.startVoipCall(friend) },
                                    onNavigateSettings = { viewModel.navigateTo(Screen.SETTINGS) },
                                    onNavigateCalls = { viewModel.navigateTo(Screen.ACTIVE_CALL) },
                                    onNavigateModeration = { viewModel.navigateTo(Screen.MODERATION) },
                                    onToggleConnectionMode = { viewModel.toggleConnectionMode() }
                                )
                            }
                        }

                        Screen.CHAT -> {
                            if (currentUser != null && activeChatPeer != null) {
                                ChatScreen(
                                    currentUser = currentUser!!,
                                    peer = activeChatPeer!!,
                                    messages = currentChatMessages,
                                    onSendMessage = { text, preferLocal -> viewModel.sendMessage(text, preferLocal) },
                                    onStartVoipCall = { peer -> viewModel.startVoipCall(peer) },
                                    onSubmitReport = { reason, excerpt -> viewModel.submitReport(reason, excerpt) },
                                    onBack = { viewModel.navigateTo(Screen.CONVERSATIONS) }
                                )
                            }
                        }

                        Screen.GROUP_CHAT -> {
                            if (currentUser != null && activeGroupChat != null) {
                                GroupChatScreen(
                                    currentUser = currentUser!!,
                                    group = activeGroupChat!!,
                                    messages = activeGroupMessages,
                                    onSendMessage = { text -> viewModel.sendGroupMessage(text) },
                                    onBack = { viewModel.closeGroupChat() }
                                )
                            }
                        }

                        Screen.FRIENDS -> {
                            FriendsScreen(
                                friends = friends,
                                pendingRequests = pendingRequests,
                                searchResults = searchResults,
                                onSearch = { query -> viewModel.searchUsers(query) },
                                onSendFriendRequest = { username -> viewModel.sendFriendRequest(username) },
                                onAcceptRequest = { req -> viewModel.acceptFriendRequest(req) },
                                onDeclineRequest = { req -> viewModel.declineFriendRequest(req) },
                                onStartChat = { friend -> viewModel.openChat(friend) },
                                onBack = { viewModel.navigateTo(Screen.CONVERSATIONS) },
                                statusMessage = actionStatusMessage
                            )
                        }

                        Screen.MODERATION -> {
                            if (currentUser != null) {
                                ModerationScreen(
                                    currentUser = currentUser!!,
                                    allUsers = allUsers,
                                    reports = reports,
                                    onUpdateRole = { uid, role -> viewModel.updateUserRole(uid, role) },
                                    onWarnUser = { uid -> viewModel.warnUser(uid) },
                                    onToggleBanUser = { uid -> viewModel.toggleUserBan(uid) },
                                    onResolveReport = { id, status, note -> viewModel.resolveReport(id, status, note) },
                                    onBack = { viewModel.navigateTo(Screen.CONVERSATIONS) }
                                )
                            }
                        }

                        Screen.SETTINGS -> {
                            if (currentUser != null) {
                                SettingsScreen(
                                    currentUser = currentUser!!,
                                    themeMode = themeMode,
                                    onSelectThemeMode = { mode -> viewModel.setThemeMode(mode) },
                                    onUpdateProfile = { name, status -> viewModel.updateProfile(name, status) },
                                    onUploadProfilePicture = { uri -> viewModel.uploadProfilePicture(uri) },
                                    onTestPushNotification = { type -> viewModel.triggerTestPushNotification(type) },
                                    onSignOut = { viewModel.signOut() },
                                    firebaseStatusText = firebaseStatusText,
                                    localChatPort = viewModel.localChatPort,
                                    localVoipPort = viewModel.localVoipPort,
                                    onBack = { viewModel.navigateTo(Screen.CONVERSATIONS) }
                                )
                            }
                        }

                        Screen.ACTIVE_CALL -> {
                            ActiveCallScreen(
                                callState = voipCallState,
                                onToggleMute = { viewModel.toggleVoipMute() },
                                onToggleSpeaker = { viewModel.toggleVoipSpeaker() },
                                onEndCall = { viewModel.endVoipCall() },
                                onMinimize = {
                                    viewModel.navigateTo(
                                        if (activeChatPeer != null) Screen.CHAT else Screen.CONVERSATIONS
                                    )
                                },
                                onAcceptCall = { viewModel.answerVoipCall() },
                                onToggleLoopback = { viewModel.toggleVoipLoopback() }
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Bottom Sheet
        if (showQuickActionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickActionSheet = false },
                sheetState = sheetState,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                dragHandle = null,
                scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)
            ) {
                MengobrolActionBottomSheet(
                    onDismiss = { showQuickActionSheet = false },
                    onNewChat = {
                        showQuickActionSheet = false
                        showStartChatDialog = true
                    },
                    onNewContact = {
                        showQuickActionSheet = false
                        showAddContactDialog = true
                    },
                    onNewCommunity = {
                        showQuickActionSheet = false
                        showCreateGroupDialog = true
                    }
                )
            }
        }

        // Add Contact Dialog
        if (showAddContactDialog) {
            AddContactDialog(
                onDismiss = { showAddContactDialog = false },
                onSendRequest = { target ->
                    viewModel.sendFriendRequest(target)
                    showAddContactDialog = false
                }
            )
        }

        // Fast Start Chat Dialog
        if (showStartChatDialog) {
            FastStartChatDialog(
                friends = friends,
                discoveredPeers = discoveredPeers,
                onSelectFriend = { friend ->
                    showStartChatDialog = false
                    viewModel.openChat(friend)
                },
                onSelectPeer = { peer ->
                    showStartChatDialog = false
                    viewModel.openChatWithLocalPeer(peer)
                },
                onDismiss = { showStartChatDialog = false }
            )
        }

        // Create Group Dialog
        if (showCreateGroupDialog) {
            CreateGroupDialog(
                friends = friends,
                onDismiss = { showCreateGroupDialog = false },
                onCreateGroup = { name, desc, selectedFriends ->
                    viewModel.createGroupChat(name, desc, selectedFriends)
                    showCreateGroupDialog = false
                }
            )
        }

        // Floating Incoming VoIP Call Banner (shows when incoming call arrives while on any screen)
        if (voipCallState.state == CallState.INCOMING_RINGING && currentScreen != Screen.ACTIVE_CALL) {
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                IncomingCallBanner(
                    callState = voipCallState,
                    onAccept = { viewModel.answerVoipCall() },
                    onDecline = { viewModel.endVoipCall() }
                )
            }
        }
    }
}
