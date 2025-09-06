package com.example.eatclub.network // Or your chosen package

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// AuthInterceptor is often defined within or alongside RetrofitClient
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = AppPreferences.getFirebaseToken(context.applicationContext) // Use applicationContext
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) { // Check if token is not null or empty
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "Token added to header.")
        } else {
            Log.w("AuthInterceptor", "Firebase token is null or empty. Request sent without token.")
            // Depending on your API, you might want to prevent the request
            // or the server will handle unauthorized access.
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}

object RetrofitClient {

    // For Emulator, 10.0.2.2 usually maps to your computer's localhost
    // For physical device on same Wi-Fi, use your computer's Wi-Fi IP address
    private const val BASE_URL = "http://10.217.110.229:5000/api/" // Ensure /api/ or your base path is correct

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
    }

    // OkHttpClient now depends on Context for AuthInterceptor
    private fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context.applicationContext)) // Pass applicationContext
            .addInterceptor(loggingInterceptor)
            // You can add timeouts here if needed:
            // .connectTimeout(30, TimeUnit.SECONDS)
            // .readTimeout(30, TimeUnit.SECONDS)
            // .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Method to get the FoodApiService instance, requiring Context
    fun getInstance(context: Context): FoodApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(context.applicationContext)) // Use applicationContext
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(FoodApiService::class.java)
    }
}

