package com.example.model

enum class ReportStatus {
    PENDING,
    RESOLVED,
    DISMISSED
}

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reporterUsername: String = "",
    val reportedUserId: String = "",
    val reportedUsername: String = "",
    val reason: String = "",
    val messageExcerpt: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: ReportStatus = ReportStatus.PENDING,
    val resolutionNote: String = ""
)
