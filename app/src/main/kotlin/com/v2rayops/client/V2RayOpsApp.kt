package com.v2rayops.client

import android.app.Application
import com.v2rayops.client.autoconnect.AutoConnectManager
import com.v2rayops.client.core.logger.AppLogger
import com.v2rayops.client.xray.XrayEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class V2RayOpsApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var xrayEngine: XrayEngine

    @Inject
    lateinit var autoConnectManager: AutoConnectManager

    override fun onCreate() {
        super.onCreate()
        AppLogger.i("V2RayOpsApp", "Initializing V2RayOPS Application Foundation")
        xrayEngine.initialize(this)
        autoConnectManager.startObserving(applicationScope)
    }
}
