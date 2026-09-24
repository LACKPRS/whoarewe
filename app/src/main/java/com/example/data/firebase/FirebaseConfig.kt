package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

object FirebaseConfig {
    private const val TAG = "FirebaseConfig"

    /**
     * Checks if Firebase is initialized and ready with non-placeholder configuration.
     */
    fun isFirebaseReady(context: Context): Boolean {
        return try {
            val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            val options = app.options
            val apiKey = options.apiKey
            val projectId = options.projectId
            val hasValidKey = !apiKey.isNullOrBlank() && !apiKey.contains("PLACEHOLDER", ignoreCase = true)
            val hasValidProject = !projectId.isNullOrBlank() && !projectId.contains("placeholder", ignoreCase = true)
            hasValidKey && hasValidProject
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization check: ${e.message}")
            false
        }
    }

    fun getConfigurationStatus(context: Context): String {
        return try {
            val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            val projectId = app.options.projectId
            if (isFirebaseReady(context)) {
                "Connected to Cloud Firebase ($projectId)"
            } else {
                "Firebase Placeholder Configured (Standby Mode: Ready for your google-services.json)"
            }
        } catch (e: Exception) {
            "Firebase Standby Mode (Awaiting google-services.json keys)"
        }
    }
}
