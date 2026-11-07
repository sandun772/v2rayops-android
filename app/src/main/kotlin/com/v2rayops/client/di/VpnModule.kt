package com.v2rayops.client.di

import com.v2rayops.client.vpn.VpnServiceController
import com.v2rayops.client.vpn.VpnServiceControllerImpl
import com.v2rayops.client.xray.XrayEngine
import com.v2rayops.client.xray.XrayEngineStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VpnModule {

    @Binds
    @Singleton
    abstract fun bindXrayEngine(impl: XrayEngineStub): XrayEngine

    @Binds
    @Singleton
    abstract fun bindVpnServiceController(impl: VpnServiceControllerImpl): VpnServiceController
}
