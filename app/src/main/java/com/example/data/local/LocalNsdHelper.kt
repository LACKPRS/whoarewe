package com.example.data.local

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.LocalPeer
import com.example.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class LocalNsdHelper(private val context: Context) {
    private val TAG = "LocalNsdHelper"
    val SERVICE_TYPE = "_textflow._tcp."

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredPeers = MutableStateFlow<List<LocalPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<LocalPeer>> = _discoveredPeers.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var scope = CoroutineScope(Dispatchers.IO + Job())

    fun startAdvertising(currentUser: User, chatPort: Int, voipPort: Int) {
        if (_isAdvertising.value || nsdManager == null) return

        try {
            acquireMulticastLock()

            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "TextFlow_${currentUser.username}"
                serviceType = SERVICE_TYPE
                port = chatPort

                // Set TXT records for user identity & VoIP audio port
                setAttribute("username", currentUser.username)
                setAttribute("displayName", currentUser.displayName)
                setAttribute("voipPort", voipPort.toString())
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service registered: ${NsdServiceInfo.serviceName}")
                    _isAdvertising.value = true
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD Registration failed: $errorCode")
                    _isAdvertising.value = false
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service unregistered")
                    _isAdvertising.value = false
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "NSD Unregistration failed: $errorCode")
                }
            }

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NSD registration: ${e.message}")
        }
    }

    fun startDiscovery(currentUsername: String) {
        if (_isDiscovering.value || nsdManager == null) return

        try {
            acquireMulticastLock()

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d(TAG, "NSD Discovery started for $regType")
                    _isDiscovering.value = true
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service found: ${service.serviceName}")
                    if (service.serviceType.contains("textflow") && !service.serviceName.contains(currentUsername)) {
                        resolveService(service)
                    }
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    Log.d(TAG, "NSD Service lost: ${service.serviceName}")
                    _discoveredPeers.value = _discoveredPeers.value.filter { it.serviceName != service.serviceName }
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(TAG, "NSD Discovery stopped")
                    _isDiscovering.value = false
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "NSD Discovery start failed: $errorCode")
                    _isDiscovering.value = false
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e(TAG, "NSD Discovery stop failed: $errorCode")
                }
            }

            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery: ${e.message}")
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service resolved: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}")
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port

                val username = serviceInfo.attributes["username"]?.let { String(it, StandardCharsets.UTF_8) }
                    ?: serviceInfo.serviceName.substringAfter("TextFlow_")
                val displayName = serviceInfo.attributes["displayName"]?.let { String(it, StandardCharsets.UTF_8) }
                    ?: username
                val voipPortStr = serviceInfo.attributes["voipPort"]?.let { String(it, StandardCharsets.UTF_8) }
                val voipPort = voipPortStr?.toIntOrNull() ?: 50555

                val peer = LocalPeer(
                    serviceName = serviceInfo.serviceName,
                    username = username,
                    displayName = displayName,
                    hostAddress = host,
                    port = port,
                    voipPort = voipPort
                )

                scope.launch {
                    val currentList = _discoveredPeers.value.filter { it.username != username }
                    _discoveredPeers.value = currentList + peer
                }
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.w(TAG, "Exception resolving service: ${e.message}")
        }
    }

    fun addSimulatedLocalPeer(peer: LocalPeer) {
        _discoveredPeers.value = _discoveredPeers.value.filter { it.username != peer.username } + peer
    }

    fun removeSimulatedPeer(username: String) {
        _discoveredPeers.value = _discoveredPeers.value.filter { it.username != username }
    }

    fun stopAdvertising() {
        if (_isAdvertising.value && nsdManager != null && registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering service: ${e.message}")
            }
            _isAdvertising.value = false
            registrationListener = null
        }
        releaseMulticastLock()
    }

    fun stopDiscovery() {
        if (_isDiscovering.value && nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping discovery: ${e.message}")
            }
            _isDiscovering.value = false
            discoveryListener = null
        }
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        if (multicastLock == null && wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("TextFlowMulticastLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        }
    }

    fun simulateDiscoveredPeer(peer: LocalPeer) {
        if (_discoveredPeers.value.none { it.username.equals(peer.username, ignoreCase = true) }) {
            _discoveredPeers.value = _discoveredPeers.value + peer
        }
    }

    private fun releaseMulticastLock() {
        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
        }
        multicastLock = null
    }
}
