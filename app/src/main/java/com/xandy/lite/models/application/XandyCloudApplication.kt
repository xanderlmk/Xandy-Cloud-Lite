package com.xandy.lite.models.application

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class XandyCloudApplication: Application() {
    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(SupervisorJob())
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(applicationScope, this.applicationContext)
    }
}
const val XANDY_CLOUD = "Xandy-Cloud"