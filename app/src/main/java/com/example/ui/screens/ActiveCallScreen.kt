package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CallState
import com.example.model.VoipCallState
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentSky
import kotlin.math.sin

@Composable
fun ActiveCallScreen(
    callState: VoipCallState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    onMinimize: () -> Unit,
    onAcceptCall: () -> Unit,
    onToggleLoopback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val minutes = callState.durationSeconds / 60
    val seconds = callState.durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    // Animated pulse on remote voice activity
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    var showTechDetails by remember { mutableStateOf(false) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_call_screen"),
        color = Color(0xFF0F172A) // Deep slate background for ambient call UI
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient Radial Background Glow based on audio level
            Canvas(modifier = Modifier.fillMaxSize()) {
                val effectiveLevel = if (callState.isMuted) callState.remoteLevel else maxOf(callState.remoteLevel, callState.micLevel)
                val glowRadius = size.minDimension * (0.35f + effectiveLevel * 0.45f)
                val glowColor = if (callState.isMuted) {
                    AccentRose.copy(alpha = 0.08f)
                } else {
                    AccentEmerald.copy(alpha = 0.07f + effectiveLevel * 0.22f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.38f),
                        radius = glowRadius
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarPadding + 12.dp, bottom = navBarPadding + 16.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimize / Back button
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("call_screen_minimize_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Minimize Call",
                            tint = Color.White
                        )
                    }

                    // Connection Quality Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentEmerald.copy(alpha = 0.18f))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { showTechDetails = !showTechDetails }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("call_tech_info_chip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi VoIP",
                            tint = AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (callState.state == CallState.CONNECTED) "WI-FI DIRECT VOIP" else "CONNECTING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    // Tech info toggle
                    IconButton(
                        onClick = { showTechDetails = !showTechDetails },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (showTechDetails) AccentSky.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f))
                            .testTag("toggle_tech_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Technical Details",
                            tint = if (showTechDetails) AccentSky else Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Permission Warning Banner if mic is not allowed
                if (!hasMicPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentRose.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microphone Permission Required",
                                    style = MaterialTheme.typography.labelLarge.copy(color = AccentRose, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Grant audio record permission to stream voice via AudioRecord.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                                )
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Middle section: Avatar, Audio Waveform & Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar with live voice ripple rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        val activeScale = if (callState.remoteLevel > 0.08f) pulseScale else 1f
                        // Outer pulsating ring
                        Box(
                            modifier = Modifier
                                .size(160.dp * activeScale)
                                .clip(CircleShape)
                                .background(AccentEmerald.copy(alpha = if (callState.remoteLevel > 0.08f) 0.15f else 0.05f))
                        )
                        // Middle ring
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald.copy(alpha = if (callState.remoteLevel > 0.05f) 0.25f else 0.1f))
                        )
                        // Inner Avatar
                        UserAvatar(
                            displayName = callState.peerDisplayName.ifBlank { callState.peerUsername },
                            photoUrl = null,
                            size = 110.dp,
                            modifier = Modifier.border(3.dp, AccentEmerald, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (callState.state == CallState.IDLE) "VoIP Voice Calling" else callState.peerDisplayName.ifBlank { "Local Peer" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = if (callState.state == CallState.IDLE) "Low-latency Wi-Fi Audio Streaming" else "@${callState.peerUsername.ifBlank { "peer" }}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Call state / Duration indicator
                    when (callState.state) {
                        CallState.IDLE -> {
                            Text(
                                text = if (callState.isLoopbackTesting) "Loopback Audio Test Running (16kHz PCM)" else "Ready for Wi-Fi Direct Calls",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (callState.isLoopbackTesting) AccentEmerald else AccentSky,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        CallState.OUTGOING_RINGING -> {
                            Text(
                                text = "Ringing peer on Wi-Fi...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = AccentSky,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        CallState.INCOMING_RINGING -> {
                            Text(
                                text = "Incoming Wi-Fi voice call...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        CallState.CONNECTED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formattedDuration,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        CallState.ENDED -> {
                            Text(
                                text = callState.errorMessage ?: "Call Ended",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = AccentRose,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Live Audio Equalizer / Waveform visualizer
                    if (callState.state == CallState.CONNECTED) {
                        AudioWaveformVisualizer(
                            remoteLevel = callState.remoteLevel,
                            micLevel = if (callState.isMuted) 0f else callState.micLevel,
                            wavePhase = wavePhase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mic and Speaker Level Meters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mic Level",
                                    tint = if (callState.isMuted) AccentRose else AccentEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                LinearProgressIndicator(
                                    progress = { if (callState.isMuted) 0f else callState.micLevel },
                                    modifier = Modifier
                                        .height(4.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (callState.isMuted) AccentRose else AccentEmerald,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaker Level",
                                    tint = AccentSky,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                LinearProgressIndicator(
                                    progress = { callState.remoteLevel },
                                    modifier = Modifier
                                        .height(4.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = AccentSky,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }

                    // Collapsible Technical Stream Details (AudioRecord / AudioTrack parameters)
                    AnimatedVisibility(
                        visible = showTechDetails,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "STREAM TELEMETRY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentSky,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Text(
                                        text = if (callState.isLoopbackTesting) "LOOPBACK SELF-TEST" else "P2P DIRECT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (callState.isLoopbackTesting) AccentSky else AccentEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Peer: ${callState.peerHost}:${callState.peerVoipPort}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Text(
                                    text = "Codec: ${callState.codecDescription}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Text(
                                    text = "Packets: ${callState.packetsSent} sent • ${callState.packetsReceived} rcvd (${callState.audioBytesTransferred / 1024} KB)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = onToggleLoopback,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (callState.isLoopbackTesting) AccentSky else Color.White.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Toggle Loopback",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (callState.isLoopbackTesting) "Self-Test Active" else "Test Mic Loopback",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Call Control Deck
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (callState.state == CallState.INCOMING_RINGING) {
                        // Incoming call answer/decline controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decline
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onEndCall,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(AccentRose)
                                        .testTag("decline_call_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "Decline Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Decline", style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
                            }

                            // Accept
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onAcceptCall,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                        .testTag("accept_call_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Accept Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Answer", style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
                            }
                        }
                    } else if (callState.state == CallState.IDLE) {
                        // Idle state: Microphone self-test and instructions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = onToggleLoopback,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (callState.isLoopbackTesting) AccentRose else AccentEmerald
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_loopback_test_idle")
                            ) {
                                Icon(
                                    imageVector = if (callState.isLoopbackTesting) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (callState.isLoopbackTesting) "Stop Mic Loopback Test" else "Start Microphone Self-Test",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "To call a contact, open Chats or Friends and tap the phone icon on their card.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                    } else {
                        // Connected or Outgoing Call Controls: Mute, End Call, Speaker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Microphone Button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onToggleMute,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (callState.isMuted) AccentRose.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.12f)
                                        )
                                        .border(
                                            1.dp,
                                            if (callState.isMuted) AccentRose else Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                        .testTag("call_screen_mute_button")
                                ) {
                                    Icon(
                                        imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = if (callState.isMuted) "Unmute Microphone" else "Mute Microphone",
                                        tint = if (callState.isMuted) AccentRose else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (callState.isMuted) "Muted" else "Mute",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (callState.isMuted) AccentRose else Color.White.copy(alpha = 0.8f)
                                    )
                                )
                            }

                            // End Call Button (Prominent Red 72dp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onEndCall,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(AccentRose)
                                        .testTag("call_screen_end_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "End Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "End Call",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = AccentRose,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Speaker Output Toggle (Speaker vs Earpiece)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onToggleSpeaker,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (callState.isSpeakerOn) AccentSky.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.12f)
                                        )
                                        .border(
                                            1.dp,
                                            if (callState.isSpeakerOn) AccentSky else Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                        .testTag("call_screen_speaker_button")
                                ) {
                                    Icon(
                                        imageVector = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                        contentDescription = if (callState.isSpeakerOn) "Switch to Earpiece" else "Switch to Speakerphone",
                                        tint = if (callState.isSpeakerOn) AccentSky else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (callState.isSpeakerOn) "Speaker" else "Earpiece",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (callState.isSpeakerOn) AccentSky else Color.White.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated real-time audio waveform/frequency equalizer visualizer.
 * Displays dynamic frequency bars reacting directly to microphone & remote voice RMS levels.
 */
@Composable
fun AudioWaveformVisualizer(
    remoteLevel: Float,
    micLevel: Float,
    wavePhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.9f

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / (barCount - 1)
            // Sine modulation combined with live voice amplitude
            val wave = (sin(wavePhase + normalizedX * 5.0) + 1.0) / 2.0 // 0..1
            val dynamicActivity = if (i < barCount / 2) micLevel else remoteLevel
            val effectiveHeight = (maxBarHeight * (0.08f + wave.toFloat() * 0.25f + dynamicActivity * 0.67f)).coerceIn(4f, maxBarHeight)

            val left = i * (barWidth + spacing)
            val top = centerY - effectiveHeight / 2f

            val barColor = if (i < barCount / 2) {
                // Outgoing mic bars
                AccentEmerald.copy(alpha = 0.5f + dynamicActivity * 0.5f)
            } else {
                // Incoming speaker bars
                AccentSky.copy(alpha = 0.5f + dynamicActivity * 0.5f)
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, effectiveHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
