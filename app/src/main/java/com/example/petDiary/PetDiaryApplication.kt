package com.example.petDiary

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import com.yandex.mapkit.MapKitFactory

class PetDiaryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initYandexMapKit()
    }

    private fun initYandexMapKit() {
        try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = ai.metaData?.getString("com.yandex.maps.ApiKey")
            if (!apiKey.isNullOrBlank()) {
                MapKitFactory.setApiKey(apiKey)
                MapKitFactory.initialize(this)
            } else {
                Log.w(TAG, "Yandex MapKit API key not found in manifest")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Yandex MapKit initialization failed", e)
        }
    }

    companion object {
        private const val TAG = "PetDiaryApp"
    }
}
