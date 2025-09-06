package com.example.eatclub

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eatclub.databinding.ActivityLoadingBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding
    private lateinit var auth: FirebaseAuth
    private val firestore by lazy { Firebase.firestore }
    private val TAG = "LoadingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root) // Your activity_loading.xml should have a ProgressBar

        auth = Firebase.auth

        // UI is already showing "Loading..." from your activity_loading.xml

        val actionType = intent.getStringExtra("ACTION_TYPE")
        Log.d(TAG, "onCreate: Action Type: $actionType")

        // Immediately handle actions based on actionType
        // The Handler for simulating data fetch will be called *after* auth is successful.
        when (actionType) {
            "GOOGLE_SIGN_IN" -> {
                val idToken = intent.getStringExtra("GOOGLE_ID_TOKEN")
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Log.e(TAG, "Google ID Token missing in LoadingActivity.")
                    navigateToLogin("Google Sign-In error: Missing token.")
                }
            }
            "EMAIL_PASSWORD_SIGN_IN" -> {
                val email = intent.getStringExtra("USER_EMAIL")
                val password = intent.getStringExtra("USER_PASSWORD")
                if (email != null && password != null) {
                    performEmailPasswordLogin(email, password)
                } else {
                    Log.e(TAG, "Email/Password missing in LoadingActivity.")
                    navigateToLogin("Login error: Missing credentials.")
                }
            }
            "REGISTER_USER_EMAIL_PASSWORD" -> {
                val email = intent.getStringExtra("USER_EMAIL")
                val password = intent.getStringExtra("USER_PASSWORD")
                val username = intent.getStringExtra("USER_USERNAME") // From RegisterActivity
                val phoneNumber = intent.getStringExtra("USER_PHONE_NUMBER") // From RegisterActivity

                if (email != null && password != null) {
                    performEmailPasswordRegistration(email, password, username, phoneNumber)
                } else {
                    Log.e(TAG, "Registration details missing for Email/Password.")
                    navigateToLogin("Registration error: Missing details.")
                }
            }
            "EXISTING_USER_CHECK" -> {
                // User is already logged in (checked by LoginActivity's onStart or similar)
                Log.d(TAG, "Existing user check. Current user: ${auth.currentUser?.uid}")
                if (auth.currentUser != null) {
                    // Proceed to fetch data or go to MainActivity
                    fetchDataAndProceedToMain(auth.currentUser)
                } else {
                    // This case should ideally be handled by LoginActivity's onStart,
                    // but as a fallback:
                    Log.w(TAG, "EXISTING_USER_CHECK but currentUser is null. Navigating to Login.")
                    navigateToLogin()
                }
            }
            "POST_PROFILE_COMPLETION" -> {
                // Came from RegisterActivity after saving profile details for a Google user
                Log.d(TAG, "Post profile completion. Current user: ${auth.currentUser?.uid}")
                if (auth.currentUser != null) {
                    fetchDataAndProceedToMain(auth.currentUser)
                } else {
                    Log.w(TAG, "POST_PROFILE_COMPLETION but currentUser is null. Navigating to Login.")
                    navigateToLogin("Session error after profile update.")
                }
            }
            else -> {
                Log.e(TAG, "Unknown or no action type specified: $actionType. Checking current user.")
                // Fallback: Check current user, could be from a cold start or unexpected state
                if (auth.currentUser != null) {
                    fetchDataAndProceedToMain(auth.currentUser)
                } else {
                    navigateToLogin("Error: Unknown application state.")
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "firebaseAuthWithGoogle: Attempting sign-in with Google token.")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "firebaseAuthWithGoogle: Success.")
                    val firebaseUser = auth.currentUser
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (firebaseUser != null) {
                        if (isNewUser) {
                            Log.d(TAG, "New user signed in with Google. Saving initial profile and navigating to complete profile.")
                            saveInitialGoogleUserProfile(firebaseUser) { success ->
                                if (success) {
                                    Intent(this, RegisterActivity::class.java).also { intent ->
                                        intent.putExtra("USER_EMAIL", firebaseUser.email)
                                        intent.putExtra("USER_DISPLAY_NAME", firebaseUser.displayName)
                                        intent.putExtra("IS_GOOGLE_SIGN_IN_COMPLETION", true)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish() // Finish LoadingActivity
                                    }
                                } else {
                                    navigateToLogin("Error saving initial Google profile.")
                                }
                            }
                        } else {
                            Log.d(TAG, "Existing user signed in with Google: ${firebaseUser.email}")
                            // Optional: Update last login time or other details in Firestore if needed
                            fetchDataAndProceedToMain(firebaseUser)
                        }
                    } else {
                        Log.e(TAG, "firebaseAuthWithGoogle: Success but firebaseUser is null.")
                        navigateToLogin("Google Sign-In error: User data unavailable.")
                    }
                } else {
                    Log.w(TAG, "firebaseAuthWithGoogle: Failure.", task.exception)
                    navigateToLogin("Google Authentication Failed: ${task.exception?.message}")
                }
            }
    }

    private fun performEmailPasswordLogin(email: String, password: String) {
        Log.d(TAG, "performEmailPasswordLogin: Attempting sign-in for $email.")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail: Success.")
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        fetchDataAndProceedToMain(firebaseUser)
                    } else {
                        Log.e(TAG, "signInWithEmail: Success but firebaseUser is null.")
                        navigateToLogin("Login error: User data unavailable.")
                    }
                } else {
                    Log.w(TAG, "signInWithEmail: Failure.", task.exception)
                    navigateToLogin("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun performEmailPasswordRegistration(
        email: String,
        password: String,
        username: String?,
        phoneNumber: String?
    ) {
        Log.d(TAG, "performEmailPasswordRegistration: Attempting user creation for $email.")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail: Success.")
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Update Firebase Auth profile display name if username is provided
                        val displayNameForAuth = username ?: email.substringBefore('@') // Fallback display name

                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayNameForAuth)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).addOnCompleteListener {
                            if(it.isSuccessful) Log.d(TAG, "Firebase Auth profile updated for new email user.")
                            else Log.w(TAG, "Failed to update Firebase Auth profile for new email user.")
                        }

                        // Save user details to Firestore
                        saveNewEmailPasswordUserToFirestore(firebaseUser, email, username, phoneNumber) { success ->
                            if (success) {
                                fetchDataAndProceedToMain(firebaseUser)
                            } else {
                                // User created in Auth but Firestore failed.
                                // Decide on recovery strategy: maybe proceed to main but log error,
                                // or try to delete Auth user (complex), or ask user to retry profile setup later.
                                // For now, proceeding to main with a warning.
                                Log.e(TAG, "Firestore save failed for new email/pass user, but Auth user created.")
                                Toast.makeText(this, "Registration complete, but profile save issue.", Toast.LENGTH_LONG).show()
                                fetchDataAndProceedToMain(firebaseUser)
                            }
                        }
                    } else {
                        Log.e(TAG, "createUserWithEmail: Success but firebaseUser is null.")
                        navigateToLogin("Registration error: User data unavailable.")
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail: Failure.", task.exception)
                    navigateToLogin("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveInitialGoogleUserProfile(firebaseUser: FirebaseUser, onComplete: (Boolean) -> Unit) {
        val userProfileData = hashMapOf(
            "uid" to firebaseUser.uid,
            "email" to firebaseUser.email,
            "displayName" to firebaseUser.displayName, // From Google
            "username" to (firebaseUser.displayName?.replace(" ", "")?.lowercase()
                ?: firebaseUser.email?.substringBefore('@')
                ?: "default_username"), // Or ?: "" for an empty string
            "provider" to "google.com",
            "createdAt" to Timestamp.now(),
            "photoUrl" to firebaseUser.photoUrl?.toString()
            // Add other initial fields like 'phoneNumber' as empty or from Google if available
        )
        firestore.collection("users").document(firebaseUser.uid)
            .set(userProfileData, SetOptions.merge()) // Merge in case something existed
            .addOnSuccessListener {
                Log.d(TAG, "Initial Google user profile saved to Firestore for UID: ${firebaseUser.uid}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving initial Google user profile to Firestore", e)
                onComplete(false)
            }
    }

    private fun saveNewEmailPasswordUserToFirestore(
        firebaseUser: FirebaseUser,
        email: String,
        username: String?,
        phoneNumber: String?,
        onComplete: (Boolean) -> Unit
    ) {
        val userProfileData = hashMapOf(
            "uid" to firebaseUser.uid,
            "email" to email,
            "displayName" to (username ?: email.substringBefore('@')),
            "username" to (username ?: email.substringBefore('@')),
            "phoneNumber" to (phoneNumber ?: ""),
            "provider" to "password",
            "createdAt" to Timestamp.now()
        )
        firestore.collection("users").document(firebaseUser.uid)
            .set(userProfileData)
            .addOnSuccessListener {
                Log.d(TAG, "New Email/Password user profile saved to Firestore for UID: ${firebaseUser.uid}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving Email/Password user profile to Firestore", e)
                onComplete(false)
            }
    }


    // This is the function that simulates data fetching *after* authentication is successful
    private fun fetchDataAndProceedToMain(user: FirebaseUser?) {
        if (user == null) {
            Log.e(TAG, "fetchDataAndProceedToMain: User is null, cannot proceed.")
            navigateToLogin("Session error.")
            return
        }

        Log.d(TAG, "fetchDataAndProceedToMain: User ${user.uid} authenticated. Simulating data fetch...")
        //binding.tvLoadingStatus.text = "Loading user data..." // Example of updating UI

        // Example: Simulate a network call or data processing
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Data fetch complete. Navigating to MainActivity for user: ${user.uid}")
            // Data fetching/processing complete
            navigateToMain()
        }, 1500) // Simulate 1.5 seconds loading time for data fetch
    }

    private fun navigateToMain() {
        Log.d(TAG, "Navigating to MainActivity.")
        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show() // Generic welcome
        Intent(this, HomeActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(it)
            finish() // Finish LoadingActivity
        }
    }

    private fun navigateToLogin(errorMessage: String? = null) {
        Log.d(TAG, "Navigating to LoginActivity. Error: $errorMessage")
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
        Intent(this, LoginActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(it)
            finish() // Finish LoadingActivity
        }
    }
}
