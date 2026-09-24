package com.example.data.local

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.DeliveryMode
import com.example.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class LocalChatSocketManager(
    var onMessageReceived: ((ChatMessage) -> Unit)? = null
) {
    private val TAG = "LocalChatSocket"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    var localPort: Int = 50550
        private set

    fun startServer(): Int {
        if (serverJob != null && serverSocket?.isClosed == false) {
            return localPort
        }

        try {
            // Try preferred port 50550, or let system assign ephemeral port
            serverSocket = try {
                ServerSocket(50550)
            } catch (e: Exception) {
                ServerSocket(0)
            }
            localPort = serverSocket?.localPort ?: 50550
            Log.d(TAG, "Local chat ServerSocket listening on port $localPort")

            serverJob = scope.launch {
                while (isActive) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch { handleIncomingClient(socket) }
                    } catch (e: Exception) {
                        if (!isActive) break
                        Log.w(TAG, "ServerSocket accept exception: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalChat server: ${e.message}")
        }
        return localPort
    }

    private suspend fun handleIncomingClient(socket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                socket.soTimeout = 10000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine()
                if (!line.isNullOrBlank()) {
                    val json = JSONObject(line)
                    val message = ChatMessage(
                        id = json.optString("id", "loc_${System.currentTimeMillis()}"),
                        chatId = json.optString("chatId", ""),
                        senderId = json.optString("senderId", ""),
                        senderUsername = json.optString("senderUsername", ""),
                        senderDisplayName = json.optString("senderDisplayName", ""),
                        recipientId = json.optString("recipientId", ""),
                        text = json.optString("text", ""),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        deliveryMode = DeliveryMode.LOCAL_ROUTER_P2P,
                        status = MessageStatus.DELIVERED
                    )
                    withContext(Dispatchers.Main) {
                        onMessageReceived?.invoke(message)
                    }

                    // Send ACK
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println("OK")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Incoming client handle error: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun sendMessageToPeer(peerHost: String, peerPort: Int, message: ChatMessage): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket(peerHost, peerPort)
                socket.soTimeout = 5000

                val json = JSONObject().apply {
                    put("id", message.id)
                    put("chatId", message.chatId)
                    put("senderId", message.senderId)
                    put("senderUsername", message.senderUsername)
                    put("senderDisplayName", message.senderDisplayName)
                    put("recipientId", message.recipientId)
                    put("text", message.text)
                    put("timestamp", message.timestamp)
                }

                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println(json.toString())

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val ack = reader.readLine()
                socket.close()

                if (ack == "OK") {
                    Result.success(Unit)
                } else {
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending P2P message to $peerHost:$peerPort: ${e.message}")
                Result.failure(e)
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
