package com.example.data.repository

import com.example.data.firebase.FirestoreService
import com.example.data.local.LocalNsdHelper
import com.example.model.Friend
import com.example.model.FriendRequest
import com.example.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FriendsRepository(
    private val firestoreService: FirestoreService,
    private val nsdHelper: LocalNsdHelper
) {
    fun getFriendsWithLocalStatus(userId: String): Flow<List<Friend>> {
        val friendsFlow = firestoreService.getFriendsFlow(userId)
        val peersFlow = nsdHelper.discoveredPeers

        return combine(friendsFlow, peersFlow) { friends, peers ->
            friends.map { friend ->
                val matchingPeer = peers.find {
                    it.username.equals(friend.username, ignoreCase = true)
                }
                if (matchingPeer != null) {
                    friend.copy(
                        isOnline = true,
                        isLocalWifiPeer = true,
                        localIp = matchingPeer.hostAddress,
                        localPort = matchingPeer.port,
                        localVoipPort = matchingPeer.voipPort
                    )
                } else {
                    friend.copy(isLocalWifiPeer = false)
                }
            }
        }
    }

    fun getPendingRequests(userId: String): Flow<List<FriendRequest>> {
        return firestoreService.getPendingRequestsFlow(userId)
    }

    suspend fun searchUsers(query: String): List<User> {
        return firestoreService.searchUserByUsername(query)
    }

    suspend fun sendFriendRequest(fromUser: User, toUsername: String): Result<String> {
        return firestoreService.sendFriendRequest(fromUser, toUsername)
    }

    suspend fun respondToRequest(request: FriendRequest, accept: Boolean): Result<Unit> {
        return firestoreService.respondToFriendRequest(request, accept)
    }
}
