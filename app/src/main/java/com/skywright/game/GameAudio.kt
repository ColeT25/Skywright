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
            GameEvent.FIRE -> ToneGenerator.TONE_DTMF_5
            GameEvent.OPEN -> ToneGenerator.TONE_DTMF_6
            GameEvent.MISS -> ToneGenerator.TONE_PROP_NACK
            GameEvent.SCORE -> ToneGenerator.TONE_DTMF_8
            GameEvent.HIT -> ToneGenerator.TONE_PROP_NACK
            GameEvent.WIN -> ToneGenerator.TONE_DTMF_9
        }
        val duration = when (event) {
            GameEvent.FLAP -> 45
            GameEvent.FIRE -> 65
            GameEvent.OPEN -> 80
            GameEvent.MISS -> 70
            GameEvent.SCORE -> 110
            GameEvent.HIT -> 180
            GameEvent.WIN -> 260
        }
        tones?.startTone(tone, duration)
    }

    fun release() = tones?.release() ?: Unit
}
