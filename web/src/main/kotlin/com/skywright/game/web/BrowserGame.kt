package com.skywright.game.web

import com.skywright.game.GameCore
import com.skywright.game.GameEvent
import com.skywright.game.GamePhase
import com.skywright.game.ScreenLayout
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.math.roundToInt

private const val TIME_STEP = 1.0 / 60
private const val MAX_FRAME_STEPS = 6
private val pageIsHidden: Boolean get() = document.asDynamic().hidden == true

fun main() {
    val canvas = element<HTMLCanvasElement>("sky")
    val context = canvas.getContext("2d") as? CanvasRenderingContext2D
    if (context == null) {
        element<HTMLElement>("flight-help").textContent = "Skywright needs a browser with Canvas 2D support."
        return
    }
    BrowserGame(canvas, BrowserRenderer(context))

    val workers = window.navigator.asDynamic().serviceWorker
    if (workers != null) workers.register("./service-worker.js").catch { _: dynamic -> Unit }
}

private class BrowserGame(
    private val canvas: HTMLCanvasElement,
    private val renderer: BrowserRenderer,
) {
    private val root = element<HTMLElement>("game")
    private val topInset = element<HTMLElement>("safe-inset")
    private val action = element<HTMLButtonElement>("action")
    private val pause = element<HTMLButtonElement>("pause")
    private val sound = element<HTMLButtonElement>("sound")
    private val vibration = element<HTMLButtonElement>("vibration")
    private val heading = element<HTMLElement>("title")
    private val eyebrow = element<HTMLElement>("eyebrow")
    private val introMark = element<HTMLElement>("intro-mark")
    private val help = element<HTMLElement>("flight-help")
    private val panel = element<HTMLElement>("panel")
    private val results = element<HTMLElement>("results")
    private val score = element<HTMLElement>("score")
    private val scoreCaption = element<HTMLElement>("score-caption")
    private val details = element<HTMLElement>("details")
    private val canVibrate = jsTypeOf(window.navigator.asDynamic().vibrate) == "function"

    private val core = GameCore().apply { best = readSetting("best")?.toIntOrNull()?.coerceAtLeast(0) ?: 0 }
    private val audio = BrowserAudio()
    private var soundOn = readSetting("sound") != "false"
    private var vibrationOn = readSetting("vibration") != "false"
    private var savedBest = core.best
    private var displayedPhase: GamePhase? = null
    private var activePointer: Int? = null
    private var spaceHeld = false
    private var pressStartedAt = 0.0
    private var ambient = 0f
    private var lastFrame: Double? = null
    private var accumulator = 0.0
    private var width = 1
    private var height = 1
    private var layout = ScreenLayout.forSize(width, height)

    init {
        action.disabled = false
        sound.disabled = false
        vibration.hidden = !canVibrate
        updateSettingButton(sound, soundOn)
        updateSettingButton(vibration, vibrationOn)

        action.addEventListener("click", { runAction() })
        pause.addEventListener("click", { pauseRun() })
        sound.addEventListener("click", {
            soundOn = !soundOn
            if (soundOn) audio.unlock()
            updateSettingButton(sound, soundOn)
            writeSetting("sound", soundOn.toString())
        })
        vibration.addEventListener("click", {
            vibrationOn = !vibrationOn
            updateSettingButton(vibration, vibrationOn)
            writeSetting("vibration", vibrationOn.toString())
        })
        canvas.addEventListener("pointerdown", { onPointerDown(it as PointerEvent) })
        canvas.addEventListener("pointerup", { onPointerUp(it as PointerEvent) })
        canvas.addEventListener("pointercancel", { onPointerCancel(it as PointerEvent) })
        canvas.addEventListener("lostpointercapture", { onPointerCancel(it as PointerEvent) })
        canvas.addEventListener("contextmenu", { it.preventDefault() })
        window.addEventListener("keydown", { onKeyDown(it as KeyboardEvent) })
        window.addEventListener("keyup", { onKeyUp(it as KeyboardEvent) })
        window.addEventListener("resize", { resize() })
        window.addEventListener("blur", { pauseRun() })
        window.addEventListener("pagehide", { pauseRun() })
        document.addEventListener("visibilitychange", {
            if (pageIsHidden) pauseRun()
        })

        resize()
        syncInterface()
        scheduleFrame()
    }

    private fun resize() {
        width = canvas.clientWidth.coerceAtLeast(1)
        height = canvas.clientHeight.coerceAtLeast(1)
        val density = window.devicePixelRatio.coerceIn(1.0, 3.0)
        val pixelWidth = (width * density).roundToInt()
        val pixelHeight = (height * density).roundToInt()
        if (canvas.width != pixelWidth) canvas.width = pixelWidth
        if (canvas.height != pixelHeight) canvas.height = pixelHeight
        val safeTop = window.getComputedStyle(topInset).paddingTop.removeSuffix("px").toDoubleOrNull()?.roundToInt() ?: 0
        layout = ScreenLayout.forSize(width, height, safeTop)
        pause.style.left = "${layout.offsetX + 321.5f * layout.scale}px"
        val pauseY = layout.offsetY + (29f + layout.hudShift) * layout.scale
        pause.style.top = "${maxOf(pauseY, safeTop + 22f)}px"
    }

    private fun scheduleFrame() {
        window.requestAnimationFrame { now ->
            val elapsed = lastFrame?.let { ((now - it) / 1000).coerceIn(0.0, 0.1) } ?: 0.0
            lastFrame = now
            if (!pageIsHidden) {
                ambient += elapsed.toFloat()
                if (core.phase == GamePhase.PLAYING) {
                    accumulator += elapsed
                    var steps = 0
                    while (accumulator >= TIME_STEP && steps < MAX_FRAME_STEPS) {
                        core.update(TIME_STEP.toFloat())
                        accumulator -= TIME_STEP
                        steps++
                    }
                    if (steps == MAX_FRAME_STEPS) accumulator = 0.0
                }
                drainEvents()
                syncInterface()
                renderer.draw(core, ambient, layout, width, height)
            }
            scheduleFrame()
        }
    }

    private fun onPointerDown(event: PointerEvent) {
        if (!event.isPrimary || event.button.toInt() != 0 || activePointer != null || spaceHeld) return
        event.preventDefault()
        if (core.phase == GamePhase.PAUSED) {
            runAction()
            return
        }
        activePointer = event.pointerId
        canvas.setPointerCapture(event.pointerId)
        beginPress(event.timeStamp.toDouble())
    }

    private fun onPointerUp(event: PointerEvent) {
        if (event.pointerId != activePointer) return
        releasePointer()
        endPress(event.timeStamp.toDouble())
    }

    private fun onPointerCancel(event: PointerEvent) {
        if (event.pointerId == activePointer) cancelInput()
    }

    private fun releasePointer() {
        val pointer = activePointer
        activePointer = null
        if (pointer != null && canvas.hasPointerCapture(pointer)) canvas.releasePointerCapture(pointer)
    }

    private fun onKeyDown(event: KeyboardEvent) {
        if (event.key == "Escape" && !event.repeat) {
            when (core.phase) {
                GamePhase.PLAYING -> pauseRun()
                GamePhase.PAUSED -> runAction()
                else -> return
            }
            event.preventDefault()
            return
        }
        if (event.code != "Space" || event.target is HTMLButtonElement || event.altKey || event.ctrlKey || event.metaKey) return
        event.preventDefault()
        if (event.repeat || activePointer != null || spaceHeld) return
        if (core.phase == GamePhase.PAUSED) {
            runAction()
            return
        }
        spaceHeld = true
        beginPress(event.timeStamp.toDouble())
    }

    private fun onKeyUp(event: KeyboardEvent) {
        if (event.code != "Space" || !spaceHeld) return
        event.preventDefault()
        spaceHeld = false
        endPress(event.timeStamp.toDouble())
    }

    private fun beginPress(timestamp: Double) {
        if (soundOn) audio.unlock()
        if (core.phase != GamePhase.PLAYING) resetClock()
        pressStartedAt = timestamp
        core.beginPress()
        canvas.focus()
        drainEvents()
        syncInterface()
    }

    private fun endPress(timestamp: Double) {
        core.endPress(((timestamp - pressStartedAt) / 1000).coerceAtLeast(0.0).toFloat())
        drainEvents()
    }

    private fun cancelInput() {
        core.cancelPress()
        spaceHeld = false
        releasePointer()
    }

    private fun runAction() {
        if (soundOn) audio.unlock()
        when (core.phase) {
            GamePhase.READY -> core.start()
            GamePhase.PAUSED -> core.resume()
            GamePhase.OVER, GamePhase.WON -> core.restart()
            GamePhase.PLAYING -> return
        }
        resetClock()
        canvas.focus()
        drainEvents()
        syncInterface()
    }

    private fun pauseRun() {
        if (core.phase != GamePhase.PLAYING) return
        core.pause()
        resetClock()
        syncInterface()
    }

    private fun resetClock() {
        accumulator = 0.0
        lastFrame = null
    }

    private fun drainEvents() {
        if (core.best > savedBest) {
            savedBest = core.best
            writeSetting("best", savedBest.toString())
        }
        for (event in core.events) {
            if (soundOn) audio.play(event)
            if (canVibrate && vibrationOn) {
                when (event) {
                    GameEvent.FLAP, GameEvent.FIRE -> window.navigator.asDynamic().vibrate(12)
                    GameEvent.HIT, GameEvent.WIN -> window.navigator.asDynamic().vibrate(55)
                    else -> Unit
                }
            }
        }
        core.events.clear()
    }

    private fun syncInterface() {
        val phase = core.phase
        if (phase == displayedPhase) return
        val previous = displayedPhase
        displayedPhase = phase
        root.setAttribute("data-phase", phase.name.lowercase())
        panel.hidden = phase == GamePhase.PLAYING
        pause.hidden = phase != GamePhase.PLAYING
        if (phase == GamePhase.PLAYING) return

        cancelInput()
        introMark.hidden = phase != GamePhase.READY
        help.hidden = phase != GamePhase.READY
        results.hidden = phase != GamePhase.OVER && phase != GamePhase.WON
        when (phase) {
            GamePhase.READY -> {
                eyebrow.textContent = "Survive to sunrise"
                heading.textContent = "Skywright"
                action.textContent = "Play"
                details.textContent = "5:00 to sunrise · Best ${core.best}"
            }
            GamePhase.PAUSED -> {
                eyebrow.textContent = "Flight on hold"
                heading.textContent = "Paused"
                action.textContent = "Resume"
                details.textContent = "Press Esc to resume."
            }
            GamePhase.OVER, GamePhase.WON -> {
                val won = phase == GamePhase.WON
                eyebrow.textContent = if (won) "You reached sunrise" else "Run ended"
                heading.textContent = if (won) "Daybreak" else "Sky closed"
                score.textContent = core.score.toString()
                scoreCaption.textContent = if (core.score == 1) "Wall cleared" else "Walls cleared"
                action.textContent = "Fly again"
                details.textContent = "Sunrise ${(core.dawnProgress() * 100).toInt()}% · Best ${core.best}"
            }
            GamePhase.PLAYING -> Unit
        }
        if (previous != null) heading.focus()
    }

    private fun updateSettingButton(button: HTMLButtonElement, enabled: Boolean) {
        button.setAttribute("aria-pressed", enabled.toString())
        button.querySelector("span")?.textContent = if (enabled) "on" else "off"
    }
}

private inline fun <reified T : HTMLElement> element(id: String): T = document.getElementById(id) as T

private fun readSetting(key: String): String? = try {
    window.localStorage.getItem("skywright.$key")
} catch (_: Throwable) {
    null
}

private fun writeSetting(key: String, value: String) {
    try {
        window.localStorage.setItem("skywright.$key", value)
    } catch (_: Throwable) {
        // Play remains available when the browser blocks storage.
    }
}
