package com.example.eatclub // Replace with your actual package name

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.auth.GoogleAuthProvider // No longer needed here
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Email/Password UI
    private lateinit var etLoginEmail: EditText
    private lateinit var etLoginPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegisterLink: TextView

    // Google Sign-In
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var btnGoogleSignIn: Button

    private val TAG = "LoginActivity"

    private val oneTapSignInResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        Log.d(TAG, "Got ID token from Google. Starting LoadingActivity for Google Auth.")
                        // IMMEDIATELY START LOADING ACTIVITY, PASSING THE TOKEN
                        val intent = Intent(this, LoadingActivity::class.java).apply {
                            putExtra("ACTION_TYPE", "GOOGLE_SIGN_IN")
                            putExtra("GOOGLE_ID_TOKEN", idToken)
                        }
                        startActivity(intent)
                        finish() // Finish LoginActivity after handing off to LoadingActivity
                    } else {
                        Log.e(TAG, "No Google ID token!")
                        Toast.makeText(this, "Google Sign-In failed: No ID token", Toast.LENGTH_LONG).show()
                        // Optionally, stop any local visual loading indicator if you added one
                    }
                } catch (e: ApiException) {
                    Log.e(TAG, "Google Sign-In failed after intent", e)
                    Toast.makeText(this, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    // Optionally, stop any local visual loading indicator
                }
            } else {
                Log.d(TAG, "Google Sign-In Canceled or Failed. Result code: ${result.resultCode}")
                if (result.resultCode != Activity.RESULT_CANCELED) { // Don't toast on user cancel
                    Toast.makeText(this, "Google Sign-In process was not completed.", Toast.LENGTH_SHORT).show()
                }
                // Optionally, stop any local visual loading indicator
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        // Email/Password Views
        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        // Google Sign-In Views
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        // Configure Google Sign-In (One Tap)
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Ensure you have R.string.default_web_client_id correctly set in your strings.xml
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false) // Show all accounts
                    .build()
            )
            .setAutoSelectEnabled(false) // Can set to true for auto sign-in attempt
            .build()

        // Email/Password Login Listener
        btnLogin.setOnClickListener {
            initiateEmailPasswordLogin()
        }

        // Google Sign-In Button Listener
        btnGoogleSignIn.setOnClickListener {
            initiateGoogleSignInPopup()
        }

        // Register Link Listener
        tvRegisterLink.setOnClickListener {
            // For registration, RegisterActivity should collect info and then start LoadingActivity
            // This example assumes RegisterActivity will handle that transition.
            val intent = Intent(this, RegisterActivity::class.java)
            // You might pass a flag if RegisterActivity needs to know it's not a Google sign-in completion
            intent.putExtra("IS_GOOGLE_SIGN_IN_COMPLETION", false)
            startActivity(intent)
            // Do not finish LoginActivity here if users can go back from RegisterActivity to Login
        }
    }

    private fun initiateGoogleSignInPopup() {
        // You could show a very minimal local loading state here if desired, e.g., disable button
        // btnGoogleSignIn.isEnabled = false
        Log.d(TAG, "Initiating Google Sign-In popup.")
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapSignInResultLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    // btnGoogleSignIn.isEnabled = true // Re-enable button on error
                    Log.e(TAG, "Couldn't start One Tap UI for Google Sign-In: ${e.localizedMessage}")
                    Toast.makeText(this, "Google Sign-In error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener(this) { e ->
                // btnGoogleSignIn.isEnabled = true // Re-enable button on error
                Log.w(TAG, "Google Sign-In failed to begin: ${e.localizedMessage}", e)
                Toast.makeText(this, "Google Sign-In may not be available. Check connection or Google Play services.", Toast.LENGTH_LONG).show()
            }
    }

    private fun initiateEmailPasswordLogin() {
        val email = etLoginEmail.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()

        if (email.isEmpty()) {
            etLoginEmail.error = "Email is required"
            etLoginEmail.requestFocus()
            return
        }
        // Basic email validation (optional but recommended)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.error = "Enter a valid email address"
            etLoginEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etLoginPassword.error = "Password is required"
            etLoginPassword.requestFocus()
            return
        }
        // Basic password length validation (optional)
        if (password.length < 6) {
            etLoginPassword.error = "Password must be at least 6 characters"
            etLoginPassword.requestFocus()
            return
        }

        Log.d(TAG, "Email/Password provided. Starting LoadingActivity for Email/Pass Auth.")
        // IMMEDIATELY START LOADING ACTIVITY, PASSING CREDENTIALS
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("ACTION_TYPE", "EMAIL_PASSWORD_SIGN_IN")
            putExtra("USER_EMAIL", email)
            putExtra("USER_PASSWORD", password)
        }
        startActivity(intent)
        finish() // Finish LoginActivity after handing off to LoadingActivity
    }

    // The actual Firebase authentication methods (firebaseAuthWithGoogle, performEmailPasswordLogin)
    // have been MOVED to LoadingActivity.kt

    // Navigation methods like navigateToMainActivity() and navigateToLoadingScreen()
    // are no longer needed here as LoadingActivity controls post-auth navigation.

    override fun onStart() {
        super.onStart()
        // This check determines if the user is already logged in when LoginActivity starts.
        // If so, it bypasses the login form and goes to LoadingActivity,
        // which will then proceed to fetch data or go to MainActivity.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User ${currentUser.uid} already signed in. Starting LoadingActivity.")
            val intent = Intent(this, LoadingActivity::class.java).apply {
                putExtra("ACTION_TYPE", "EXISTING_USER_CHECK")
            }
            startActivity(intent)
            finish() // Finish LoginActivity as we are moving to LoadingActivity
        } else {
            Log.d(TAG, "No user signed in. Staying on LoginActivity to show login form.")
        }
    }
}
