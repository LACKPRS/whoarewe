package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FirebaseStorageManager(private val context: Context) {
    private val TAG = "FirebaseStorage"

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // First, copy image bytes into local cache file for offline resilience & immediate display
            val localAvatarFile = File(context.filesDir, "avatar_${userId}.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(localAvatarFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Check if Firebase is available and configured
            val isFirebaseReady = FirebaseConfig.isFirebaseReady(context)
            if (isFirebaseReady) {
                try {
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.reference.child("profile_pictures/${userId}.jpg")
                    val uploadTask = storageRef.putFile(imageUri).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    Log.d(TAG, "Uploaded profile picture to Firebase Storage: $downloadUrl")
                    return@withContext Result.success(downloadUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase Storage upload failed: ${e.message}, falling back to local cached picture")
                }
            }

            // Fallback to local stored file URI so picture is fully functional in development/preview
            val localUri = Uri.fromFile(localAvatarFile).toString()
            Log.d(TAG, "Saved profile picture locally: $localUri")
            Result.success(localUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }
}
