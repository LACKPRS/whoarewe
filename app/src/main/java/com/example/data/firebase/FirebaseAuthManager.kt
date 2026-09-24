package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.CustomCredential
import com.example.model.User
import com.example.model.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(private val context: Context) {
    private val TAG = "FirebaseAuthManager"
    private val prefs = context.getSharedPreferences("textflow_auth_session", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseApp.initializeApp(context)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not initialized: ${e.message}")
            null
        }
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Automatically restore session on app launch
        val restoredUser = loadUserSession()
        if (restoredUser != null) {
            _currentUser.value = restoredUser
            _authState.value = AuthState.Success(restoredUser)
            Log.d(TAG, "Session restored successfully for: ${restoredUser.email}")
        }
    }

    private fun saveUserSession(user: User) {
        try {
            prefs.edit()
                .putString("saved_uid", user.uid)
                .putString("saved_email", user.email)
                .putString("saved_username", user.username)
                .putString("saved_display_name", user.displayName)
                .putString("saved_status_text", user.statusText)
                .putString("saved_photo_url", user.photoUrl)
                .putString("saved_role", user.role.name)
                .putLong("saved_created_at", user.createdAt)
                .putBoolean("is_logged_in", true)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user session: ${e.message}")
        }
    }

    private fun loadUserSession(): User? {
        try {
            // First check Firebase Auth currentUser if initialized
            val fUser = auth?.currentUser
            if (fUser != null && !fUser.email.isNullOrBlank()) {
                val fEmail = fUser.email!!.trim().lowercase()
                val assignedRole = User.roleForEmail(fEmail)
                val username = fEmail.substringBefore("@").replace(Regex("[^a-z0-9_]"), "")
                val displayName = fUser.displayName?.ifBlank { null }
                    ?: prefs.getString("saved_display_name", null)
                    ?: fEmail.substringBefore("@")
                val user = User(
                    uid = fUser.uid,
                    email = fEmail,
                    username = username,
                    displayName = displayName,
                    statusText = prefs.getString("saved_status_text", if (assignedRole == UserRole.ADMIN) "System Administrator" else "Active on TextFlow")
                        ?: "Active on TextFlow",
                    role = assignedRole,
                    photoUrl = fUser.photoUrl?.toString() ?: prefs.getString("saved_photo_url", "") ?: "",
                    isOnline = true
                )
                saveUserSession(user)
                return user
            }

            // Fallback: Check local SharedPreferences session
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val uid = prefs.getString("saved_uid", null)
            val email = prefs.getString("saved_email", null)
            if (isLoggedIn && !uid.isNullOrBlank() && !email.isNullOrBlank()) {
                val normalizedEmail = email.trim().lowercase()
                val roleName = prefs.getString("saved_role", UserRole.STANDARD.name) ?: UserRole.STANDARD.name
                val role = try {
                    UserRole.valueOf(roleName)
                } catch (_: Exception) {
                    User.roleForEmail(normalizedEmail)
                }
                return User(
                    uid = uid,
                    email = normalizedEmail,
                    username = prefs.getString("saved_username", normalizedEmail.substringBefore("@")) ?: normalizedEmail.substringBefore("@"),
                    displayName = prefs.getString("saved_display_name", normalizedEmail.substringBefore("@")) ?: normalizedEmail.substringBefore("@"),
                    statusText = prefs.getString("saved_status_text", "Active on TextFlow") ?: "Active on TextFlow",
                    photoUrl = prefs.getString("saved_photo_url", "") ?: "",
                    role = role,
                    isOnline = true,
                    createdAt = prefs.getLong("saved_created_at", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved user session: ${e.message}")
        }
        return null
    }

    private fun clearUserSession() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user session: ${e.message}")
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<User> {
        _authState.value = AuthState.Loading
        val normalizedEmail = email.trim().lowercase()
        val assignedRole = User.roleForEmail(normalizedEmail)

        return try {
            if (auth != null && FirebaseConfig.isFirebaseReady(context)) {
                val authResult = auth?.signInWithEmailAndPassword(normalizedEmail, pass)?.await()
                val firebaseUser = authResult?.user
                if (firebaseUser != null) {
                    val user = User(
                        uid = firebaseUser.uid,
                        email = normalizedEmail,
                        username = normalizedEmail.substringBefore("@").replace(Regex("[^a-z0-9_]"), ""),
                        displayName = firebaseUser.displayName ?: normalizedEmail.substringBefore("@"),
                        statusText = if (assignedRole == UserRole.ADMIN) "System Administrator" else "Online on TextFlow",
                        role = assignedRole,
                        isOnline = true
                    )
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                    saveUserSession(user)
                    return Result.success(user)
                }
            }

            // Local fallback sign-in
            val fallbackUid = "uid_${normalizedEmail.hashCode()}"
            val username = normalizedEmail.substringBefore("@").replace(Regex("[^a-z0-9_]"), "")
                .ifBlank { "user_${System.currentTimeMillis() % 1000}" }
            val displayName = normalizedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            val resolvedUser = User(
                uid = fallbackUid,
                email = normalizedEmail,
                username = username,
                displayName = displayName,
                statusText = if (assignedRole == UserRole.ADMIN) "System Administrator" else "Online on TextFlow",
                role = assignedRole,
                isOnline = true
            )
            _currentUser.value = resolvedUser
            _authState.value = AuthState.Success(resolvedUser)
            saveUserSession(resolvedUser)
            Result.success(resolvedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        username: String,
        displayName: String,
        statusText: String
    ): Result<User> {
        _authState.value = AuthState.Loading
        val normalizedEmail = email.trim().lowercase()
        val assignedRole = User.roleForEmail(normalizedEmail)

        return try {
            var uid = "uid_${normalizedEmail.hashCode()}"
            if (auth != null && FirebaseConfig.isFirebaseReady(context)) {
                val authResult = auth?.createUserWithEmailAndPassword(normalizedEmail, pass)?.await()
                val firebaseUser = authResult?.user
                if (firebaseUser != null) {
                    uid = firebaseUser.uid
                }
            }

            val newUser = User(
                uid = uid,
                email = normalizedEmail,
                username = username.lowercase().trim().replace(Regex("[^a-z0-9_]"), ""),
                displayName = displayName.trim().ifBlank { username },
                statusText = statusText.trim().ifBlank {
                    if (assignedRole == UserRole.ADMIN) "System Administrator" else "Active on TextFlow"
                },
                role = assignedRole,
                isOnline = true,
                createdAt = System.currentTimeMillis()
            )
            _currentUser.value = newUser
            _authState.value = AuthState.Success(newUser)
            saveUserSession(newUser)
            Result.success(newUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}", e)
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(webClientId: String = "placeholder-client-id.apps.googleusercontent.com"): Result<User> {
        _authState.value = AuthState.Loading
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId.ifBlank { "placeholder-client-id.apps.googleusercontent.com" })
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context = context, request = request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id.trim().lowercase()
                val assignedRole = User.roleForEmail(email)

                if (auth != null && FirebaseConfig.isFirebaseReady(context)) {
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth?.signInWithCredential(firebaseCred)?.await()
                    val fUser = authResult?.user
                    if (fUser != null) {
                        val fEmail = (fUser.email ?: email).trim().lowercase()
                        val user = User(
                            uid = fUser.uid,
                            email = fEmail,
                            username = fEmail.substringBefore("@").replace(Regex("[^a-z0-9_]"), ""),
                            displayName = fUser.displayName ?: fEmail.substringBefore("@"),
                            statusText = if (User.isDesignatedAdmin(fEmail)) "System Administrator" else "Connected via Google",
                            role = User.roleForEmail(fEmail),
                            photoUrl = fUser.photoUrl?.toString() ?: "",
                            isOnline = true
                        )
                        _currentUser.value = user
                        _authState.value = AuthState.Success(user)
                        saveUserSession(user)
                        return Result.success(user)
                    }
                }

                val user = User(
                    uid = "google_${googleIdTokenCredential.id.hashCode()}",
                    email = email,
                    username = email.substringBefore("@").replace(Regex("[^a-z0-9_]"), ""),
                    displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@"),
                    statusText = if (assignedRole == UserRole.ADMIN) "System Administrator" else "Connected via Google Sign-In",
                    role = assignedRole,
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                    isOnline = true
                )
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
                saveUserSession(user)
                return Result.success(user)
            }

            _authState.value = AuthState.Error("Google Sign-In canceled or unsupported credential type")
            Result.failure(Exception("Google Sign-In canceled"))
        } catch (e: Exception) {
            Log.w(TAG, "Google sign in error: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Google Sign-In error")
            Result.failure(e)
        }
    }

    fun updateProfile(displayName: String, statusText: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName.ifBlank { current.displayName },
            statusText = statusText.ifBlank { current.statusText }
        )
        _currentUser.value = updated
        saveUserSession(updated)
    }

    fun updateProfilePhoto(photoUrl: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(photoUrl = photoUrl)
        _currentUser.value = updated
        saveUserSession(updated)
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Sign out error: ${e.message}")
        }
        clearUserSession()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
