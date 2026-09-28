package com.skywright.game

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class SkywrightView(context: Context) : View(context), Choreographer.FrameCallback {
    private val prefs = context.getSharedPreferences("skywright", Context.MODE_PRIVATE)
    private val core = GameCore().apply { best = prefs.getInt("best", 0) }
    private val renderer = Renderer()
    private val audio = GameAudio()
    private var soundOn = prefs.getBoolean("sound", true)
    private var vibrationOn = prefs.getBoolean("vibration", true)
    private var ambient = 0f
    private var lastFrameNanos = 0L
    private var accumulator = 0f
    private var framePosted = false
    private var tapX = 180f
    private var tapY = 360f

    init { isFocusable = true }

    private fun scheduleFrame() {
        if (isAttachedToWindow && windowVisibility == VISIBLE && !framePosted) {
            framePosted = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleFrame()
    }

    override fun onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this)
        framePosted = false
        audio.release()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            lastFrameNanos = 0L
            scheduleFrame()
        } else {
            Choreographer.getInstance().removeFrameCallback(this)
            framePosted = false
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        framePosted = false
        val dt = if (lastFrameNanos == 0L) 0f else ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastFrameNanos = frameTimeNanos
        ambient += dt
        accumulator += dt
        var steps = 0
        while (accumulator >= 1f / 60f && steps < 6) {
            core.update(1f / 60f)
            accumulator -= 1f / 60f
            steps++
        }
        if (steps == 6) accumulator = 0f
        drainEvents()
        invalidate()
        scheduleFrame()
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas, core, ambient, soundOn, vibrationOn)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        val scale = minOf(width / GameCore.WIDTH, height / GameCore.HEIGHT)
        tapX = (event.x - (width - GameCore.WIDTH * scale) / 2f) / scale
        tapY = (event.y - (height - GameCore.HEIGHT * scale) / 2f) / scale
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        val x = tapX
        val y = tapY

        if (core.phase != GamePhase.PLAYING && y in 626f..665f) {
            when {
                x in 43f..169f -> {
                    soundOn = !soundOn
                    prefs.edit().putBoolean("sound", soundOn).apply()
                    invalidate()
                    return true
                }
                x in 191f..317f -> {
                    vibrationOn = !vibrationOn
                    prefs.edit().putBoolean("vibration", vibrationOn).apply()
                    invalidate()
                    return true
                }
            }
        }

        when (core.phase) {
            GamePhase.READY -> core.start()
            GamePhase.PLAYING -> if (x > 292f && y < GameCore.TOP) core.pause() else core.flap()
            GamePhase.PAUSED -> core.resume()
            GamePhase.OVER -> core.restart()
        }
        drainEvents()
        invalidate()
        return true
    }

    fun pauseGame() {
        core.pause()
        accumulator = 0f
        lastFrameNanos = 0L
        invalidate()
    }

    private fun drainEvents() {
        if (core.events.isEmpty()) return
        core.events.forEach { event ->
            audio.play(event, soundOn)
            if (vibrationOn && (event == GameEvent.FLAP || event == GameEvent.HIT)) {
                performHapticFeedback(if (event == GameEvent.HIT) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP)
            }
            if (event == GameEvent.SCORE) prefs.edit().putInt("best", core.best).apply()
        }
        core.events.clear()
    }
}
