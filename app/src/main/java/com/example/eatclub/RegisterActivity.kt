package com.example.eatclub // Replace with your actual package name

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val firestore by lazy { Firebase.firestore }

    private lateinit var tvRegisterTitle: TextView
    private lateinit var etRegisterEmail: EditText
    private lateinit var etRegisterPassword: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnRegisterOrSave: Button
    private lateinit var tvLoginLinkFromRegister: TextView

    private var isGoogleSignInCompletion = false
    private var prefilledEmail: String? = null
    private var prefilledDisplayName: String? = null

    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        isGoogleSignInCompletion = intent.getBooleanExtra("IS_GOOGLE_SIGN_IN_COMPLETION", false)
        prefilledEmail = intent.getStringExtra("USER_EMAIL")
        prefilledDisplayName = intent.getStringExtra("USER_DISPLAY_NAME")

        tvRegisterTitle = findViewById(R.id.tvRegisterTitle)
        etRegisterEmail = findViewById(R.id.etRegisterEmail)
        etRegisterPassword = findViewById(R.id.etRegisterPassword)
        etUsername = findViewById(R.id.etUsername)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnRegisterOrSave = findViewById(R.id.btnRegisterOrSave)
        tvLoginLinkFromRegister = findViewById(R.id.tvLoginLinkFromRegister)

        setupUIForFlow()

        btnRegisterOrSave.setOnClickListener {
            if (isGoogleSignInCompletion) {
                // This is for completing the profile AFTER Google sign-in has already created the user
                saveUserProfileAfterGoogleSignIn()
            } else {
                // This is for new Email/Password registration
                initiateEmailPasswordRegistration()
            }
        }

        tvLoginLinkFromRegister.setOnClickListener {
            // Navigate to LoginActivity if user wants to log in instead of registering
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Clear RegisterActivity from back stack
            startActivity(intent)
            finish()
        }
    }

    private fun setupUIForFlow() {
        if (isGoogleSignInCompletion) {
            tvRegisterTitle.text = getString(R.string.title_activity_register_complete_profile) // Make sure this string exists
            etRegisterEmail.setText(prefilledEmail)
            etRegisterEmail.isEnabled = false
            etRegisterPassword.visibility = View.GONE

            etUsername.visibility = View.VISIBLE
            etPhoneNumber.visibility = View.VISIBLE
            if (!prefilledDisplayName.isNullOrEmpty() && !prefilledDisplayName!!.contains(" ")) {
                etUsername.setText(prefilledDisplayName)
            }
            btnRegisterOrSave.text = getString(R.string.save_profile_button_text) // Make sure this string exists
            tvLoginLinkFromRegister.visibility = View.GONE
        } else {
            tvRegisterTitle.text = getString(R.string.title_activity_register_email_password) // Make sure this string exists
            etRegisterEmail.isEnabled = true
            etRegisterPassword.visibility = View.VISIBLE
            // For standard registration, you decide if username/phone are collected here or later
            // For simplicity, let's assume they are collected if visible.
            // If you want to collect them for email/pass users on this screen, make them VISIBLE.
            // Otherwise, keep them GONE and collect them after initial registration if needed.
            etUsername.visibility = View.VISIBLE // Or View.GONE if not collecting username during initial email/pass reg
            etPhoneNumber.visibility = View.VISIBLE // Or View.GONE
            btnRegisterOrSave.text = getString(R.string.register_button_text) // Make sure this string exists
            tvLoginLinkFromRegister.visibility = View.VISIBLE
        }
    }

    private fun initiateEmailPasswordRegistration() {
        val email = etRegisterEmail.text.toString().trim()
        val password = etRegisterPassword.text.toString().trim()
        val username = etUsername.text.toString().trim() // Collect if visible
        val phoneNumber = etPhoneNumber.text.toString().trim() // Collect if visible

        // --- Validation ---
        if (email.isEmpty()) {
            etRegisterEmail.error = "Email is required"; etRegisterEmail.requestFocus(); return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.error = "Enter a valid email"; etRegisterEmail.requestFocus(); return
        }
        if (password.isEmpty()) {
            etRegisterPassword.error = "Password is required"; etRegisterPassword.requestFocus(); return
        }
        if (password.length < 6) {
            etRegisterPassword.error = "Password must be at least 6 characters"; etRegisterPassword.requestFocus(); return
        }
        // Add validation for username and phone number if they are mandatory for email/pass registration
        if (etUsername.visibility == View.VISIBLE && username.isEmpty()) {
            etUsername.error = "Username is required"; etUsername.requestFocus(); return
        }
        // Optional: Phone number validation

        // --- Start LoadingActivity to handle actual Firebase registration ---
        Log.d(TAG, "Registration details collected. Starting LoadingActivity for Email/Pass Registration.")
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("ACTION_TYPE", "REGISTER_USER_EMAIL_PASSWORD") // New specific action type
            putExtra("USER_EMAIL", email)
            putExtra("USER_PASSWORD", password)
            // Pass username and phone number if collected, LoadingActivity will need them
            // to save to Firestore after user creation.
            if (etUsername.visibility == View.VISIBLE) putExtra("USER_USERNAME", username)
            if (etPhoneNumber.visibility == View.VISIBLE) putExtra("USER_PHONE_NUMBER", phoneNumber)
        }
        startActivity(intent)
        finish() // Finish RegisterActivity after handing off
    }

    private fun saveUserProfileAfterGoogleSignIn() {
        // This function is called when RegisterActivity is used to complete a profile
        // for a user ALREADY created via Google Sign-In (by LoadingActivity).
        // So, auth.currentUser should exist.

        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "Not logged in. Cannot save profile.", Toast.LENGTH_SHORT).show()
            // This case should ideally not happen if the flow is correct.
            // If it does, navigate back to Login.
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
            return
        }

        val username = etUsername.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Username is required"; etUsername.requestFocus(); return
        }
        // Add more validation for phone number if needed

        // Show some loading indicator locally if this operation takes time
        // (e.g., disable button, show progress bar)
        btnRegisterOrSave.isEnabled = false
        // You could also use a ProgressBar here

        // Update Firebase Auth Profile (Display Name)
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        firebaseUser.updateProfile(profileUpdates)
            .addOnCompleteListener { profileUpdateTask ->
                if (profileUpdateTask.isSuccessful) {
                    Log.d(TAG, "User profile display name updated in Firebase Auth.")
                } else {
                    Log.w(TAG, "Failed to update display name in Firebase Auth.", profileUpdateTask.exception)
                    // Still proceed to save to Firestore, display name update is secondary here
                }

                // Save/Update additional data in Firestore
                // Note: UID and Email are from firebaseUser (already authenticated)
                val userProfileData = hashMapOf(
                    // "uid" and "email" will be derived from firebaseUser in Firestore write
                    // Or, if your LoadingActivity's Firestore write for Google users is robust,
                    // this might just be an update.
                    "displayName" to username, // User's chosen display name
                    "username" to username,    // Storing it explicitly as 'username'
                    "phoneNumber" to phoneNumber
                    // "provider" and "createdAt" should have been set when the user was created via Google in LoadingActivity
                )

                // We are MERGING data. This is important if LoadingActivity already created a document
                // for this Google user with initial details (like email, uid, provider, createdAt).
                firestore.collection("users").document(firebaseUser.uid)
                    .set(userProfileData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "User profile updated in Firestore for UID: ${firebaseUser.uid}")
                        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                        navigateToLoadingScreenOrMain() // Proceed to Loading/Main
                    }
                    .addOnFailureListener { e ->
                        btnRegisterOrSave.isEnabled = true // Re-enable button
                        Log.w(TAG, "Error updating user profile in Firestore", e)
                        Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun navigateToLoadingScreenOrMain() {
        // After profile completion for a Google user, you might want to go to LoadingActivity
        // to ensure any final data fetches happen, or directly to MainActivity.
        // Let's go to LoadingActivity for consistency.
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("ACTION_TYPE", "POST_PROFILE_COMPLETION") // New action type for LoadingActivity
            // No need to pass user details, LoadingActivity will use auth.currentUser
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity() // Clear the registration/login stack
    }


}

