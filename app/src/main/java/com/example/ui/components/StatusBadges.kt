package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeliveryMode
import com.example.model.UserRole
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentSky

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (role) {
        UserRole.ADMIN -> AccentRose.copy(alpha = 0.2f) to AccentRose
        UserRole.MODERATOR -> AccentAmber.copy(alpha = 0.2f) to AccentAmber
        UserRole.STANDARD -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .testTag("role_badge_${role.name.lowercase()}")
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = role.label.uppercase(),
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun OnlineStatusDot(
    isOnline: Boolean,
    isLocalWifi: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = when {
        isLocalWifi -> AccentEmerald
        isOnline -> AccentSky
        else -> Color.Gray
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
    )
}

@Composable
fun DeliveryModeBadge(
    deliveryMode: DeliveryMode,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (deliveryMode) {
        DeliveryMode.LOCAL_ROUTER_P2P -> "ROUTER P2P" to AccentEmerald
        DeliveryMode.CLOUD_REALTIME -> "CLOUD" to AccentSky
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
