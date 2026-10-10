package com.example.app_reto2

import android.app.Application
import com.cloudinary.android.MediaManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank()) {
            MediaManager.init(
                this,
                mapOf(
                    "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                    "secure" to "true"
                )
            )
        }
    }
}