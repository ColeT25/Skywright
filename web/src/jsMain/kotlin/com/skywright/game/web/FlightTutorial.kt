package com.skywright.game.web

import com.skywright.game.GameCore
import com.skywright.game.GamePhase

internal enum class TutorialCue(
    val heading: String,
    val instruction: String,
) {
    FLAP("Flap", "Tap or click the sky, or tap Space, to lift the moth."),
    GLIDE("Hold to glide", "Hold your finger, mouse button, or Space until the aiming line appears."),
    RELEASE("Release to fire", "Let go while the line is cyan to open the glowing band."),
    SHOT("Air burst", "The burst travels straight from where you released."),
    RETRY_SHOT("Try the shot again", "Hold to aim, then let go when the line turns cyan."),
    CLEAR("Fly through", "Tap or press Space to continue. Flap when the moth drops toward the bottom of the gap."),
    RETRY_CLEAR("Try the gap again", "Stay inside the opening. Flap as the moth approaches the lower edge."),
    COMPLETE("Tutorial complete", "Each wall you clear earns a point. Survive five minutes to reach sunrise."),
}

/** Runs practice attempts with the same inputs, physics, and scoring as a regular flight. */
internal class FlightTutorial {
    private enum class Lesson { FLAP, AIM, CLEAR, COMPLETE }

    val core = GameCore()
    private var lesson = Lesson.FLAP
    private var retry = false
    private var nextLessonAt: Float? = null

    init {
        resetAttempt()
    }

    val cue: TutorialCue
        get() =
            when (lesson) {
                Lesson.FLAP -> TutorialCue.FLAP
                Lesson.AIM ->
                    when {
                        core.gliding -> TutorialCue.RELEASE
                        core.pulses.isNotEmpty() -> TutorialCue.SHOT
                        retry || core.walls.first().missAge > 0f -> TutorialCue.RETRY_SHOT
                        else -> TutorialCue.GLIDE
                    }
                Lesson.CLEAR -> if (retry) TutorialCue.RETRY_CLEAR else TutorialCue.CLEAR
                Lesson.COMPLETE -> TutorialCue.COMPLETE
            }

    fun endPress(heldSeconds: Float) {
        if (lesson == Lesson.FLAP && nextLessonAt == null && !core.gliding && heldSeconds < GameCore.HOLD_SECONDS) {
            // Let the player see the flap before setting up the next exercise.
            nextLessonAt = core.elapsed + 0.25f
        }
        core.endPress(heldSeconds)
    }

    /** Returns true when the browser needs to clear held input for a new attempt or lesson. */
    fun advance(): Boolean =
        when {
            lesson == Lesson.COMPLETE -> false
            lesson == Lesson.CLEAR && core.score > 0 -> {
                lesson = Lesson.COMPLETE
                core.pause()
                true
            }
            core.phase == GamePhase.OVER -> {
                retry = true
                resetAttempt()
                true
            }
            lesson == Lesson.FLAP && nextLessonAt?.let { core.elapsed >= it } == true -> {
                lesson = Lesson.AIM
                retry = false
                resetAttempt()
                true
            }
            lesson == Lesson.AIM && core.walls.first().gapCenter != null -> {
                lesson = Lesson.CLEAR
                retry = false
                core.pause()
                true
            }
            else -> false
        }

    private fun resetAttempt() {
        nextLessonAt = null
        core.reset()
        if (lesson == Lesson.FLAP) {
            core.walls.removeAt(0)
        } else {
            val wall = core.walls.first()
            core.walls[0] =
                wall.copy(
                    x = 260f,
                    openingHeight = 246f,
                    gapCenter = if (lesson == Lesson.CLEAR) wall.weakCenter else null,
                )
        }
    }
}
