package com.xandy.lite.models.media.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val loadControl = "Xandy.LoadControl"

@Serializable
@SerialName(loadControl)
sealed class LoadControl {
    companion object {
        private const val DEFAULT_MIN_BUFFER = 50_000
        private const val DEFAULT_MAX_BUFFER = 60_000
        private const val DEFAULT_BUFFER_FOR_PB = 1_000
        private const val DEFAULT_BUFFER_FOR_PB_AFTER_REBUFFER = 2_000
    }
    @Serializable
    @SerialName("$loadControl.Default")
    data object Default : LoadControl()

    @Serializable
    @SerialName("$loadControl.LowLatency")
    data object LowLatency : LoadControl()

    @Serializable
    @SerialName("$loadControl.Balanced")
    data object Balanced : LoadControl()

    @Serializable
    @SerialName("$loadControl.HighStability")
    data object HighStability : LoadControl()

    @Serializable
    @SerialName("$loadControl.Custom")
    data class Custom(
        val minBuffer: Int = DEFAULT_MIN_BUFFER, val maxBuffer: Int = DEFAULT_MAX_BUFFER,
        val bufferForPlayback: Int = DEFAULT_BUFFER_FOR_PB,
        val bufferForRebuffer: Int = DEFAULT_BUFFER_FOR_PB_AFTER_REBUFFER
    ) : LoadControl()

    /** Minimum buffer in milliseconds */
    internal fun minBuffer() = when (this) {
        is Balanced -> 30_000
        is Custom -> this.minBuffer
        is Default -> 50_000
        is HighStability -> 60_000
        is LowLatency -> 15_000
    }

    /** Maximum buffer in milliseconds */
    internal fun maxBuffer() = when (this) {
        is Balanced -> 60_000
        is Custom -> this.maxBuffer
        is Default -> 60_000
        is HighStability -> 100_000
        is LowLatency -> 25_000
    }

    /** Buffer for playback in milliseconds */
    internal fun bufferForPlayback() = when (this) {
        is Balanced -> 1_500
        is Custom -> this.bufferForPlayback
        is Default -> 1_000
        is HighStability -> 3_000
        is LowLatency -> 500
    }

    /** Buffer for Playback after rebuffer in milliseconds */
    internal fun bufferForPlaybackAfterRebuffer() = when (this) {
        is Balanced -> 3_000
        is Custom -> this.bufferForRebuffer
        is Default -> 2_000
        is HighStability -> 5_000
        is LowLatency -> 1_000
    }
}