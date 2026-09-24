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
    val createdAt: Long = System.currentTimeMillis()
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
