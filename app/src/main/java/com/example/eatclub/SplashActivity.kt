package com.example.eatclub
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val TEXT_APPEAR_DELAY = 1500L // Delay in milliseconds (e.g., 1.5 seconds)
    private val SPLASH_DISPLAY_LENGTH = 3500L // 3.5 seconds total


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appNameTextView: TextView = findViewById(R.id.eatclub)

        // Option 1: Using Handler for a simple delay
        Handler(Looper.getMainLooper()).postDelayed({
            appNameTextView.animate()
                .alpha(1f) // Fade in to fully visible
                .setDuration(500) // Animation duration
                .start()
        }, TEXT_APPEAR_DELAY)


        Handler(Looper.getMainLooper()).postDelayed({
            // Check if a user is already logged in
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User is signed in, go to MainActivity
                val intent = Intent(this@SplashActivity, HomeActivity::class.java)
                startActivity(intent)
            } else {
                // No user signed in, go to LoginActivity
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
            }
            finish()
        }, SPLASH_DISPLAY_LENGTH)



    }
}

