import com.android.build.gradle.tasks.registerDataBindingOutputs

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //firebase
    id("com.google.gms.google-services")
    //KSP
    alias(libs.plugins.googleDevtoolsKsp)
}

android {
    namespace = "com.example.eatclub"
    compileSdk = 36

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.eatclub"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.hbb20:ccp:2.7.0")
    implementation("androidx.appcompat:appcompat:1.7.0")


    //animation dependencies
    implementation ("com.airbnb.android:lottie:3.5.0")

    //firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth") // For Java users
    implementation("com.google.firebase:firebase-auth-ktx") // For Kotlin users (provides Kotlin extensions like Firebase.auth)
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Google Sign-In (Needed for Google Sign-In specific UI and client)
    implementation("com.google.android.gms:play-services-auth:21.2.0") // Check for the latest version

    // Credential Manager (recommended by Firebase for modern sign-in flows)
    implementation("androidx.credentials:credentials:1.3.0-alpha01")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha01")
    implementation("com.google.firebase:firebase-firestore")
    // Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    // AndroidX & Material
    implementation("androidx.core:core-ktx:1.13.1") // Or latest
    implementation("androidx.appcompat:appcompat:1.7.0") // Or latest
    implementation("com.google.android.material:material:1.12.0") // Or latest
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Or latest

    //Room Database
    implementation (libs.androidx.room.runtime)
    annotationProcessor ("androidx.room:room-compiler:2.6.1")

    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation("androidx.room:room-testing:2.6.1")

    //glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Retrofit for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // For JSON parsing

    // OkHttp Logging Interceptor (optional, but helpful for debugging)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")


}