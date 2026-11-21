package com.skywright.game.web

import com.skywright.game.GameEvent
import kotlinx.browser.window

internal class BrowserAudio {
    private var context: dynamic = null

    fun unlock() {
        try {
            if (context == null) {
                val constructor = window.asDynamic().AudioContext ?: window.asDynamic().webkitAudioContext ?: return
                val construct = js("Reflect.construct")
                context = construct(constructor, emptyArray<Any>())
            }
            if (context.state == "suspended") context.resume().catch { _: dynamic -> Unit }
        } catch (_: Throwable) {
            // Some browsers or user settings block Web Audio.
        }
    }

    fun play(event: GameEvent) {
        val audio = context ?: return
        if (audio.state != "running") return
        val (low, high, duration) =
            when (event) {
                GameEvent.FLAP -> Triple(697, 1336, 0.045)
                GameEvent.FIRE -> Triple(770, 1336, 0.065)
                GameEvent.OPEN -> Triple(770, 1477, 0.080)
                GameEvent.MISS -> Triple(400, 0, 0.070)
                GameEvent.SCORE -> Triple(852, 1336, 0.110)
                GameEvent.HIT -> Triple(400, 0, 0.180)
                GameEvent.WIN -> Triple(852, 1477, 0.260)
            }
        val now = audio.currentTime as Double
        val gain = audio.createGain()
        gain.connect(audio.destination)
        gain.gain.setValueAtTime(0.0, now)
        gain.gain.linearRampToValueAtTime(0.055, now + 0.006)
        gain.gain.linearRampToValueAtTime(0.0, now + duration)
        val root = tone(audio, gain, low, now, duration)
        if (high > 0) tone(audio, gain, high, now, duration)
        root.onended = { gain.disconnect() }
    }

    private fun tone(
        audio: dynamic,
        gain: dynamic,
        pitch: Int,
        start: Double,
        duration: Double,
    ): dynamic {
        val oscillator = audio.createOscillator()
        oscillator.frequency.setValueAtTime(pitch, start)
        oscillator.connect(gain)
        oscillator.start(start)
        oscillator.stop(start + duration)
        return oscillator
    }
}
