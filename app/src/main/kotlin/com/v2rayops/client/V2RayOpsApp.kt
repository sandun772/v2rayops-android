package com.v2rayops.client

import android.app.Application
import com.v2rayops.client.autoconnect.AutoConnectManager
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.xray.XrayEngine
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class V2RayOpsApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var xrayEngine: Lazy<XrayEngine>

    @Inject
    lateinit var autoConnectManager: Lazy<AutoConnectManager>

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("V2RayOpsApp", "Initializing V2RayOPS Application Foundation")
        
        // Fully asynchronous initialization on background thread: leaves Main UI thread 100% free!
        applicationScope.launch(Dispatchers.IO) {
            xrayEngine.get().initialize(this@V2RayOpsApp)
            autoConnectManager.get().startObserving(applicationScope)
            AppLogger.d("V2RayOpsApp", "Background engine and auto-connect initialization complete")
        }
    }
}
