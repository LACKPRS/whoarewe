package com.example.data.repository

import com.example.data.firebase.FirestoreService
import com.example.model.Report
import com.example.model.ReportStatus
import com.example.model.User
import com.example.model.UserRole
import kotlinx.coroutines.flow.Flow

class ModerationRepository(private val firestoreService: FirestoreService) {

    fun getAllUsers(): Flow<List<User>> = firestoreService.getAllUsersFlow()

    fun getAllReports(): Flow<List<Report>> = firestoreService.getReportsFlow()

    suspend fun submitReport(
        reporter: User,
        targetUser: User,
        reason: String,
        messageExcerpt: String = ""
    ): Result<Unit> {
        val report = Report(
            id = "rep_${System.currentTimeMillis()}",
            reporterId = reporter.uid,
            reporterUsername = reporter.username,
            reportedUserId = targetUser.uid,
            reportedUsername = targetUser.username,
            reason = reason,
            messageExcerpt = messageExcerpt,
            timestamp = System.currentTimeMillis(),
            status = ReportStatus.PENDING
        )
        return firestoreService.submitReport(report)
    }

    suspend fun resolveReport(reportId: String, status: ReportStatus, note: String): Result<Unit> {
        return firestoreService.resolveReport(reportId, status, note)
    }

    suspend fun updateUserRole(targetUid: String, newRole: UserRole): Result<Unit> {
        return firestoreService.updateUserRole(targetUid, newRole)
    }

    suspend fun warnUser(targetUid: String): Result<Unit> {
        return firestoreService.warnUser(targetUid)
    }

    suspend fun toggleUserBan(targetUid: String): Result<Boolean> {
        return firestoreService.toggleUserBan(targetUid)
    }
}
