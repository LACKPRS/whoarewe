package com.example.kaiser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaiserTheme {
                KaiserApp()
            }
        }
    }
}

@Composable
fun KaiserTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF1E88E5),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3F2FD),
        onPrimaryContainer = Color(0xFF0D47A1),
        secondary = Color(0xFF26A69A),
        onSecondary = Color.White,
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF212121),
        background = Color(0xFFF5F5F5),
        onBackground = Color(0xFF212121)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

enum class Screen(val title: String) {
    CHATS("Chats"),
    GROUPS("Groups"),
    FRIENDS("Friends"),
    KICON("Kicon Studio")
}

data class ChatMessage(val sender: String, val message: String, val time: String, val isSelf: Boolean)
data class UserFriend(val id: String, val name: String, val username: String, val status: String, val isFriend: Boolean)
data class GroupChat(val id: String, val name: String, val memberCount: Int, val lastMessage: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaiserApp() {
    var currentScreen by remember { mutableStateOf(Screen.CHATS) }

    var friends by remember {
        mutableStateOf(
            listOf(
                UserFriend("1", "Alex Morgan", "@alexm", "Active now", true),
                UserFriend("2", "Jordan Lee", "@jordan", "Online 1h ago", true),
                UserFriend("3", "Taylor Swift", "@taylor", "Pending request...", false),
                UserFriend("4", "Sam Rivera", "@samr", "Suggested for you", false)
            )
        )
    }

    var groups by remember {
        mutableStateOf(
            listOf(
                GroupChat("101", "Kaiser Core Devs", 8, "Build passed successfully!"),
                GroupChat("102", "Android Enthusiasts", 42, "Check out the latest release!"),
                GroupChat("103", "Design Sprint", 12, "Reviewing Kicon Score components.")
            )
        )
    }

    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("Alex Morgan", "Hey! Have you seen the latest CI build?", "10:14 AM", false),
                ChatMessage("You", "Yes, GitHub Actions is building the debug APK automatically now!", "10:15 AM", true),
                ChatMessage("Alex Morgan", "Awesome, releases tagged 'latest' are working smoothly.", "10:16 AM", false)
            )
        )
    }

    var newMessageText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var kiconScore by remember { mutableIntStateOf(850) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kaiser • " + currentScreen.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.CHATS,
                    onClick = { currentScreen = Screen.CHATS },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.GROUPS,
                    onClick = { currentScreen = Screen.GROUPS },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Groups") },
                    label = { Text("Groups") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.FRIENDS,
                    onClick = { currentScreen = Screen.FRIENDS },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Friends") },
                    label = { Text("Friends") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.KICON,
                    onClick = { currentScreen = Screen.KICON },
                    icon = { Icon(Icons.Default.AccountBox, contentDescription = "Kicon") },
                    label = { Text("Kicon Studio") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.CHATS -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { chat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (chat.isSelf) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (chat.isSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (!chat.isSelf) {
                                                Text(
                                                    text = chat.sender,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = chat.message,
                                                color = if (chat.isSelf) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = chat.time,
                                                fontSize = 10.sp,
                                                color = if (chat.isSelf) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMessageText,
                                onValueChange = { newMessageText = it },
                                placeholder = { Text("Type a message...") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newMessageText.isNotBlank()) {
                                        chatMessages = chatMessages + ChatMessage("You", newMessageText.trim(), "Just now", true)
                                        newMessageText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Screen.GROUPS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groups) { group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "${group.memberCount} members", fontSize = 12.sp, color = Color.Gray)
                                        Text(text = group.lastMessage, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Screen.FRIENDS -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search friends by name or @username...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(friends.filter { it.name.contains(searchQuery, ignoreCase = true) || it.username.contains(searchQuery, ignoreCase = true) }) { friend ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = friend.name.take(1),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = friend.name, fontWeight = FontWeight.Bold)
                                            Text(text = friend.username, fontSize = 12.sp, color = Color.Gray)
                                            Text(text = friend.status, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Button(
                                            onClick = {
                                                friends = friends.map {
                                                    if (it.id == friend.id) it.copy(isFriend = !it.isFriend, status = if (!it.isFriend) "Friends now" else "Request sent") else it
                                                }
                                            }
                                        ) {
                                            Text(if (friend.isFriend) "Message" else "Add Friend")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Screen.KICON -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Kicon Profile Card", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("Verified Community Member", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Score", fontSize = 12.sp, color = Color.Gray)
                                        Text("$kiconScore", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Badges", fontSize = 12.sp, color = Color.Gray)
                                        Text("Level 5", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { kiconScore += 10 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Boost Kicon Score (+10)")
                        }
                    }
                }
            }
        }
    }
}
