package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonOutline
import com.example.ui.theme.AccentEmerald
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Friend
import com.example.model.GroupChat
import com.example.model.LocalPeer
import com.example.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// MARK: - Local Data Models for Mengobrol Interface
data class MengobrolStory(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isAddButton: Boolean = false,
    val friend: Friend? = null,
    val peer: LocalPeer? = null
)

data class MengobrolChat(
    val id: String,
    val name: String,
    val message: String,
    val time: String,
    val avatarUrl: String,
    val unreadCount: Int = 0,
    val isReadReceipt: Boolean = false,
    val friend: Friend? = null,
    val group: GroupChat? = null,
    val peer: LocalPeer? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    currentUser: User,
    friends: List<Friend>,
    groupChats: List<GroupChat>,
    discoveredPeers: List<LocalPeer>,
    isLocalWifiMode: Boolean,
    onSelectFriend: (Friend) -> Unit,
    onSelectGroupChat: (GroupChat) -> Unit,
    onSelectLocalPeer: (LocalPeer) -> Unit,
    onNavigateFriends: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onStartVoipCall: (Friend) -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateCalls: () -> Unit = {},
    onNavigateModeration: () -> Unit = {},
    onToggleConnectionMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    // Build Stories List: Real contacts and discovered local peers
    val stories = remember(friends, discoveredPeers) {
        val list = mutableListOf<MengobrolStory>()
        list.add(MengobrolStory("add_story", "Add contact", "", isAddButton = true))

        friends.forEach { f ->
            list.add(
                MengobrolStory(
                    id = f.uid,
                    name = f.displayName.split(" ").firstOrNull() ?: f.displayName,
                    avatarUrl = f.photoUrl,
                    friend = f
                )
            )
        }
        discoveredPeers.forEach { p ->
            list.add(
                MengobrolStory(
                    id = p.serviceName,
                    name = p.displayName.split(" ").firstOrNull() ?: p.displayName,
                    avatarUrl = "",
                    peer = p
                )
            )
        }
        list
    }

    // Build Chat List: Real active data only (Community groups, Friends, and Nearby Wi-Fi Peers)
    val chatList = remember(friends, groupChats, discoveredPeers) {
        val list = mutableListOf<MengobrolChat>()

        groupChats.forEach { g ->
            list.add(
                MengobrolChat(
                    id = "group_${g.id}",
                    name = g.name,
                    message = if (g.lastMessageText.isNotBlank()) g.lastMessageText else "Community Group • ${g.memberIds.size} members",
                    time = formatTimestamp(g.lastMessageTimestamp),
                    avatarUrl = g.iconUrl,
                    unreadCount = 0,
                    isReadReceipt = true,
                    group = g
                )
            )
        }
        friends.forEach { f ->
            list.add(
                MengobrolChat(
                    id = "friend_${f.uid}",
                    name = f.displayName,
                    message = if (f.isLocalWifiPeer) "Active on Wi-Fi router" else if (f.statusText.isNotBlank()) f.statusText else if (f.isOnline) "Online" else "Offline",
                    time = formatTimestamp(f.lastSeen),
                    avatarUrl = f.photoUrl,
                    unreadCount = 0,
                    isReadReceipt = true,
                    friend = f
                )
            )
        }
        discoveredPeers.forEach { p ->
            list.add(
                MengobrolChat(
                    id = "peer_${p.serviceName}",
                    name = p.displayName,
                    message = "Nearby Wi-Fi Peer (${p.hostAddress})",
                    time = "Live",
                    avatarUrl = "",
                    unreadCount = 0,
                    isReadReceipt = false,
                    peer = p
                )
            )
        }
        list
    }

    // Filter chat list by search query & category
    val trimmedQuery = searchQuery.trim().lowercase()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredChats = remember(chatList, trimmedQuery) {
        if (trimmedQuery.isEmpty()) chatList
        else chatList.filter {
            it.name.lowercase().contains(trimmedQuery) ||
            it.message.lowercase().contains(trimmedQuery)
        }
    }

    val categorizedChats = remember(filteredChats, selectedFilter) {
        when (selectedFilter) {
            "Direct" -> filteredChats.filter { it.friend != null || it.peer != null }
            "Groups" -> filteredChats.filter { it.group != null }
            "Unread" -> filteredChats.filter { it.unreadCount > 0 }
            else -> filteredChats
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Bar & Action Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search conversations, contacts, groups...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("conversations_search_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Conversation Category Filter Tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("All", "Direct", "Groups", "Unread").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("filter_chip_$filter")
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Stories / Status Row
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(stories) { story ->
                        MengobrolStoryItemView(
                            item = story,
                            onClick = {
                                when {
                                    story.isAddButton -> onNavigateFriends()
                                    story.friend != null -> onSelectFriend(story.friend)
                                    story.peer != null -> onSelectLocalPeer(story.peer)
                                }
                            }
                        )
                    }
                }
            }

            // Chats Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { showMoreMenu = true }
                                .testTag("btn_more_menu")
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isLocalWifiMode) "Network: Nearby Offline" else "Network: Cloud Online") },
                                leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleConnectionMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("VoIP Voice Calls") },
                                leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateCalls()
                                }
                            )
                            if (currentUser.role.canModerate()) {
                                DropdownMenuItem(
                                    text = { Text("Moderation Panel") },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onNavigateModeration()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onNavigateSettings()
                                }
                            )
                        }
                    }
                }
            }

            // Chats List
            if (categorizedChats.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when {
                                trimmedQuery.isNotEmpty() -> "No results found"
                                selectedFilter != "All" -> "No $selectedFilter chats"
                                else -> "No conversations yet"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when {
                                trimmedQuery.isNotEmpty() -> "No chats match \"$searchQuery\""
                                selectedFilter != "All" -> "You don't have any $selectedFilter conversations right now."
                                else -> "Add contacts or create a community group to start chatting."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        if (trimmedQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onNavigateFriends,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("btn_empty_add_contacts")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Find Contacts", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                                }

                                OutlinedButton(
                                    onClick = onCreateGroupClick,
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("btn_empty_create_group")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New Group", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                items(categorizedChats) { chat ->
                    MengobrolChatItemView(
                        chat = chat,
                        onClick = {
                            when {
                                chat.friend != null -> onSelectFriend(chat.friend)
                                chat.group != null -> onSelectGroupChat(chat.group)
                                chat.peer != null -> onSelectLocalPeer(chat.peer)
                            }
                        }
                    )
                }
            }

            // Extra space so bottom items aren't occluded by floating bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Bottom Sheet Modal
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                dragHandle = null,
                scrimColor = Color.Black.copy(alpha = 0.3f)
            ) {
                MengobrolActionBottomSheet(
                    onDismiss = { showBottomSheet = false },
                    onNewChat = {
                        showBottomSheet = false
                        onNavigateFriends()
                    },
                    onNewContact = {
                        showBottomSheet = false
                        onNavigateFriends()
                    },
                    onNewCommunity = {
                        showBottomSheet = false
                        onCreateGroupClick()
                    }
                )
            }
        }
    }
}

// MARK: - Story Item View
@Composable
fun MengobrolStoryItemView(
    item: MengobrolStory,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp)
    ) {
        if (item.isAddButton) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClick = onClick)
                    .testTag("story_add_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onClick)
                    .testTag("story_item_${item.name}"),
                contentAlignment = Alignment.Center
            ) {
                if (item.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.avatarUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = item.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// MARK: - Chat Item View
@Composable
fun MengobrolChatItemView(
    chat: MengobrolChat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("chat_item_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (chat.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = chat.avatarUrl,
                    contentDescription = chat.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = chat.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isReadReceipt) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = AccentEmerald,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(end = 4.dp)
                    )
                }

                Text(
                    text = chat.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = chat.time,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )

            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8BC35)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

// MARK: - Floating Pill Bottom Bar
@Composable
fun MengobrolBottomFloatingBar(
    onNewChatClick: () -> Unit,
    onChatsClick: () -> Unit = {},
    onContactsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(58.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFF7F7F7))
                .border(1.dp, Color(0xFFECECEC), RoundedCornerShape(32.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onChatsClick,
                modifier = Modifier.testTag("floating_nav_chats")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Chats",
                    tint = Color(0xFF1E1E1E),
                    modifier = Modifier.size(22.dp)
                )
            }

            Button(
                onClick = onNewChatClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("floating_nav_new_chat")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New Chat",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onContactsClick,
                modifier = Modifier.testTag("floating_nav_contacts")
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = "Contacts",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// MARK: - Action Sheet Menu
@Composable
fun MengobrolActionBottomSheet(
    onDismiss: () -> Unit,
    onNewChat: () -> Unit = onDismiss,
    onNewContact: () -> Unit = onDismiss,
    onNewCommunity: () -> Unit = onDismiss
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MengobrolActionItem(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    title = "New Chat",
                    subtitle = "Send a message to your contact",
                    onClick = onNewChat
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))
                MengobrolActionItem(
                    icon = Icons.Outlined.PersonOutline,
                    title = "New Contact",
                    subtitle = "Add a contact to be able to send messages",
                    onClick = onNewContact
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))
                MengobrolActionItem(
                    icon = Icons.Outlined.Group,
                    title = "New Community",
                    subtitle = "Join the community around you",
                    onClick = onNewCommunity
                )
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_action_cancel"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(26.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "Cancel",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MengobrolActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "02:11"
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
