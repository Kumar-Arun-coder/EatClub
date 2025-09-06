package com.example.eatclub

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
// For debug builds, you might use:
// import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MyEatClubApplication : Application() {

    override fun onCreate() {
        super.onCreate() // Always call the superclass's onCreate()

        FirebaseApp.initializeApp(this)


        val firebaseAppCheck = FirebaseAppCheck.getInstance()


        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

    }
}
