package dev.anilbeesetti.nextplayer.feature.player.popup

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout

/**
 * A small floating window that hosts video playback via [WindowManager], for devices
 * below API 26 (where system Picture-in-Picture isn't available) or where the user
 * prefers a movable window over PiP.
 *
 * Ported from VLC-Android's PopupLayout/PopupManager. Unlike VLC, this attaches a plain
 * [SurfaceView] directly to a Media3 [androidx.media3.common.Player] via
 * `setVideoSurfaceView`, so no video-output wrapper (VLC's IVLCVout) is needed — Media3
 * handles scaling and layout inside the surface on its own.
 *
 * A freshly added [SurfaceView] does not have a live [android.view.Surface] the instant
 * it's attached to a window — the system creates it asynchronously. Callers must wait
 * for [onSurfaceReady] before calling `player.setVideoSurfaceView(surfaceView)`, and
 * detach the player (via [onSurfaceDestroyed]) before the surface is torn down, or the
 * renderer can be handed an invalid surface — this is what caused the intermittent
 * "Can't play video" crash when popup mode was toggled repeatedly.
 *
 * Only ever created programmatically (never inflated from XML), so it does not need
 * the (Context, AttributeSet) constructor Android Studio's design tools look for.
 */
@SuppressLint("ViewConstructor")
class PopupLayout(
    context: Context,
    private val onClose: () -> Unit,
    private val onExpand: () -> Unit,
    private val onSurfaceReady: () -> Unit,
    private val onSurfaceDestroyed: () -> Unit,
) : FrameLayout(context) {

    val surfaceView = SurfaceView(context)

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams: WindowManager.LayoutParams

    private var startX = 0f
    private var startY = 0f
    private var startLayoutX = 0
    private var startLayoutY = 0
    private var dragging = false

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            onSurfaceReady()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // No action needed: Media3 reads size from the surface itself.
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            onSurfaceDestroyed()
        }
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val (maxWidth, _) = currentScreenSize()
                val minWidth = (POPUP_MIN_WIDTH_DP * context.resources.displayMetrics.density).toInt()

                val newWidth = (layoutParams.width * detector.scaleFactor)
                    .toInt()
                    .coerceIn(minWidth, maxWidth)
                val newHeight = (newWidth * VIDEO_ASPECT_RATIO).toInt()

                layoutParams.width = newWidth
                layoutParams.height = newHeight
                windowManager.updateViewLayout(this@PopupLayout, layoutParams)
                return true
            }
        },
    )

    @SuppressLint("ClickableViewAccessibility")
    private val dragTouchListener = OnTouchListener { _, event ->
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                startLayoutX = layoutParams.x
                startLayoutY = layoutParams.y
                dragging = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress) return@OnTouchListener true

                val dx = event.rawX - startX
                val dy = event.rawY - startY
                if (!dragging && (dx * dx + dy * dy) > DRAG_THRESHOLD_PX_SQ) {
                    dragging = true
                }
                if (dragging) {
                    val (screenWidth, screenHeight) = currentScreenSize()
                    layoutParams.x = (startLayoutX + dx.toInt())
                        .coerceIn(0, (screenWidth - layoutParams.width).coerceAtLeast(0))
                    layoutParams.y = (startLayoutY + dy.toInt())
                        .coerceIn(0, (screenHeight - layoutParams.height).coerceAtLeast(0))
                    windowManager.updateViewLayout(this, layoutParams)
                }
                true
            }
            else -> false
        }
    }

    init {
        val density = context.resources.displayMetrics.density
        val (screenWidth, _) = currentScreenSize()
        val defaultWidth = (POPUP_DEFAULT_WIDTH_DP * density).toInt().coerceAtMost(screenWidth)
        val defaultHeight = (defaultWidth * VIDEO_ASPECT_RATIO).toInt()

        // TYPE_APPLICATION_OVERLAY requires API 26; devices below that (down to this
        // module's minSdk 23) need the older TYPE_PHONE overlay type instead.
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            defaultWidth,
            defaultHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - defaultWidth
            y = 0
        }

        surfaceView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        surfaceView.holder.addCallback(surfaceCallback)
        addView(surfaceView)
        addView(buildControlsOverlay(context))

        setOnTouchListener(dragTouchListener)
    }

    private fun buildControlsOverlay(context: Context): View {
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP or Gravity.END
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )
        }

        val expandButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setBackgroundColor(0x00000000)
            setOnClickListener { onExpand() }
        }
        val closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(0x00000000)
            setOnClickListener { onClose() }
        }

        controls.addView(expandButton)
        controls.addView(closeButton)
        return controls
    }

    /**
     * Current, rotation-aware screen size in pixels. Deliberately not
     * `context.resources.displayMetrics`: for an application context that can still
     * describe the previous orientation while a rotation is in progress, which placed
     * the popup off-screen when it was opened as the player returned to portrait.
     */
    private fun currentScreenSize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    /** Pulls the window fully back on-screen after a rotation or a stale first placement. */
    private fun clampToScreen() {
        val (screenWidth, screenHeight) = currentScreenSize()
        // An overlay window not tied to an Activity can briefly report a degenerate
        // size (zero, or implausibly small) right as the window is still settling —
        // trusting that reading shrank the surface to nothing, leaving the popup
        // playing audio with no picture. Skip clamping rather than apply a bad reading.
        if (screenWidth < MIN_PLAUSIBLE_SCREEN_PX || screenHeight < MIN_PLAUSIBLE_SCREEN_PX) {
            return
        }
        layoutParams.width = layoutParams.width.coerceAtMost(screenWidth)
        layoutParams.height = layoutParams.height.coerceAtMost(screenHeight)
        layoutParams.x = layoutParams.x
            .coerceIn(0, (screenWidth - layoutParams.width).coerceAtLeast(0))
        layoutParams.y = layoutParams.y
            .coerceIn(0, (screenHeight - layoutParams.height).coerceAtLeast(0))
        if (isAttachedToWindow) {
            windowManager.updateViewLayout(this, layoutParams)
        }
    }

    private val reclampRunnable = Runnable { clampToScreen() }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // A real rotation is exactly what this needs to react to, and by the time this
        // callback fires the new size is genuine, not a transient reading — unlike the
        // one right after the window is first attached, so only this path re-clamps.
        postDelayed(reclampRunnable, RECLAMP_DELAY_MS)
    }

    fun attachToWindow() {
        windowManager.addView(this, layoutParams)
    }

    /**
     * Removes this window. Note that removing the window is what triggers
     * [SurfaceHolder.Callback.surfaceDestroyed] on [surfaceView] — the caller does not
     * need to detach the player's surface separately beforehand; [onSurfaceDestroyed]
     * fires as a result of this call.
     */
    fun close() {
        removeCallbacks(reclampRunnable)
        runCatching { windowManager.removeView(this) }
    }

    companion object {
        private const val POPUP_DEFAULT_WIDTH_DP = 200
        private const val POPUP_MIN_WIDTH_DP = 120
        private const val VIDEO_ASPECT_RATIO = 9f / 16f
        private const val DRAG_THRESHOLD_PX_SQ = 100
        private const val RECLAMP_DELAY_MS = 500L
        private const val MIN_PLAUSIBLE_SCREEN_PX = 200
    }
}