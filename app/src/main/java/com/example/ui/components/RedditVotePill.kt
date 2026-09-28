package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedditDownvoteBlue
import com.example.ui.theme.RedditDownvoteTint
import com.example.ui.theme.RedditOrange
import com.example.ui.theme.RedditOrangeTint

/**
 * Reddit-style Upvote / Downvote Vote Pill
 *
 * @param score Current net score (upvotes - downvotes)
 * @param isUpvoted Whether the current user has upvoted this item
 * @param isDownvoted Whether the current user has downvoted this item
 * @param onUpvote Callback when user taps upvote
 * @param onDownvote Callback when user taps downvote
 * @param messageId Unique message ID for test tags
 * @param isDarkBubble Whether this pill is rendered on a dark bubble background
 */
@Composable
fun RedditVotePill(
    score: Int,
    isUpvoted: Boolean,
    isDownvoted: Boolean,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    messageId: String,
    modifier: Modifier = Modifier,
    isDarkBubble: Boolean = false
) {
    val pillBgColor by animateColorAsState(
        targetValue = when {
            isUpvoted -> RedditOrangeTint
            isDownvoted -> RedditDownvoteTint
            isDarkBubble -> Color.Black.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        },
        animationSpec = spring(),
        label = "pill_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isUpvoted -> RedditOrange.copy(alpha = 0.6f)
            isDownvoted -> RedditDownvoteBlue.copy(alpha = 0.6f)
            isDarkBubble -> Color.White.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        },
        animationSpec = spring(),
        label = "pill_border"
    )

    val scoreTextColor by animateColorAsState(
        targetValue = when {
            isUpvoted -> RedditOrange
            isDownvoted -> RedditDownvoteBlue
            isDarkBubble -> Color.White.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(),
        label = "pill_score_color"
    )

    val upvoteTint by animateColorAsState(
        targetValue = if (isUpvoted) RedditOrange else if (isDarkBubble) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        label = "upvote_tint"
    )

    val downvoteTint by animateColorAsState(
        targetValue = if (isDownvoted) RedditDownvoteBlue else if (isDarkBubble) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        label = "downvote_tint"
    )

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(pillBgColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("reddit_pill_$messageId"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Upvote Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 14.dp),
                    onClick = onUpvote
                )
                .padding(3.dp)
                .testTag("vote_up_$messageId"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Upvote",
                tint = upvoteTint,
                modifier = Modifier.size(13.dp)
            )
        }

        // Karma Score Text
        Text(
            text = when {
                score > 0 -> "+$score"
                score < 0 -> "$score"
                else -> "0"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isUpvoted || isDownvoted) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = scoreTextColor
            ),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .testTag("vote_score_$messageId")
        )

        // Downvote Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 14.dp),
                    onClick = onDownvote
                )
                .padding(3.dp)
                .testTag("vote_down_$messageId"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Downvote",
                tint = downvoteTint,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
