package com.example

import com.example.model.ChatMessage
import com.example.model.GroupMessage
import com.example.model.User
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun redditChatMessage_scoreCalculation() {
    val message = ChatMessage(
      id = "msg1",
      text = "Hello Kaiser",
      upvotedBy = listOf("userA", "userB", "userC"),
      downvotedBy = listOf("userD")
    )
    // Score should be 3 - 1 = 2
    assertEquals(2, message.score)
  }

  @Test
  fun redditGroupMessage_scoreCalculation() {
    val grpMessage = GroupMessage(
      id = "gmsg1",
      text = "Community announcement",
      upvotedBy = listOf("user1"),
      downvotedBy = listOf("user2", "user3")
    )
    // Score should be 1 - 2 = -1
    assertEquals(-1, grpMessage.score)
  }

  @Test
  fun snapchatCustomProfile_attributes() {
    val customUser = User(
      uid = "snap_user_1",
      username = "cool_avatar",
      displayName = "Cool Avatar",
      snapScore = 2500,
      snapStreaks = 14,
      zodiacSign = "Leo ♌",
      bitmojiSkin = "caramel",
      bitmojiHair = "waves",
      bitmojiHairColor = "blonde",
      bitmojiOutfit = "snap_hoodie",
      bitmojiOutfitColor = "yellow",
      bitmojiMood = "cool",
      bitmojiAccessory = "sunglasses",
      bitmojiBackground = "sunset",
      bitmojiPose = "peace",
      hasCustomBitmoji = true
    )

    assertEquals(2500, customUser.snapScore)
    assertEquals(14, customUser.snapStreaks)
    assertEquals("Leo ♌", customUser.zodiacSign)
    assertEquals("caramel", customUser.bitmojiSkin)
    assertEquals("sunglasses", customUser.bitmojiAccessory)
    assertEquals("sunset", customUser.bitmojiBackground)
    assertTrue(customUser.hasCustomBitmoji)
  }
}

