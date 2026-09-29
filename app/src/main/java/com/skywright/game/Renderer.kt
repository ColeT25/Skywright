package com.skywright.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Original vector artwork drawn in logical game coordinates. */
class Renderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dark = Color.rgb(10, 20, 47)
    private val midnight = Color.rgb(22, 34, 72)
    private val blue = Color.rgb(52, 74, 129)
    private val cyan = Color.rgb(121, 241, 225)
    private val cream = Color.rgb(255, 240, 204)
    private val coral = Color.rgb(255, 161, 149)
    private val gold = Color.rgb(255, 209, 133)
    private val stars = List(58) {
        val rng = Random(314 + it * 17)
        Triple(rng.nextFloat() * 360f, 74f + rng.nextFloat() * 545f, 0.6f + rng.nextFloat() * 1.7f)
    }
    private val titleFace = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val bodyFace = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun draw(canvas: Canvas, core: GameCore, ambient: Float, soundOn: Boolean, vibrationOn: Boolean) {
        canvas.drawColor(dark)
        val scale = minOf(canvas.width / GameCore.WIDTH, canvas.height / GameCore.HEIGHT)
        canvas.save()
        canvas.translate((canvas.width - GameCore.WIDTH * scale) / 2f, (canvas.height - GameCore.HEIGHT * scale) / 2f)
        canvas.scale(scale, scale)

        drawSky(canvas, ambient, core.dawnProgress())
        drawHorizon(canvas, ambient)
        core.walls.forEach { drawWall(canvas, core, it, ambient) }
        drawGuideAndPulses(canvas, core, ambient)
        core.sparks.forEach { spark ->
            fill(if (spark.warm) coral else cyan, (spark.life * 300).toInt().coerceIn(0, 255))
            canvas.drawCircle(spark.x, spark.y, 1.7f + spark.life * 2f, paint)
        }
        drawMoth(canvas, core, ambient)
        drawHud(canvas, core)

        when (core.phase) {
            GamePhase.READY -> drawReady(canvas, core, soundOn, vibrationOn, ambient)
            GamePhase.PAUSED -> drawPaused(canvas, soundOn, vibrationOn)
            GamePhase.OVER -> drawOver(canvas, core, soundOn, vibrationOn)
            GamePhase.WON -> drawWon(canvas, core, soundOn, vibrationOn)
            GamePhase.PLAYING -> Unit
        }
        canvas.restore()
    }

    private fun drawSky(canvas: Canvas, ambient: Float, progress: Float) {
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.shader = LinearGradient(0f, 0f, 0f, 720f,
            intArrayOf(
                blend(Color.rgb(11, 22, 54), Color.rgb(69, 139, 200), progress),
                blend(Color.rgb(31, 45, 88), Color.rgb(115, 173, 218), progress),
                blend(Color.rgb(87, 70, 107), Color.rgb(224, 174, 173), progress),
                blend(Color.rgb(180, 103, 116), Color.rgb(255, 199, 137), progress),
            ),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 360f, 720f, paint)
        paint.shader = null

        stars.forEachIndexed { index, (sx, sy, radius) ->
            val x = (sx - ambient * (2f + index % 4) + 3600f) % 360f
            val twinkle = ((150 + 75 * sin(ambient * 2f + index)) * (1f - progress * 0.9f)).toInt().coerceIn(0, 230)
            fill(cream, twinkle)
            canvas.drawCircle(x, sy, radius, paint)
        }

        val sunX = 286f - 30f * sin(progress * 1.570796f)
        val sunY = 690f - 510f * progress
        fill(gold, 42)
        canvas.drawCircle(sunX, sunY, 68f, paint)
        fill(coral, 54)
        canvas.drawCircle(sunX, sunY, 54f, paint)
        stroke(gold, (progress * 145f).toInt().coerceIn(0, 145), 2f)
        for (index in 0 until 12) {
            val angle = index * 6.283185f / 12f + ambient * 0.07f
            val dx = cos(angle)
            val dy = sin(angle)
            canvas.drawLine(sunX + dx * 48f, sunY + dy * 48f, sunX + dx * 57f, sunY + dy * 57f, paint)
        }
        fill(cream, 185 + (progress * 70).toInt())
        canvas.drawCircle(sunX, sunY, 37f, paint)
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val p = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * p).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * p).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * p).toInt(),
        )
    }

    private fun drawHorizon(canvas: Canvas, ambient: Float) {
        fill(midnight, 145)
        for (i in 0..8) {
            val x = i * 55f - ((ambient * 7f) % 55f)
            val h = 65f + ((i * 37) % 5) * 17f
            canvas.drawRoundRect(x, GameCore.BOTTOM - h, x + 39f, GameCore.BOTTOM + 2f, 6f, 6f, paint)
        }
        fill(dark)
        canvas.drawRect(0f, GameCore.BOTTOM, 360f, 720f, paint)
        fill(coral, 175)
        canvas.drawRect(0f, GameCore.BOTTOM, 360f, GameCore.BOTTOM + 2f, paint)
        fill(cream, 18)
        for (i in 0..12) {
            val x = i * 34f - ((ambient * 42f) % 34f)
            canvas.drawRoundRect(x, 682f, x + 17f, 684f, 1f, 1f, paint)
        }
        fill(dark, 150)
        canvas.drawRect(0f, 0f, 360f, GameCore.TOP, paint)
    }

    private fun drawWall(canvas: Canvas, core: GameCore, wall: Wall, ambient: Float) {
        val x = wall.x
        if (x < -GameCore.WALL_WIDTH || x > 380f) return
        val gap = wall.gapCenter
        if (gap == null) {
            drawPaperPanel(canvas, x, GameCore.TOP, GameCore.BOTTOM, false, false)
            val weakTop = wall.weakCenter - wall.weakHalfHeight
            val weakBottom = wall.weakCenter + wall.weakHalfHeight
            val targetColor = if (wall.missAge > 0f) coral else cyan
            fill(targetColor, if (wall.missAge > 0f) 110 else 58)
            canvas.drawRect(x + 4f, weakTop, x + GameCore.WALL_WIDTH - 4f, weakBottom, paint)
            stroke(targetColor, 235, 2.5f)
            canvas.drawLine(x - 4f, weakTop, x + GameCore.WALL_WIDTH + 4f, weakTop, paint)
            canvas.drawLine(x - 4f, weakBottom, x + GameCore.WALL_WIDTH + 4f, weakBottom, paint)
            fill(targetColor, 220)
            canvas.drawCircle(x + GameCore.WALL_WIDTH / 2f, wall.weakCenter, 6f, paint)
            fill(dark)
            canvas.drawCircle(x + GameCore.WALL_WIDTH / 2f, wall.weakCenter, 2.5f, paint)
        } else {
            val half = core.gapHeight(wall) / 2f
            val upper = gap - half
            val lower = gap + half
            drawPaperPanel(canvas, x, GameCore.TOP, upper, true, false)
            drawPaperPanel(canvas, x, lower, GameCore.BOTTOM, false, true)
            // Curl lines grow away from the doorway without changing its hitbox.
            val curl = (wall.openAge / 0.24f).coerceIn(0f, 1f) * 15f
            fill(coral, 145)
            canvas.drawRoundRect(x + 3f, upper - curl, x + GameCore.WALL_WIDTH - 3f, upper, 2f, 2f, paint)
            canvas.drawRoundRect(x + 3f, lower, x + GameCore.WALL_WIDTH - 3f, lower + curl, 2f, 2f, paint)
            stroke(cyan, 220, 2.5f)
            canvas.drawLine(x - 3f, upper, x + GameCore.WALL_WIDTH + 3f, upper, paint)
            canvas.drawLine(x - 3f, lower, x + GameCore.WALL_WIDTH + 3f, lower, paint)
            fill(cyan, (125 + 45 * sin(ambient * 5f)).toInt().coerceIn(60, 200))
            canvas.drawCircle(x + GameCore.WALL_WIDTH / 2f, upper, 3f, paint)
            canvas.drawCircle(x + GameCore.WALL_WIDTH / 2f, lower, 3f, paint)
        }
    }

    private fun drawPaperPanel(canvas: Canvas, x: Float, top: Float, bottom: Float, openAtBottom: Boolean, openAtTop: Boolean) {
        if (bottom <= top) return
        fill(dark)
        canvas.drawRect(x, top, x + GameCore.WALL_WIDTH, bottom, paint)
        fill(blue, 220)
        canvas.drawRect(x + 4f, top, x + GameCore.WALL_WIDTH - 4f, bottom, paint)
        fill(midnight, 220)
        canvas.drawRect(x + 10f, top, x + GameCore.WALL_WIDTH - 10f, bottom, paint)
        stroke(cream, 75, 1f)
        canvas.drawLine(x + 8f, top, x + 8f, bottom, paint)
        canvas.drawLine(x + GameCore.WALL_WIDTH - 8f, top, x + GameCore.WALL_WIDTH - 8f, bottom, paint)
        fill(gold, 90)
        var y = top + 19f
        while (y < bottom - 16f) {
            canvas.drawCircle(x + GameCore.WALL_WIDTH / 2f, y, 2f, paint)
            y += 30f
        }
        if (openAtBottom || openAtTop) {
            fill(coral, 150)
            val edgeY = if (openAtBottom) bottom - 5f else top
            canvas.drawRect(x, edgeY, x + GameCore.WALL_WIDTH, edgeY + 5f, paint)
        }
    }

    private fun drawGuideAndPulses(canvas: Canvas, core: GameCore, ambient: Float) {
        if (core.gliding && core.phase == GamePhase.PLAYING) {
            val target = core.targetWall()
            val ready = core.canFire()
            val aligned = target != null && abs(core.birdY - target.weakCenter) <= target.weakHalfHeight
            val aimColor = if (!ready) cream else if (aligned) cyan else coral
            val aimAlpha = if (ready) 195 else 70
            val endX = target?.x?.coerceAtMost(350f) ?: 346f
            stroke(aimColor, aimAlpha, 1.5f)
            var lineX = GameCore.BIRD_X + 19f
            while (lineX < endX) {
                canvas.drawLine(lineX, core.birdY, minOf(lineX + 8f, endX), core.birdY, paint)
                lineX += 14f
            }
            if (target != null) {
                stroke(aimColor, aimAlpha, 2.2f)
                canvas.drawCircle(target.x, core.birdY, 7f + sin(ambient * 9f), paint)
            }
        }
        if (core.guideLife > 0f) {
            stroke(cyan, (core.guideLife * 140).toInt().coerceIn(0, 150), 1f)
            canvas.drawLine(GameCore.BIRD_X, core.guideY, 360f, core.guideY, paint)
        }
        core.pulses.forEach { pulse ->
            stroke(cyan, 140, 1.5f)
            canvas.drawLine(GameCore.BIRD_X + 13f, pulse.y, pulse.x, pulse.y, paint)
            stroke(cyan, 230, 2.2f)
            canvas.drawCircle(pulse.x, pulse.y, 9f + 2f * sin(ambient * 18f), paint)
            fill(cream)
            canvas.drawCircle(pulse.x, pulse.y, 3.5f, paint)
        }
    }

    private fun drawMoth(canvas: Canvas, core: GameCore, ambient: Float) {
        val y = core.birdY + if (core.phase == GamePhase.READY) sin(ambient * 2f) * 6f else 0f
        canvas.save()
        canvas.translate(GameCore.BIRD_X, y)
        canvas.rotate((core.birdVy / 430f * 24f).coerceIn(-23f, 25f))
        val flutter = if (core.gliding) sin(ambient * 4f) else sin(ambient * 22f) * 5f
        val wingSpan = if (core.gliding) 42f else 31f
        fill(cyan, 33)
        canvas.drawCircle(0f, 0f, 25f, paint)
        val leftWing = Path().apply {
            moveTo(-2f, -2f); cubicTo(-wingSpan, -25f - flutter, -wingSpan - 4f, 11f + flutter, -3f, 8f); close()
        }
        val rightWing = Path().apply {
            moveTo(2f, -2f); cubicTo(wingSpan, -25f - flutter, wingSpan + 4f, 11f + flutter, 3f, 8f); close()
        }
        fill(cream)
        canvas.drawPath(leftWing, paint)
        canvas.drawPath(rightWing, paint)
        fill(coral)
        canvas.drawCircle(-15f, -1f, 4f, paint)
        canvas.drawCircle(15f, -1f, 4f, paint)
        fill(gold)
        canvas.drawOval(-7f, -12f, 7f, 12f, paint)
        fill(dark)
        canvas.drawCircle(2f, -4f, 1.8f, paint)
        stroke(gold, 220, 1.5f)
        canvas.drawLine(-3f, -10f, -8f, -17f, paint)
        canvas.drawLine(3f, -10f, 8f, -17f, paint)
        canvas.restore()
    }

    private fun drawHud(canvas: Canvas, core: GameCore) {
        label(canvas, "DAWN", 47f, 22f, 10f, cyan, bodyFace)
        val seconds = core.secondsToDawn()
        label(canvas, "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", 47f, 43f, 17f, cream, titleFace)
        label(canvas, "${core.score}", 180f, 35f, 29f, cream, titleFace)
        fill(cream, 45)
        canvas.drawRoundRect(108f, 47f, 272f, 52f, 3f, 3f, paint)
        fill(gold)
        canvas.drawRoundRect(108f, 47f, 108f + 164f * core.dawnProgress(), 52f, 3f, 3f, paint)
        if (core.phase == GamePhase.PLAYING) {
            val burstLabel = when {
                core.pulses.isNotEmpty() -> "GUST IN FLIGHT"
                core.shotCooldown > 0f -> "GUST RECHARGING"
                core.gliding && core.canFire() -> "RELEASE TO FIRE"
                core.gliding -> "GLIDING"
                else -> "GUST READY"
            }
            val charged = core.shotCooldown <= 0f && core.pulses.isEmpty()
            label(canvas, burstLabel, 180f, 72f, 10f, if (charged) cyan else cream, bodyFace)
            fill(cream, 45)
            canvas.drawRoundRect(133f, 77f, 227f, 80f, 2f, 2f, paint)
            fill(if (charged) cyan else gold, 220)
            canvas.drawRoundRect(133f, 77f,
                133f + 94f * (1f - core.shotCooldown / GameCore.SHOT_COOLDOWN).coerceIn(0f, 1f),
                80f, 2f, 2f, paint)
            stroke(cream, 210, 2.5f)
            canvas.drawLine(316f, 19f, 316f, 39f, paint)
            canvas.drawLine(327f, 19f, 327f, 39f, paint)
        }
    }

    private fun drawReady(canvas: Canvas, core: GameCore, soundOn: Boolean, vibrationOn: Boolean, ambient: Float) {
        shade(canvas)
        fill(midnight, 238)
        canvas.drawRoundRect(26f, 174f, 334f, 551f, 24f, 24f, paint)
        stroke(cyan, 105, 2f)
        canvas.drawRoundRect(26f, 174f, 334f, 551f, 24f, 24f, paint)
        label(canvas, "SKYWRIGHT", 180f, 240f, 43f, cream, titleFace)
        label(canvas, "CUT YOUR OWN WAY THROUGH", 180f, 271f, 13f, cyan, bodyFace)
        drawTinyGate(canvas, 180f, 337f, ambient)
        label(canvas, "TAP TO FLAP", 180f, 419f, 23f, cream, titleFace)
        label(canvas, "Hold to glide, release to fire", 180f, 447f, 15f, cream, bodyFace)
        label(canvas, "Hit each glowing band", 180f, 468f, 15f, cream, bodyFace)
        fill(coral)
        canvas.drawRoundRect(74f, 494f, 286f, 535f, 20f, 20f, paint)
        label(canvas, "TAP TO BEGIN", 180f, 521f, 17f, dark, titleFace)
        drawToggles(canvas, soundOn, vibrationOn)
        label(canvas, "SURVIVE TO SUNRISE  •  5 MIN", 180f, 581f, 13f, gold, bodyFace)
        label(canvas, "BEST  ${core.best}", 180f, 604f, 14f, cream, bodyFace)
    }

    private fun drawPaused(canvas: Canvas, soundOn: Boolean, vibrationOn: Boolean) {
        shade(canvas)
        fill(midnight, 238)
        canvas.drawRoundRect(36f, 238f, 324f, 490f, 24f, 24f, paint)
        stroke(cyan, 105, 2f)
        canvas.drawRoundRect(36f, 238f, 324f, 490f, 24f, 24f, paint)
        label(canvas, "PAUSED", 180f, 313f, 39f, cream, titleFace)
        label(canvas, "The sky can wait.", 180f, 345f, 16f, cyan, bodyFace)
        fill(coral)
        canvas.drawRoundRect(74f, 389f, 286f, 432f, 21f, 21f, paint)
        label(canvas, "TAP TO RESUME", 180f, 417f, 18f, dark, titleFace)
        drawToggles(canvas, soundOn, vibrationOn)
    }

    private fun drawOver(canvas: Canvas, core: GameCore, soundOn: Boolean, vibrationOn: Boolean) {
        shade(canvas)
        fill(midnight, 240)
        canvas.drawRoundRect(32f, 192f, 328f, 528f, 24f, 24f, paint)
        stroke(coral, 125, 2f)
        canvas.drawRoundRect(32f, 192f, 328f, 528f, 24f, 24f, paint)
        label(canvas, "SKY CLOSED", 180f, 261f, 35f, cream, titleFace)
        label(canvas, "YOU FLEW THROUGH", 180f, 301f, 13f, cyan, bodyFace)
        label(canvas, "${core.score}", 180f, 377f, 72f, gold, titleFace)
        label(canvas, "WALLS     •     BEST  ${core.best}", 180f, 410f, 14f, cream, bodyFace)
        label(canvas, "SUNRISE  ${(core.dawnProgress() * 100f).toInt()}%", 180f, 437f, 13f, gold, bodyFace)
        fill(coral)
        canvas.drawRoundRect(74f, 458f, 286f, 505f, 23f, 23f, paint)
        label(canvas, "TAP TO FLY AGAIN", 180f, 489f, 18f, dark, titleFace)
        drawToggles(canvas, soundOn, vibrationOn)
    }

    private fun drawWon(canvas: Canvas, core: GameCore, soundOn: Boolean, vibrationOn: Boolean) {
        shade(canvas)
        fill(midnight, 238)
        canvas.drawRoundRect(30f, 192f, 330f, 528f, 24f, 24f, paint)
        stroke(gold, 220, 2.5f)
        canvas.drawRoundRect(30f, 192f, 330f, 528f, 24f, 24f, paint)
        label(canvas, "DAYBREAK", 180f, 263f, 40f, gold, titleFace)
        label(canvas, "YOU WON THE SKY", 180f, 303f, 15f, cyan, bodyFace)
        label(canvas, "${core.score}", 180f, 379f, 72f, cream, titleFace)
        label(canvas, "WALLS CLEARED  •  5:00", 180f, 413f, 14f, cream, bodyFace)
        fill(coral)
        canvas.drawRoundRect(74f, 458f, 286f, 505f, 23f, 23f, paint)
        label(canvas, "FLY AGAIN", 180f, 489f, 18f, dark, titleFace)
        drawToggles(canvas, soundOn, vibrationOn)
    }

    private fun drawTinyGate(canvas: Canvas, x: Float, y: Float, ambient: Float) {
        fill(blue)
        canvas.drawRoundRect(x - 29f, y - 40f, x - 14f, y + 40f, 4f, 4f, paint)
        canvas.drawRoundRect(x + 14f, y - 40f, x + 29f, y + 40f, 4f, 4f, paint)
        stroke(cyan, 210, 2f)
        canvas.drawLine(x - 16f, y - 17f, x + 16f, y - 17f, paint)
        canvas.drawLine(x - 16f, y + 17f, x + 16f, y + 17f, paint)
        fill(cream)
        canvas.drawCircle(x + sin(ambient * 2f) * 4f, y, 7f, paint)
    }

    private fun drawToggles(canvas: Canvas, soundOn: Boolean, vibrationOn: Boolean) {
        fill(midnight, 230)
        canvas.drawRoundRect(43f, 626f, 169f, 665f, 16f, 16f, paint)
        canvas.drawRoundRect(191f, 626f, 317f, 665f, 16f, 16f, paint)
        stroke(cyan, 85, 1.2f)
        canvas.drawRoundRect(43f, 626f, 169f, 665f, 16f, 16f, paint)
        canvas.drawRoundRect(191f, 626f, 317f, 665f, 16f, 16f, paint)
        label(canvas, "SOUND ${if (soundOn) "ON" else "OFF"}", 106f, 651f, 13f, cream, bodyFace)
        label(canvas, "VIBE ${if (vibrationOn) "ON" else "OFF"}", 254f, 651f, 13f, cream, bodyFace)
    }

    private fun shade(canvas: Canvas) {
        fill(dark, 180)
        canvas.drawRect(0f, 0f, 360f, 720f, paint)
    }

    private fun label(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float, color: Int, face: Typeface) {
        fill(color)
        paint.textSize = size
        paint.typeface = face
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, x, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun fill(color: Int, alpha: Int = 255) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f
        paint.color = color
        paint.alpha = alpha
        paint.shader = null
    }

    private fun stroke(color: Int, alpha: Int = 255, width: Float = 1f) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = color
        paint.alpha = alpha
        paint.shader = null
    }
}
