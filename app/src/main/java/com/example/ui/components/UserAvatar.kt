package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StoryGradientColors

@Composable
fun UserAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    isEditable: Boolean = false,
    hasStoryRing: Boolean = false,
    storyRingColors: List<Color> = StoryGradientColors,
    user: com.example.model.User? = null,
    bitmojiSkin: String? = null,
    bitmojiHair: String? = null,
    bitmojiHairColor: String? = null,
    bitmojiOutfit: String? = null,
    bitmojiOutfitColor: String? = null,
    bitmojiMood: String? = null,
    bitmojiAccessory: String? = null,
    onEditClick: () -> Unit = {}
) {
    val initial = displayName.trim().take(1).ifBlank { "?" }.uppercase()
    val fontSize = (size.value * 0.40f).sp

    val outerSize = if (hasStoryRing) size + 6.dp else size
    val innerSize = size

    val effectiveSkin = bitmojiSkin ?: user?.bitmojiSkin ?: "light"
    val effectiveHair = bitmojiHair ?: user?.bitmojiHair ?: "fade"
    val effectiveHairColor = bitmojiHairColor ?: user?.bitmojiHairColor ?: "black"
    val effectiveOutfit = bitmojiOutfit ?: user?.bitmojiOutfit ?: "snap_hoodie"
    val effectiveOutfitColor = bitmojiOutfitColor ?: user?.bitmojiOutfitColor ?: "yellow"
    val effectiveMood = bitmojiMood ?: user?.bitmojiMood ?: "smile"
    val effectiveAccessory = bitmojiAccessory ?: user?.bitmojiAccessory ?: "none"
    val showBitmoji = user?.hasCustomBitmoji == true || photoUrl?.startsWith("bitmoji://") == true || (photoUrl.isNullOrBlank() && bitmojiSkin != null)

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        if (hasStoryRing) {
            Box(
                modifier = Modifier
                    .size(outerSize)
                    .clip(CircleShape)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.sweepGradient(storyRingColors),
                        shape = CircleShape
                    )
            )
        }

        Box(
            modifier = Modifier.size(innerSize),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank() && !photoUrl.startsWith("bitmoji://")) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile picture of $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                        .border(
                            if (hasStoryRing) 1.5.dp else 1.dp,
                            if (hasStoryRing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    loading = {
                        DefaultMonogramAvatar(initial, innerSize, fontSize)
                    },
                    error = {
                        DefaultMonogramAvatar(initial, innerSize, fontSize)
                    }
                )
            } else if (showBitmoji) {
                BitmojiAvatar(
                    skin = effectiveSkin,
                    hair = effectiveHair,
                    hairColor = effectiveHairColor,
                    outfit = effectiveOutfit,
                    outfitColor = effectiveOutfitColor,
                    mood = effectiveMood,
                    accessory = effectiveAccessory,
                    size = innerSize,
                    showBackground = true,
                    isCircle = true
                )
            } else {
                DefaultMonogramAvatar(initial, innerSize, fontSize)
            }

            if (isEditable) {
                Box(
                    modifier = Modifier
                        .size(innerSize * 0.35f)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(innerSize * 0.22f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultMonogramAvatar(
    initial: String,
    size: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}
