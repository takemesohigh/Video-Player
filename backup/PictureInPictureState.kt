package dev.anilbeesetti.nextplayer.feature.player.state

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.graphics.Rect
import androidx.media3.common.Player
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * TEMPORARY STUB for the minSdk-23 downgrade test.
 *
 * The real implementation (feature branch / v0.18.0) uses androidx.core.pip, which
 * declares minSdk 24 in its own manifest and blocks builds targeting API 23. Since
 * Picture-in-Picture itself requires API 26+ regardless of this library, this stub
 * simply reports PiP as unsupported everywhere and no-ops every call, so the rest of
 * the player (which already treats PiP as optional) is unaffected.
 *
 * If this base is kept long-term and PiP support is wanted on API 26+ devices, replace
 * this with a direct android.app.PictureInPictureParams implementation instead of
 * restoring the core-pip dependency.
 */
@Composable
fun rememberPictureInPictureState(
    player: Player,
    autoEnter: Boolean = true,
): PictureInPictureState {
    val activity = LocalActivity.current
    val pictureInPictureState = remember {
        PictureInPictureState(activity as ComponentActivity)
    }
    DisposableEffect(activity) { pictureInPictureState.handleListeners(this) }
    LaunchedEffect(player) { pictureInPictureState.observe() }
    return pictureInPictureState
}

@Stable
class PictureInPictureState(
    private val activity: ComponentActivity,
) {
    val isPipSupported: Boolean = false

    val hasPipPermission: Boolean = true

    var isInPictureInPictureMode: Boolean by mutableStateOf(false)
        private set

    fun setVideoViewRect(rect: Rect) {
        // no-op: PiP unsupported in this stub
    }

    fun enterPictureInPictureMode(): Boolean = false

    fun openPictureInPictureSettings() {
        // no-op: PiP unsupported in this stub
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult =
        with(disposableEffectScope) { onDispose { } }

    suspend fun observe() {
        // no-op: PiP unsupported in this stub
    }
}