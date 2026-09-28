package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.components.BitmojiAvatar
import com.example.ui.components.BitmojiPalette
import com.example.ui.theme.SnapchatBlack
import com.example.ui.theme.SnapchatYellow
import com.example.ui.theme.SnapchatYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapchatProfileScreen(
    currentUser: User,
    onSaveProfile: (
        displayName: String,
        statusText: String,
        zodiacSign: String,
        skin: String,
        hair: String,
        hairColor: String,
        outfit: String,
        outfitColor: String,
        mood: String,
        accessory: String,
        background: String,
        pose: String
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current avatar state
    var selectedSkin by remember { mutableStateOf(currentUser.bitmojiSkin) }
    var selectedHair by remember { mutableStateOf(currentUser.bitmojiHair) }
    var selectedHairColor by remember { mutableStateOf(currentUser.bitmojiHairColor) }
    var selectedOutfit by remember { mutableStateOf(currentUser.bitmojiOutfit) }
    var selectedOutfitColor by remember { mutableStateOf(currentUser.bitmojiOutfitColor) }
    var selectedMood by remember { mutableStateOf(currentUser.bitmojiMood) }
    var selectedAccessory by remember { mutableStateOf(currentUser.bitmojiAccessory) }
    var selectedBackground by remember { mutableStateOf(currentUser.bitmojiBackground) }
    var selectedPose by remember { mutableStateOf(currentUser.bitmojiPose) }
    var editDisplayName by remember { mutableStateOf(currentUser.displayName) }
    var editStatusBio by remember { mutableStateOf(currentUser.statusText) }
    var selectedZodiac by remember { mutableStateOf(currentUser.zodiacSign) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSavedToast by remember { mutableStateOf(false) }

    val tabs = listOf("Face & Skin", "Hair", "Outfit", "Accessories", "Backdrop & Pose", "Bio & Zodiac")

    val zodiacOptions = listOf(
        "Aries ♈", "Taurus ♉", "Gemini ♊", "Cancer ♋",
        "Leo ♌", "Virgo ♍", "Libra ♎", "Scorpio ♏",
        "Sagittarius ♐", "Capricorn ♑", "Aquarius ♒", "Pisces ♓"
    )

    val scrollState = rememberScrollState()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kicon Profile Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("snap_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveProfile(
                                editDisplayName,
                                editStatusBio,
                                selectedZodiac,
                                selectedSkin,
                                selectedHair,
                                selectedHairColor,
                                selectedOutfit,
                                selectedOutfitColor,
                                selectedMood,
                                selectedAccessory,
                                selectedBackground,
                                selectedPose
                            )
                            showSavedToast = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SnapchatYellow,
                            contentColor = SnapchatBlack
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_snap_profile_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
            // Success banner
            AnimatedVisibility(
                visible = showSavedToast,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SnapchatYellow)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨ Custom Kicon Profile & Avatar saved! Ready across all chats.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SnapchatBlack,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Official Snapchat Snapcode Card
            SnapcodeCard(
                user = currentUser,
                displayName = editDisplayName,
                skin = selectedSkin,
                hair = selectedHair,
                hairColor = selectedHairColor,
                outfit = selectedOutfit,
                outfitColor = selectedOutfitColor,
                mood = selectedMood,
                accessory = selectedAccessory,
                background = selectedBackground,
                pose = selectedPose,
                zodiac = selectedZodiac
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Customization Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SnapchatYellowDark
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Category Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Face & Skin Tone
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Skin Tone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BitmojiPalette.SkinTones.forEach { (key, color) ->
                                        ColorSwatchCircle(
                                            color = color,
                                            label = key.replaceFirstChar { it.uppercase() },
                                            isSelected = selectedSkin == key,
                                            onClick = { selectedSkin = key }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text("Expression & Mood", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val moods = listOf(
                                    "smile" to "Warm Smile 😊",
                                    "wink" to "Playful Wink 😉",
                                    "cool" to "Cool Shades 😎",
                                    "laugh" to "Laughing Out Loud 😆"
                                )
                                FlowChipSelector(
                                    items = moods,
                                    selectedKey = selectedMood,
                                    onSelect = { selectedMood = it }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Hair Style & Color
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Hair Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val hairStyles = listOf(
                                    "fade" to "Modern Fade 💈",
                                    "buzz" to "Buzz Cut ✂️",
                                    "afro" to "Afro Puffs 👑",
                                    "waves" to "Flowing Waves 🌊",
                                    "spikes" to "Spiky Top ⚡",
                                    "ponytail" to "High Ponytail 🎀",
                                    "bob" to "Classic Bob 💇"
                                )
                                FlowChipSelector(
                                    items = hairStyles,
                                    selectedKey = selectedHair,
                                    onSelect = { selectedHair = it }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text("Hair Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BitmojiPalette.HairColors.forEach { (key, color) ->
                                        ColorSwatchCircle(
                                            color = color,
                                            label = key.replaceFirstChar { it.uppercase() },
                                            isSelected = selectedHairColor == key,
                                            onClick = { selectedHairColor = key }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Outfit & Fashion
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Clothing Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val outfits = listOf(
                                    "snap_hoodie" to "Snap Signature Hoodie 💛",
                                    "street_bomber" to "Cyber Bomber Jacket 🚀",
                                    "suit" to "Tailored Suit & Tie 👔",
                                    "tee" to "Minimalist Crewneck 👕"
                                )
                                FlowChipSelector(
                                    items = outfits,
                                    selectedKey = selectedOutfit,
                                    onSelect = { selectedOutfit = it }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text("Outfit Accent Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BitmojiPalette.OutfitColors.forEach { (key, color) ->
                                        ColorSwatchCircle(
                                            color = color,
                                            label = key.replaceFirstChar { it.uppercase() },
                                            isSelected = selectedOutfitColor == key,
                                            onClick = { selectedOutfitColor = key }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Headwear & Accessories
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Headwear & Gear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val accessories = listOf(
                                    "none" to "No Accessory",
                                    "snapback" to "Snapback Cap 🧢",
                                    "beanie" to "Cozy Knit Beanie 🧶",
                                    "headphones" to "Studio Headphones 🎧",
                                    "glasses" to "Retro Wire Glasses 👓",
                                    "halo" to "Golden Angel Halo 😇",
                                    "crown" to "Royal Gem Crown 👑"
                                )
                                FlowChipSelector(
                                    items = accessories,
                                    selectedKey = selectedAccessory,
                                    onSelect = { selectedAccessory = it }
                                )
                            }
                        }
                    }

                    4 -> {
                        // Backdrop & Pose
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("3D Profile Backdrop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val backdrops = listOf(
                                    "sunset" to "Venice Beach Sunset 🌇",
                                    "neon" to "Cyberpunk Neon 🌌",
                                    "yellow" to "Iconic Snap Yellow ⚡",
                                    "galaxy" to "Cosmic Galaxy 🪐",
                                    "palm" to "Palm Springs Oasis 🌴",
                                    "minimal" to "Minimalist Grid 📐"
                                )
                                FlowChipSelector(
                                    items = backdrops,
                                    selectedKey = selectedBackground,
                                    onSelect = { selectedBackground = it }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text("Kicon Pose", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val poses = listOf(
                                    "peace" to "Peace Sign ✌️",
                                    "thumbs_up" to "Thumbs Up 👍",
                                    "wave" to "Hey Wave 👋"
                                )
                                FlowChipSelector(
                                    items = poses,
                                    selectedKey = selectedPose,
                                    onSelect = { selectedPose = it }
                                )
                            }
                        }
                    }

                    5 -> {
                        // Bio & Zodiac Sign
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Astrological Zodiac Sign", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(zodiacOptions) { sign ->
                                        FilterChip(
                                            selected = selectedZodiac == sign,
                                            onClick = { selectedZodiac = sign },
                                            label = { Text(sign) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SnapchatYellow,
                                                selectedLabelColor = SnapchatBlack
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                OutlinedTextField(
                                    value = editDisplayName,
                                    onValueChange = { editDisplayName = it },
                                    label = { Text("Display Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = editStatusBio,
                                    onValueChange = { editStatusBio = it },
                                    label = { Text("Status Bio") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom CTA
                Button(
                    onClick = {
                        onSaveProfile(
                            editDisplayName,
                            editStatusBio,
                            selectedZodiac,
                            selectedSkin,
                            selectedHair,
                            selectedHairColor,
                            selectedOutfit,
                            selectedOutfitColor,
                            selectedMood,
                            selectedAccessory,
                            selectedBackground,
                            selectedPose
                        )
                        showSavedToast = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnapchatYellow,
                        contentColor = SnapchatBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_snap_profile_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply & Save Kicon Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Snapchat-Style Snapcode Card Component
 */
@Composable
fun SnapcodeCard(
    user: User,
    displayName: String,
    skin: String,
    hair: String,
    hairColor: String,
    outfit: String,
    outfitColor: String,
    mood: String,
    accessory: String,
    background: String,
    pose: String,
    zodiac: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SnapchatYellow),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(2.dp, SnapchatBlack, RoundedCornerShape(24.dp))
            .testTag("snapcode_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✨ KICON CARD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = SnapchatBlack
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SnapchatBlack)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = zodiac,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Snapcode Ghost Frame with Bitmoji in center
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(3.dp, SnapchatBlack, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Outer Dotted Snapcode ring
                Canvas(modifier = Modifier.size(144.dp)) {
                    val r = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dotsCount = 20
                    for (i in 0 until dotsCount) {
                        val angle = (i * 360f / dotsCount) * (Math.PI / 180f)
                        val dx = center.x + (r - 10.dp.toPx()) * Math.cos(angle).toFloat()
                        val dy = center.y + (r - 10.dp.toPx()) * Math.sin(angle).toFloat()
                        drawCircle(SnapchatBlack, radius = 2.5.dp.toPx(), center = Offset(dx, dy))
                    }
                }

                // Bitmoji Center Avatar
                BitmojiAvatar(
                    skin = skin,
                    hair = hair,
                    hairColor = hairColor,
                    outfit = outfit,
                    outfitColor = outfitColor,
                    mood = mood,
                    accessory = accessory,
                    background = background,
                    pose = pose,
                    size = 115.dp,
                    showBackground = true,
                    isCircle = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = displayName.ifBlank { user.displayName },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = SnapchatBlack
                )
            )

            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SnapchatBlack.copy(alpha = 0.75f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Pill Row (Snap Score & Streaks)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SnapchatBlack)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SnapchatYellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${user.snapScore} Kicon Score",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SnapchatBlack)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🔥 ${user.snapStreaks} Streaks",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) SnapchatBlack else Color.Gray.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (color == SnapchatBlack || color == Color(0xFF18181B)) Color.White else SnapchatBlack,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FlowChipSelector(
    items: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { (key, title) ->
                        FilterChip(
                            selected = selectedKey == key,
                            onClick = { onSelect(key) },
                            label = { Text(title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SnapchatYellow,
                                selectedLabelColor = SnapchatBlack
                            )
                        )
                    }
                }
            }
        }
    }
}
