package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SnapchatYellow

object BitmojiPalette {
    val SkinTones = mapOf(
        "fair" to Color(0xFFFFDFC4),
        "light" to Color(0xFFF3C99F),
        "sand" to Color(0xFFE2B58B),
        "golden" to Color(0xFFC98A58),
        "caramel" to Color(0xFF9A6237),
        "deep" to Color(0xFF663C1F),
        "espresso" to Color(0xFF3E2112)
    )

    val HairColors = mapOf(
        "black" to Color(0xFF1E1E24),
        "brown" to Color(0xFF4A3324),
        "blonde" to Color(0xFFE8BD6D),
        "auburn" to Color(0xFF993311),
        "pink" to Color(0xFFF472B6),
        "blue" to Color(0xFF0284C7),
        "purple" to Color(0xFF8B5CF6),
        "silver" to Color(0xFF94A3B8)
    )

    val OutfitColors = mapOf(
        "yellow" to SnapchatYellow,
        "cyan" to Color(0xFF06B6D4),
        "red" to Color(0xFFEF4444),
        "purple" to Color(0xFF8B5CF6),
        "black" to Color(0xFF18181B),
        "emerald" to Color(0xFF10B981),
        "white" to Color(0xFFF8FAFC)
    )

    val BackgroundThemes = mapOf(
        "sunset" to listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)),
        "neon" to listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)),
        "yellow" to listOf(SnapchatYellow, Color(0xFFF5DE00)),
        "galaxy" to listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)),
        "palm" to listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
        "minimal" to listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
    )
}

/**
 * Customizable Snapchat 3D/Vector Bitmoji Avatar
 */
@Composable
fun BitmojiAvatar(
    skin: String = "light",
    hair: String = "fade",
    hairColor: String = "black",
    outfit: String = "snap_hoodie",
    outfitColor: String = "yellow",
    mood: String = "smile",
    accessory: String = "none",
    background: String = "sunset",
    pose: String = "peace",
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    isCircle: Boolean = true
) {
    val skinColor = BitmojiPalette.SkinTones[skin] ?: Color(0xFFF3C99F)
    val hairCol = BitmojiPalette.HairColors[hairColor] ?: Color(0xFF1E1E24)
    val outfitCol = BitmojiPalette.OutfitColors[outfitColor] ?: SnapchatYellow
    val bgGradients = BitmojiPalette.BackgroundThemes[background] ?: BitmojiPalette.BackgroundThemes["sunset"]!!

    val clipShape = if (isCircle) CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(clipShape)
            .then(
                if (showBackground) Modifier.background(Brush.linearGradient(bgGradients))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Draw Torso / Outfit & Shoulders
            drawBitmojiTorso(w, h, outfit, outfitCol)

            // 2. Draw Neck
            val neckWidth = w * 0.22f
            val neckHeight = h * 0.22f
            val neckLeft = (w - neckWidth) / 2f
            val neckTop = h * 0.52f
            drawRect(
                color = skinColor.copy(alpha = 0.95f),
                topLeft = Offset(neckLeft, neckTop),
                size = Size(neckWidth, neckHeight)
            )

            // 3. Draw Head (Face Shape)
            val headRadius = w * 0.26f
            val headCenter = Offset(w * 0.5f, h * 0.44f)
            drawCircle(
                color = skinColor,
                radius = headRadius,
                center = headCenter
            )

            // Subtle Cheek Blush
            val blushColor = Color(0xFFFF8A80).copy(alpha = 0.35f)
            drawCircle(blushColor, radius = headRadius * 0.22f, center = Offset(headCenter.x - headRadius * 0.55f, headCenter.y + headRadius * 0.22f))
            drawCircle(blushColor, radius = headRadius * 0.22f, center = Offset(headCenter.x + headRadius * 0.55f, headCenter.y + headRadius * 0.22f))

            // 4. Draw Hair Base (Behind face or sides)
            drawBitmojiHair(w, h, headCenter, headRadius, hair, hairCol)

            // 5. Draw Eyes, Eyebrows & Mood
            drawBitmojiMood(w, h, headCenter, headRadius, mood)

            // 6. Draw Accessories (glasses, headphones, hat, halo, crown)
            drawBitmojiAccessory(w, h, headCenter, headRadius, accessory)

            // 7. Draw Pose Hand / Gesture if applicable (peace, thumbs up, wave)
            drawBitmojiPose(w, h, skinColor, pose)
        }
    }
}

private fun DrawScope.drawBitmojiTorso(w: Float, h: Float, outfit: String, color: Color) {
    val shoulderPath = Path().apply {
        moveTo(w * 0.12f, h)
        cubicTo(w * 0.16f, h * 0.70f, w * 0.32f, h * 0.65f, w * 0.38f, h * 0.68f)
        lineTo(w * 0.62f, h * 0.68f)
        cubicTo(w * 0.68f, h * 0.65f, w * 0.84f, h * 0.70f, w * 0.88f, h)
        close()
    }
    drawPath(shoulderPath, color)

    // Collar / Details
    when (outfit) {
        "snap_hoodie" -> {
            // Draw Hoodie drawstrings & pouch line
            val innerCollar = Path().apply {
                moveTo(w * 0.38f, h * 0.68f)
                cubicTo(w * 0.44f, h * 0.82f, w * 0.56f, h * 0.82f, w * 0.62f, h * 0.68f)
                close()
            }
            drawPath(innerCollar, Color.Black.copy(alpha = 0.25f))
            // Strings
            drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.45f, h * 0.78f), Offset(w * 0.45f, h * 0.90f), strokeWidth = w * 0.02f)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.55f, h * 0.78f), Offset(w * 0.55f, h * 0.90f), strokeWidth = w * 0.02f)
        }
        "suit" -> {
            // White shirt triangle & tie
            val shirtPath = Path().apply {
                moveTo(w * 0.42f, h * 0.68f)
                lineTo(w * 0.5f, h * 0.85f)
                lineTo(w * 0.58f, h * 0.68f)
                close()
            }
            drawPath(shirtPath, Color.White)
            // Tie
            val tiePath = Path().apply {
                moveTo(w * 0.48f, h * 0.74f)
                lineTo(w * 0.52f, h * 0.74f)
                lineTo(w * 0.53f, h * 0.92f)
                lineTo(w * 0.5f, h * 0.96f)
                lineTo(w * 0.47f, h * 0.92f)
                close()
            }
            drawPath(tiePath, Color(0xFFEF4444))
        }
        "street_bomber" -> {
            // Zipper in center
            drawLine(Color.Black.copy(alpha = 0.4f), Offset(w * 0.5f, h * 0.68f), Offset(w * 0.5f, h), strokeWidth = w * 0.03f)
            // Neon accent stripe
            drawLine(Color(0xFF06B6D4), Offset(w * 0.30f, h * 0.76f), Offset(w * 0.38f, h * 0.76f), strokeWidth = w * 0.035f)
        }
        else -> {
            // Minimal Crewneck ring
            drawArc(
                color = Color.Black.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.38f, h * 0.64f),
                size = Size(w * 0.24f, h * 0.12f),
                style = Stroke(width = w * 0.03f)
            )
        }
    }
}

private fun DrawScope.drawBitmojiHair(
    w: Float,
    h: Float,
    headCenter: Offset,
    headRadius: Float,
    hair: String,
    hairColor: Color
) {
    when (hair) {
        "buzz" -> {
            drawArc(
                color = hairColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 1.02f, headCenter.y - headRadius * 1.08f),
                size = Size(headRadius * 2.04f, headRadius * 1.8f)
            )
        }
        "afro" -> {
            drawCircle(
                color = hairColor,
                radius = headRadius * 1.25f,
                center = Offset(headCenter.x, headCenter.y - headRadius * 0.25f)
            )
        }
        "spikes" -> {
            // Top spikes
            val spikePath = Path().apply {
                moveTo(headCenter.x - headRadius * 0.9f, headCenter.y - headRadius * 0.3f)
                lineTo(headCenter.x - headRadius * 0.6f, headCenter.y - headRadius * 1.35f)
                lineTo(headCenter.x - headRadius * 0.2f, headCenter.y - headRadius * 0.85f)
                lineTo(headCenter.x, headCenter.y - headRadius * 1.45f)
                lineTo(headCenter.x + headRadius * 0.2f, headCenter.y - headRadius * 0.85f)
                lineTo(headCenter.x + headRadius * 0.6f, headCenter.y - headRadius * 1.35f)
                lineTo(headCenter.x + headRadius * 0.9f, headCenter.y - headRadius * 0.3f)
                close()
            }
            drawPath(spikePath, hairColor)
        }
        "waves" -> {
            // Flowing hair on sides & top
            drawCircle(hairColor, radius = headRadius * 0.65f, center = Offset(headCenter.x - headRadius * 0.85f, headCenter.y + headRadius * 0.35f))
            drawCircle(hairColor, radius = headRadius * 0.65f, center = Offset(headCenter.x + headRadius * 0.85f, headCenter.y + headRadius * 0.35f))
            drawArc(
                color = hairColor,
                startAngle = 170f,
                sweepAngle = 200f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 1.08f, headCenter.y - headRadius * 1.25f),
                size = Size(headRadius * 2.16f, headRadius * 2.1f)
            )
        }
        "ponytail" -> {
            // High ponytail knot
            drawCircle(hairColor, radius = headRadius * 0.45f, center = Offset(headCenter.x + headRadius * 0.85f, headCenter.y - headRadius * 0.95f))
            drawArc(
                color = hairColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.15f),
                size = Size(headRadius * 2.1f, headRadius * 1.8f)
            )
        }
        "bob" -> {
            // Crisp sleek bob covering sides
            val bobPath = Path().apply {
                moveTo(headCenter.x - headRadius * 1.15f, headCenter.y + headRadius * 0.65f)
                lineTo(headCenter.x - headRadius * 1.15f, headCenter.y - headRadius * 0.5f)
                cubicTo(headCenter.x - headRadius, headCenter.y - headRadius * 1.3f, headCenter.x + headRadius, headCenter.y - headRadius * 1.3f, headCenter.x + headRadius * 1.15f, headCenter.y - headRadius * 0.5f)
                lineTo(headCenter.x + headRadius * 1.15f, headCenter.y + headRadius * 0.65f)
                lineTo(headCenter.x + headRadius * 0.75f, headCenter.y + headRadius * 0.5f)
                lineTo(headCenter.x, headCenter.y - headRadius * 0.4f)
                lineTo(headCenter.x - headRadius * 0.75f, headCenter.y + headRadius * 0.5f)
                close()
            }
            drawPath(bobPath, hairColor)
        }
        else -> {
            // Default Modern Fade / Clean Cut
            val fadePath = Path().apply {
                moveTo(headCenter.x - headRadius * 0.95f, headCenter.y - headRadius * 0.2f)
                cubicTo(headCenter.x - headRadius * 0.8f, headCenter.y - headRadius * 1.35f, headCenter.x + headRadius * 0.8f, headCenter.y - headRadius * 1.35f, headCenter.x + headRadius * 0.95f, headCenter.y - headRadius * 0.2f)
                cubicTo(headCenter.x + headRadius * 0.5f, headCenter.y - headRadius * 0.65f, headCenter.x - headRadius * 0.5f, headCenter.y - headRadius * 0.65f, headCenter.x - headRadius * 0.95f, headCenter.y - headRadius * 0.2f)
                close()
            }
            drawPath(fadePath, hairColor)
        }
    }
}

private fun DrawScope.drawBitmojiMood(
    w: Float,
    h: Float,
    headCenter: Offset,
    headRadius: Float,
    mood: String
) {
    val eyeY = headCenter.y - headRadius * 0.05f
    val eyeSpacing = headRadius * 0.42f
    val eyeRadius = headRadius * 0.13f

    // Eyebrows
    val browY = eyeY - headRadius * 0.26f
    val browWidth = headRadius * 0.28f
    drawLine(Color(0xFF262626), Offset(headCenter.x - eyeSpacing - browWidth / 2f, browY), Offset(headCenter.x - eyeSpacing + browWidth / 2f, browY - headRadius * 0.05f), strokeWidth = w * 0.02f)
    drawLine(Color(0xFF262626), Offset(headCenter.x + eyeSpacing - browWidth / 2f, browY - headRadius * 0.05f), Offset(headCenter.x + eyeSpacing + browWidth / 2f, browY), strokeWidth = w * 0.02f)

    when (mood) {
        "wink" -> {
            // Right eye wink (arc)
            drawCircle(Color.White, radius = eyeRadius, center = Offset(headCenter.x - eyeSpacing, eyeY))
            drawCircle(Color(0xFF1E293B), radius = eyeRadius * 0.6f, center = Offset(headCenter.x - eyeSpacing, eyeY))
            drawCircle(Color.White, radius = eyeRadius * 0.25f, center = Offset(headCenter.x - eyeSpacing + eyeRadius * 0.2f, eyeY - eyeRadius * 0.2f))

            // Left eye closed in playful wink
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(headCenter.x + eyeSpacing - eyeRadius, eyeY - eyeRadius * 0.3f),
                size = Size(eyeRadius * 2f, eyeRadius * 0.8f),
                style = Stroke(width = w * 0.025f)
            )

            // Playful smirk smile
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.28f, headCenter.y + headRadius * 0.32f),
                size = Size(headRadius * 0.56f, headRadius * 0.35f),
                style = Stroke(width = w * 0.025f)
            )
        }
        "cool" -> {
            // Cool Sunglasses
            val glassesWidth = headRadius * 1.5f
            val glassesHeight = headRadius * 0.65f
            val glassesLeft = headCenter.x - glassesWidth / 2f
            val glassesTop = eyeY - glassesHeight * 0.4f
            drawRoundRect(
                color = Color(0xFF111827),
                topLeft = Offset(glassesLeft, glassesTop),
                size = Size(glassesWidth, glassesHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.22f)
            )
            // Lens sheen
            drawLine(
                Color.White.copy(alpha = 0.55f),
                Offset(glassesLeft + glassesWidth * 0.15f, glassesTop + glassesHeight * 0.2f),
                Offset(glassesLeft + glassesWidth * 0.35f, glassesTop + glassesHeight * 0.8f),
                strokeWidth = w * 0.02f
            )

            // Confident half smile
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.22f, headCenter.y + headRadius * 0.42f),
                size = Size(headRadius * 0.5f, headRadius * 0.26f),
                style = Stroke(width = w * 0.025f)
            )
        }
        "laugh" -> {
            // Curved laughing eyes
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(headCenter.x - eyeSpacing - eyeRadius, eyeY - eyeRadius * 0.3f),
                size = Size(eyeRadius * 2f, eyeRadius * 0.8f),
                style = Stroke(width = w * 0.025f)
            )
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(headCenter.x + eyeSpacing - eyeRadius, eyeY - eyeRadius * 0.3f),
                size = Size(eyeRadius * 2f, eyeRadius * 0.8f),
                style = Stroke(width = w * 0.025f)
            )

            // Open happy laughing mouth
            val mouthPath = Path().apply {
                moveTo(headCenter.x - headRadius * 0.32f, headCenter.y + headRadius * 0.32f)
                quadraticBezierTo(headCenter.x, headCenter.y + headRadius * 0.72f, headCenter.x + headRadius * 0.32f, headCenter.y + headRadius * 0.32f)
                close()
            }
            drawPath(mouthPath, Color(0xFF881337))
            // Tongue
            drawCircle(Color(0xFFFB7185), radius = headRadius * 0.18f, center = Offset(headCenter.x, headCenter.y + headRadius * 0.54f))
        }
        else -> {
            // Classic bright smile with vibrant eyes
            drawCircle(Color.White, radius = eyeRadius, center = Offset(headCenter.x - eyeSpacing, eyeY))
            drawCircle(Color(0xFF1E293B), radius = eyeRadius * 0.65f, center = Offset(headCenter.x - eyeSpacing, eyeY))
            drawCircle(Color.White, radius = eyeRadius * 0.28f, center = Offset(headCenter.x - eyeSpacing + eyeRadius * 0.2f, eyeY - eyeRadius * 0.2f))

            drawCircle(Color.White, radius = eyeRadius, center = Offset(headCenter.x + eyeSpacing, eyeY))
            drawCircle(Color(0xFF1E293B), radius = eyeRadius * 0.65f, center = Offset(headCenter.x + eyeSpacing, eyeY))
            drawCircle(Color.White, radius = eyeRadius * 0.28f, center = Offset(headCenter.x + eyeSpacing + eyeRadius * 0.2f, eyeY - eyeRadius * 0.2f))

            // Smile
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 0.28f, headCenter.y + headRadius * 0.32f),
                size = Size(headRadius * 0.56f, headRadius * 0.34f),
                style = Stroke(width = w * 0.025f)
            )
        }
    }
}

private fun DrawScope.drawBitmojiAccessory(
    w: Float,
    h: Float,
    headCenter: Offset,
    headRadius: Float,
    accessory: String
) {
    when (accessory) {
        "snapback" -> {
            // Cap brim
            drawRoundRect(
                color = SnapchatYellow,
                topLeft = Offset(headCenter.x - headRadius * 1.15f, headCenter.y - headRadius * 0.95f),
                size = Size(headRadius * 2.3f, headRadius * 0.38f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.15f)
            )
            // Cap crown
            drawArc(
                color = Color(0xFF18181B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 0.95f, headCenter.y - headRadius * 1.55f),
                size = Size(headRadius * 1.9f, headRadius * 1.25f)
            )
        }
        "beanie" -> {
            drawArc(
                color = Color(0xFFE11D48),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius * 1.05f, headCenter.y - headRadius * 1.6f),
                size = Size(headRadius * 2.1f, headRadius * 1.45f)
            )
            // Beanie fold line
            drawRoundRect(
                color = Color(0xFF9F1239),
                topLeft = Offset(headCenter.x - headRadius * 1.08f, headCenter.y - headRadius * 0.95f),
                size = Size(headRadius * 2.16f, headRadius * 0.28f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(headRadius * 0.1f)
            )
        }
        "headphones" -> {
            // Headband arc
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(headCenter.x - headRadius * 1.18f, headCenter.y - headRadius * 1.25f),
                size = Size(headRadius * 2.36f, headRadius * 1.8f),
                style = Stroke(width = w * 0.045f)
            )
            // Ear cushions
            drawRoundRect(Color(0xFF06B6D4), Offset(headCenter.x - headRadius * 1.25f, headCenter.y - headRadius * 0.25f), Size(headRadius * 0.32f, headRadius * 0.72f), androidx.compose.ui.geometry.CornerRadius(headRadius * 0.12f))
            drawRoundRect(Color(0xFF06B6D4), Offset(headCenter.x + headRadius * 0.93f, headCenter.y - headRadius * 0.25f), Size(headRadius * 0.32f, headRadius * 0.72f), androidx.compose.ui.geometry.CornerRadius(headRadius * 0.12f))
        }
        "glasses" -> {
            // Wireframe glasses
            val r = headRadius * 0.28f
            drawCircle(Color(0xFFE2E8F0).copy(alpha = 0.4f), radius = r, center = Offset(headCenter.x - headRadius * 0.42f, headCenter.y - headRadius * 0.05f))
            drawCircle(Color(0xFF0F172A), radius = r, center = Offset(headCenter.x - headRadius * 0.42f, headCenter.y - headRadius * 0.05f), style = Stroke(w * 0.02f))

            drawCircle(Color(0xFFE2E8F0).copy(alpha = 0.4f), radius = r, center = Offset(headCenter.x + headRadius * 0.42f, headCenter.y - headRadius * 0.05f))
            drawCircle(Color(0xFF0F172A), radius = r, center = Offset(headCenter.x + headRadius * 0.42f, headCenter.y - headRadius * 0.05f), style = Stroke(w * 0.02f))

            // Bridge
            drawLine(Color(0xFF0F172A), Offset(headCenter.x - headRadius * 0.14f, headCenter.y - headRadius * 0.05f), Offset(headCenter.x + headRadius * 0.14f, headCenter.y - headRadius * 0.05f), strokeWidth = w * 0.02f)
        }
        "halo" -> {
            drawOval(
                color = SnapchatYellow,
                topLeft = Offset(headCenter.x - headRadius * 0.8f, headCenter.y - headRadius * 1.55f),
                size = Size(headRadius * 1.6f, headRadius * 0.45f),
                style = Stroke(width = w * 0.045f)
            )
        }
        "crown" -> {
            val crownPath = Path().apply {
                moveTo(headCenter.x - headRadius * 0.65f, headCenter.y - headRadius * 0.95f)
                lineTo(headCenter.x - headRadius * 0.8f, headCenter.y - headRadius * 1.5f)
                lineTo(headCenter.x - headRadius * 0.25f, headCenter.y - headRadius * 1.15f)
                lineTo(headCenter.x, headCenter.y - headRadius * 1.65f)
                lineTo(headCenter.x + headRadius * 0.25f, headCenter.y - headRadius * 1.15f)
                lineTo(headCenter.x + headRadius * 0.8f, headCenter.y - headRadius * 1.5f)
                lineTo(headCenter.x + headRadius * 0.65f, headCenter.y - headRadius * 0.95f)
                close()
            }
            drawPath(crownPath, SnapchatYellow)
            // Jewels
            drawCircle(Color(0xFFEF4444), radius = headRadius * 0.08f, center = Offset(headCenter.x, headCenter.y - headRadius * 1.25f))
        }
    }
}

private fun DrawScope.drawBitmojiPose(
    w: Float,
    h: Float,
    skinColor: Color,
    pose: String
) {
    when (pose) {
        "peace" -> {
            // Peace hand overlay on bottom right
            val handCenter = Offset(w * 0.78f, h * 0.75f)
            val handRadius = w * 0.11f
            // Palm
            drawCircle(skinColor, radius = handRadius, center = handCenter)
            // 2 fingers up
            drawRoundRect(skinColor, Offset(handCenter.x - handRadius * 0.7f, handCenter.y - handRadius * 1.9f), Size(handRadius * 0.55f, handRadius * 1.4f), androidx.compose.ui.geometry.CornerRadius(handRadius * 0.25f))
            drawRoundRect(skinColor, Offset(handCenter.x + handRadius * 0.1f, handCenter.y - handRadius * 1.8f), Size(handRadius * 0.55f, handRadius * 1.3f), androidx.compose.ui.geometry.CornerRadius(handRadius * 0.25f))
        }
        "thumbs_up" -> {
            val handCenter = Offset(w * 0.80f, h * 0.78f)
            val handRadius = w * 0.10f
            drawCircle(skinColor, radius = handRadius, center = handCenter)
            // Thumb up
            drawRoundRect(skinColor, Offset(handCenter.x - handRadius * 0.4f, handCenter.y - handRadius * 1.6f), Size(handRadius * 0.65f, handRadius * 1.2f), androidx.compose.ui.geometry.CornerRadius(handRadius * 0.25f))
        }
        "wave" -> {
            val handCenter = Offset(w * 0.82f, h * 0.55f)
            val handRadius = w * 0.10f
            drawCircle(skinColor, radius = handRadius, center = handCenter)
            // 4 fingers spread
            drawRoundRect(skinColor, Offset(handCenter.x - handRadius * 0.5f, handCenter.y - handRadius * 1.5f), Size(handRadius * 0.9f, handRadius * 1.1f), androidx.compose.ui.geometry.CornerRadius(handRadius * 0.3f))
        }
    }
}
