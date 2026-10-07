package dev.anilbeesetti.nextplayer.feature.player.popup

import android.content.Context
import androidx.media3.common.Player

/**
 * Owns the single popup-player window, if one is open. There is never more than one at a
 * time.
 *
 * This runs inside PlayerService and talks to the service's real player directly. An
 * earlier version had the popup connect its own MediaController instead, which routes the
 * video surface to the real player through the session connection; the popup window then
 * opened with playback running but no picture. VLC and NewPipe both host their popup
 * player in the playback service rather than behind a remote controller, and so does this.
 *
 * [show] stops the player before attaching the popup's surface, and [hide] stops it again
 * before detaching, so a running decoder is never handed a new surface. The caller's normal
 * resume path (PlayerActivity.startPlayback) restarts playback afterward. Live surface
 * swaps on a running decoder are unreliable on some devices and are deliberately avoided.
 */
object PopupWindowController {

    private var popupLayout: PopupLayout? = null
    private var hostedPlayer: Player? = null
    private var surfaceGuard: Player.Listener? = null

    val isShowing: Boolean
        get() = popupLayout != null

    fun show(context: Context, player: Player, onExpandRequested: () -> Unit) {
        if (popupLayout != null) return

        player.stop()

        var playbackStarted = false
        lateinit var layout: PopupLayout
        layout = PopupLayout(
            context = context.applicationContext,
            onClose = {
                player.pause()
                hide()
            },
            onExpand = {
                onExpandRequested()
                hide()
            },
            onSurfaceReady = {
                if (popupLayout === layout) {
                    player.setVideoSurfaceView(layout.surfaceView)
                    // Start playback only once the surface is attached. The surface can be
                    // recreated later (for example on resize); only the first callback
                    // starts playback.
                    if (!playbackStarted) {
                        playbackStarted = true
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            },
            onSurfaceDestroyed = {
                player.clearVideoSurfaceView(layout.surfaceView)
            },
        )

        // Defensive: a surface-clear arriving from elsewhere (for example the player
        // activity's own controller being torn down) would leave the popup with playback
        // but no picture. If the popup's surface is still valid when that happens,
        // attach it again.
        val guard = object : Player.Listener {
            override fun onSurfaceSizeChanged(width: Int, height: Int) {
                if (width == 0 && height == 0 &&
                    popupLayout === layout &&
                    layout.surfaceView.holder.surface.isValid
                ) {
                    player.setVideoSurfaceView(layout.surfaceView)
                }
            }
        }

        popupLayout = layout
        hostedPlayer = player
        surfaceGuard = guard
        player.addListener(guard)
        layout.attachToWindow()
    }

    fun hide() {
        val layout = popupLayout ?: return
        val player = hostedPlayer
        val guard = surfaceGuard

        popupLayout = null
        hostedPlayer = null
        surfaceGuard = null

        if (player != null) {
            if (guard != null) player.removeListener(guard)
            player.stop()
            player.clearVideoSurfaceView(layout.surfaceView)
        }
        layout.close()
    }
}