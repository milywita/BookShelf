// BookApplication.kt
package com.example.bookapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class BookApplication : Application(){
    companion object{
        private const val TAG = "BookApplication"
        private const val FIRESTORE_CACHE_SIZE = 104857600L //100MB

        @Volatile
        private lateinit var instance: BookApplication
            private set

        fun getInstance(): BookApplication {
            check(::instance.isInitialized){
                "BookApplication not initialized. Ensure you're not calling this before onCreate()"
            }
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            initializeApplication()
            Log.i(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize application", e)
            // In a production app, you might want to add crash reporting here
            throw RuntimeException("Critical initialization failed", e)
        }
    }

    private fun initializeApplication() {
        instance = this
        initializeFirebase()
        setupFirestore()
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
            throw e
        }
    }

    private fun setupFirestore() {
        try {
            val settings = createFirestoreSettings()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d(TAG, "Firestore configured with cache size: $FIRESTORE_CACHE_SIZE bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore configuration failed", e)
            throw e
        }
    }

    private fun createFirestoreSettings(): FirebaseFirestoreSettings {
        return FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FIRESTORE_CACHE_SIZE)
                    .build()
            )
            .build()
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "Application terminated")
    }
}