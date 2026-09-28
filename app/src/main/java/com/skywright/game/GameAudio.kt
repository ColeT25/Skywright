package com.skywright.game

import android.media.AudioManager
import android.media.ToneGenerator

/** Tiny synthesized cues keep the APK self-contained. */
class GameAudio {
    private val tones: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 42) } catch (_: RuntimeException) { null }

    fun play(event: GameEvent, enabled: Boolean) {
        if (!enabled) return
        val tone = when (event) {
            GameEvent.FLAP -> ToneGenerator.TONE_DTMF_2
            GameEvent.OPEN -> ToneGenerator.TONE_DTMF_6
            GameEvent.SCORE -> ToneGenerator.TONE_DTMF_8
            GameEvent.HIT -> ToneGenerator.TONE_PROP_NACK
        }
        val duration = when (event) {
            GameEvent.FLAP -> 45
            GameEvent.OPEN -> 80
            GameEvent.SCORE -> 110
            GameEvent.HIT -> 180
        }
        tones?.startTone(tone, duration)
    }

    fun release() = tones?.release() ?: Unit
}

