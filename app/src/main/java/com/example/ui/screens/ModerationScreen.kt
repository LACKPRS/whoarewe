package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Report
import com.example.model.ReportStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentSky

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    currentUser: User,
    allUsers: List<User>,
    reports: List<Report>,
    onUpdateRole: (targetUid: String, newRole: UserRole) -> Unit,
    onWarnUser: (targetUid: String) -> Unit,
    onToggleBanUser: (targetUid: String) -> Unit,
    onResolveReport: (reportId: String, status: ReportStatus, note: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
            Spacer(modifier = Modifier.height(10.dp))

            // User Identity & Privilege Notice
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Acting as ${currentUser.displayName} (@${currentUser.username})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentUser.role.canAdmin())
                                "Admin Privileges: Full role reassignment, banning, & report resolution."
                            else
                                "Moderator Privileges: Warning, banning, & report resolution.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    RoleBadge(role = currentUser.role)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Users (${allUsers.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_mod_users")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val pendingCount = reports.count { it.status == ReportStatus.PENDING }
                        Text(
                            text = if (pendingCount > 0) "Reports ($pendingCount)" else "Reports",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("tab_mod_reports")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // User Management Tab
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allUsers) { user ->
                        val isTargetAdmin = User.isDesignatedAdmin(user.email)
                        var showRoleMenu by remember { mutableStateOf(false) }

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .testTag("mod_user_card_${user.username}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = user.displayName, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "@${user.username}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (user.warningCount > 0) {
                                                Text(
                                                    text = "${user.warningCount} Warnings • ",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = AccentAmber,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                            if (user.isBanned) {
                                                Text(
                                                    text = "BANNED",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = AccentRose,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            } else {
                                                Text(
                                                    text = user.statusText,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable(enabled = currentUser.role.canAdmin() && !isTargetAdmin) {
                                                showRoleMenu = true
                                            }
                                        ) {
                                            RoleBadge(role = user.role)
                                        }

                                        if (currentUser.role.canAdmin() && !isTargetAdmin) {
                                            DropdownMenu(
                                                expanded = showRoleMenu,
                                                onDismissRequest = { showRoleMenu = false }
                                            ) {
                                                listOf(UserRole.STANDARD, UserRole.MODERATOR).forEach { roleOption ->
                                                    DropdownMenuItem(
                                                        text = { Text("Make ${roleOption.label}") },
                                                        onClick = {
                                                            onUpdateRole(user.uid, roleOption)
                                                            showRoleMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (!isTargetAdmin) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons: Warn, Ban/Unban
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { onWarnUser(user.uid) },
                                            modifier = Modifier.testTag("warn_user_${user.username}")
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Warn", color = AccentAmber, fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onToggleBanUser(user.uid) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (user.isBanned) AccentEmerald else AccentRose
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.testTag("ban_user_${user.username}")
                                        ) {
                                            Icon(
                                                imageVector = if (user.isBanned) Icons.Default.Check else Icons.Default.Block,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (user.isBanned) "Unban" else "Ban", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Reports Queue Tab
                if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No reports in queue. Moderation status clean.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(reports) { rep ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (rep.status == ReportStatus.PENDING) AccentRose.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .testTag("report_card_${rep.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reported: @${rep.reportedUsername}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (rep.status) {
                                                        ReportStatus.PENDING -> AccentRose.copy(alpha = 0.15f)
                                                        ReportStatus.RESOLVED -> AccentEmerald.copy(alpha = 0.15f)
                                                        ReportStatus.DISMISSED -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = rep.status.name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = when (rep.status) {
                                                        ReportStatus.PENDING -> AccentRose
                                                        ReportStatus.RESOLVED -> AccentEmerald
                                                        ReportStatus.DISMISSED -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Reporter: @${rep.reporterUsername} • Reason: ${rep.reason}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )

                                    if (rep.messageExcerpt.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "\"${rep.messageExcerpt}\"",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    if (rep.status == ReportStatus.PENDING) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { onResolveReport(rep.id, ReportStatus.DISMISSED, "Dismissed as non-violation") },
                                                modifier = Modifier.testTag("dismiss_report_${rep.id}")
                                            ) {
                                                Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = { onResolveReport(rep.id, ReportStatus.RESOLVED, "Resolved: Warning issued") },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                                modifier = Modifier.testTag("resolve_report_${rep.id}")
                                            ) {
                                                Text("Resolve & Warn", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
