package com.example.model

enum class UserRole(val label: String, val level: Int) {
    STANDARD("User", 1),
    MODERATOR("Mod", 2),
    ADMIN("Admin", 3);

    fun canModerate(): Boolean = this == MODERATOR || this == ADMIN
    fun canAdmin(): Boolean = this == ADMIN
}

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val statusText: String = "Active on TextFlow",
    val photoUrl: String = "",
    val role: UserRole = UserRole.STANDARD,
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val isBanned: Boolean = false,
    val warningCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val snapScore: Int = 1420,
    val snapStreaks: Int = 7,
    val zodiacSign: String = "Aries ♈",
    val bitmojiSkin: String = "light",
    val bitmojiHair: String = "fade",
    val bitmojiHairColor: String = "black",
    val bitmojiOutfit: String = "snap_hoodie",
    val bitmojiOutfitColor: String = "yellow",
    val bitmojiMood: String = "smile",
    val bitmojiAccessory: String = "none",
    val bitmojiBackground: String = "sunset",
    val bitmojiPose: String = "peace",
    val hasCustomBitmoji: Boolean = true
) {
    companion object {
        const val EXCLUSIVE_ADMIN_EMAIL = "peterparkerm4178@gmail.com"

        fun isDesignatedAdmin(checkEmail: String?): Boolean {
            return checkEmail?.trim()?.equals(EXCLUSIVE_ADMIN_EMAIL, ignoreCase = true) == true
        }

        fun roleForEmail(checkEmail: String?): UserRole {
            return if (isDesignatedAdmin(checkEmail)) UserRole.ADMIN else UserRole.STANDARD
        }
    }
}
