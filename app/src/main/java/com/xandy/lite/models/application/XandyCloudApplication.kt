package com.xandy.lite.models.application

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class XandyCloudApplication : Application() {
    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(SupervisorJob())
    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(applicationScope, this)
    }
}

const val XANDY_CLOUD = "Xandy-Cloud"

data class AppValues(
    val unknownTrackUri: Uri, val unknownArtist: String, val unknown: String
)

data class AppStrings(val unknownArtist: String, val unknown: String)

fun AppValues.toStrings() = AppStrings(unknownArtist = unknownArtist, unknown = unknown)

fun StateFlow<AppValues>.toStrings(scope: CoroutineScope) = this.map { it.toStrings() }
    .stateIn(
        scope = scope, started = SharingStarted.Eagerly,
        initialValue = value.toStrings()
    )