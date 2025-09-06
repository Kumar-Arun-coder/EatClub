package com.example.eatclub.network

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "eatclub_app_prefs" // Changed name slightly for clarity
    private const val FIREBASE_TOKEN = "firebase_id_token" // More specific key name

    // Helper function to get SharedPreferences instance
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveFirebaseToken(context: Context, token: String?) {
        val editor = getPreferences(context).edit()
        editor.putString(FIREBASE_TOKEN, token)
        editor.apply() // Use apply() for asynchronous saving
    }

    fun getFirebaseToken(context: Context): String? {
        return getPreferences(context).getString(FIREBASE_TOKEN, null)
    }

    fun clearFirebaseToken(context: Context) { // Added a clear function
        val editor = getPreferences(context).edit()
        editor.remove(FIREBASE_TOKEN)
        editor.apply()
    }
}

