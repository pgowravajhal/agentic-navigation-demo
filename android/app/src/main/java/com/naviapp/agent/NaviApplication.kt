package com.naviapp.agent

import android.app.Application
import android.util.Log
import com.here.sdk.core.engine.SDKNativeEngine
import com.here.sdk.core.engine.SDKOptions
import com.here.sdk.core.errors.InstantiationErrorException

class NaviApplication : Application() {

    companion object {
        private const val TAG = "NaviApp"
        var hereSdkInitialized: Boolean = false
            private set
        var hereSdkError: String? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        initializeHereSdk()
    }

    private fun initializeHereSdk() {
        try {
            // SDKOptions with empty string uses credentials from AndroidManifest meta-data
            val options = SDKOptions("", "")
            SDKNativeEngine.makeSharedInstance(this, options)
            hereSdkInitialized = true
            Log.i(TAG, "HERE SDK initialized successfully")
        } catch (e: InstantiationErrorException) {
            hereSdkInitialized = false
            hereSdkError = "HERE SDK init failed: ${e.error.name}"
            Log.w(TAG, "HERE SDK initialization failed: ${e.error.name}. Map will show placeholder.")
        } catch (e: Exception) {
            hereSdkInitialized = false
            hereSdkError = "HERE SDK error: ${e.message}"
            Log.w(TAG, "HERE SDK error: ${e.message}. Map will show placeholder.")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            SDKNativeEngine.getSharedInstance()?.dispose()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
