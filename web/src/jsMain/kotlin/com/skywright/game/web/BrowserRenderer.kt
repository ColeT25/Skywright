package com.skywright.game.web

import com.skywright.game.GameCore
import com.skywright.game.GamePhase
import com.skywright.game.ScreenLayout
import com.skywright.game.Wall
import com.skywright.game.WindGust
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** Skywright's original artwork, drawn with the browser's Canvas 2D API. */
internal class BrowserRenderer(
    private val ctx: CanvasRenderingContext2D,
) {
    private val dark = rgb(10, 20, 47)
    private val midnight = rgb(22, 34, 72)
    private val blue = rgb(52, 74, 129)
    private val cyan = rgb(121, 241, 225)
    private val cream = rgb(255, 240, 204)
    private val coral = rgb(255, 161, 149)
    private val gold = rgb(255, 209, 133)
    private val stars =
        List(58) {
            val rng = Random(314 + it * 17)
            Triple(rng.nextFloat() * 360f, 74f + rng.nextFloat() * 545f, 0.6f + rng.nextFloat() * 1.7f)
        }
    private val titleFace = "800"
    private val bodyFace = "600"

    private var outlined = false

    fun draw(
        core: GameCore,
        ambient: Float,
        layout: ScreenLayout,
        width: Int,
        height: Int,
        showHud: Boolean,
    ) {
        ctx.setTransform(
            ctx.canvas.width.toDouble() / width,
            0.0,
            0.0,
            ctx.canvas.height.toDouble() / height,
            0.0,
            0.0,
        )
        ctx.translate(layout.offsetX.toDouble(), layout.offsetY.toDouble())
        ctx.scale(layout.scale.toDouble(), layout.scale.toDouble())

        drawSky(ambient, core.dawnProgress(), layout)
        drawHorizon(ambient, core.distanceTravelled, layout)

        // Wide screens show more scenery while preserving the original wall lookahead.
        ctx.save()
        ctx.beginPath()
        ctx.rect(0.0, layout.top.toDouble(), GameCore.WIDTH.toDouble(), (layout.bottom - layout.top).toDouble())
        ctx.clip()
        core.gusts.forEach { drawWindGust(it, ambient) }
        core.walls.forEach { drawWall(core, it, ambient) }
        drawGuideAndPulses(core, ambient)
        core.sparks.forEach { spark ->
            fill(if (spark.warm) coral else cyan, (spark.life * 300).toInt().coerceIn(0, 255))
            circle(spark.x, spark.y, 1.7f + spark.life * 2f)
        }
        drawMoth(core, ambient)
        ctx.restore()

        if (showHud && core.phase == GamePhase.PLAYING) {
            ctx.translate(0.0, layout.hudShift.toDouble())
            drawHud(core)
        }
    }

    private fun drawSky(
        ambient: Float,
        progress: Float,
        layout: ScreenLayout,
    ) {
        val gradient = ctx.createLinearGradient(0.0, 0.0, 0.0, 720.0)
        gradient.addColorStop(0.0, blend(rgb(11, 22, 54), rgb(69, 139, 200), progress).css())
        gradient.addColorStop(1.0 / 3, blend(rgb(31, 45, 88), rgb(115, 173, 218), progress).css())
        gradient.addColorStop(2.0 / 3, blend(rgb(87, 70, 107), rgb(224, 174, 173), progress).css())
        gradient.addColorStop(1.0, blend(rgb(180, 103, 116), rgb(255, 199, 137), progress).css())
        ctx.globalAlpha = 1.0
        ctx.fillStyle = gradient
        ctx.fillRect(
            layout.left.toDouble(),
            layout.top.toDouble(),
            (layout.right - layout.left).toDouble(),
            (layout.bottom - layout.top).toDouble(),
        )

        stars.forEachIndexed { index, (sx, sy, radius) ->
            val x = (sx - (ambient * (2f + index % 4)) % 360f + 360f) % 360f
            val twinkle = ((150 + 75 * sin(ambient * 2f + index)) * (1f - progress * 0.9f)).toInt().coerceIn(0, 230)
            fill(cream, twinkle)
            circle(x, sy, radius)
        }

        val sunX = 286f - 30f * sin(progress * 1.570796f)
        val sunY = 690f - 510f * progress
        fill(gold, 42)
        circle(sunX, sunY, 68f)
        fill(coral, 54)
        circle(sunX, sunY, 54f)
        stroke(gold, (progress * 145f).toInt().coerceIn(0, 145), 2f)
        for (index in 0 until 12) {
            val angle = index * 6.283185f / 12f + ambient * 0.07f
            val dx = cos(angle)
            val dy = sin(angle)
            line(sunX + dx * 48f, sunY + dy * 48f, sunX + dx * 57f, sunY + dy * 57f)
        }
        fill(cream, 185 + (progress * 70).toInt())
        circle(sunX, sunY, 37f)
    }

    private fun blend(
        a: Int,
        b: Int,
        t: Float,
    ): Int {
        val p = t.coerceIn(0f, 1f)
        return rgb(
            (((a shr 16) and 255) + (((b shr 16) and 255) - ((a shr 16) and 255)) * p).toInt(),
            (((a shr 8) and 255) + (((b shr 8) and 255) - ((a shr 8) and 255)) * p).toInt(),
            ((a and 255) + ((b and 255) - (a and 255)) * p).toInt(),
        )
    }

    private fun drawHorizon(
        ambient: Float,
        distance: Float,
        layout: ScreenLayout,
    ) {
        fill(midnight, 145)
        for (i in (floor(layout.left / 55f).toInt() - 1)..(ceil(layout.right / 55f).toInt() + 1)) {
            val x = i * 55f - ((ambient + distance * 0.055f) % 55f)
            val h = 65f + ((i * 37 % 5 + 5) % 5) * 17f
            roundRect(x, GameCore.BOTTOM - h, x + 39f, GameCore.BOTTOM + 2f, 6f)
        }
        // A thin veil lets the below-horizon sun color the foreground at dawn.
        fill(dark, 232)
        rect(layout.left, GameCore.BOTTOM, layout.right, layout.bottom)
        fill(coral, 175)
        rect(0f, GameCore.BOTTOM, 360f, GameCore.BOTTOM + 2f)
        fill(cream, 18)
        for (i in (floor(layout.left / 34f).toInt() - 1)..(ceil(layout.right / 34f).toInt() + 1)) {
            val x = i * 34f - ((ambient * 3f + distance * 0.35f) % 34f)
            val y = 682f + ((i % 3 + 3) % 3) * 22f
            if (y < layout.bottom - 5f) roundRect(x, y, x + 17f, y + 2f, 1f)
        }
        fill(dark, 150)
        rect(layout.left, layout.top, layout.right, GameCore.TOP)
    }

    private fun drawWindGust(
        gust: WindGust,
        ambient: Float,
    ) {
        if (gust.x + gust.width < 0f || gust.x > GameCore.WIDTH) return
        val color = if (gust.direction < 0) cyan else coral
        val top = GameCore.TOP + 30f
        val bottom = GameCore.BOTTOM - 18f
        fill(color, 22)
        roundRect(gust.x, top, gust.x + gust.width, bottom, 28f)
        stroke(color, 105, 1.5f)
        line(gust.x + 4f, top + 22f, gust.x + 4f, bottom - 22f)
        line(gust.x + gust.width - 4f, top + 22f, gust.x + gust.width - 4f, bottom - 22f)
        label(
            if (gust.direction < 0) "UPDRAFT" else "DOWNDRAFT",
            gust.x + gust.width / 2f,
            top + 16f,
            10f,
            color,
            bodyFace,
        )
        for (column in 0..2) {
            val x = gust.x + 25f + column * 31f
            for (row in 0..9) {
                val travel = (row * 56f + ambient * 63f + column * 19f) % 510f
                val y = top + 36f + if (gust.direction < 0) 510f - travel else travel
                if (y < top + 35f || y > bottom - 20f) continue
                val tip = y + gust.direction * 10f
                stroke(color, 100 + column * 25, 2f)
                line(x, y - gust.direction * 8f, x, tip)
                line(x - 5f, tip - gust.direction * 5f, x, tip)
                line(x + 5f, tip - gust.direction * 5f, x, tip)
            }
        }
    }

    private fun drawWall(
        core: GameCore,
        wall: Wall,
        ambient: Float,
    ) {
        val x = wall.x
        if (x < -GameCore.WALL_WIDTH || x > 380f) return
        val gap = wall.gapCenter
        if (gap == null) {
            drawPaperPanel(x, GameCore.TOP, GameCore.BOTTOM, false, false)
            val weakTop = wall.weakCenter - wall.weakHalfHeight
            val weakBottom = wall.weakCenter + wall.weakHalfHeight
            val targetColor = if (wall.missAge > 0f) coral else cyan
            fill(targetColor, if (wall.missAge > 0f) 110 else 58)
            rect(x + 4f, weakTop, x + GameCore.WALL_WIDTH - 4f, weakBottom)
            stroke(targetColor, 235, 2.5f)
            line(x - 4f, weakTop, x + GameCore.WALL_WIDTH + 4f, weakTop)
            line(x - 4f, weakBottom, x + GameCore.WALL_WIDTH + 4f, weakBottom)
            fill(targetColor, 220)
            circle(x + GameCore.WALL_WIDTH / 2f, wall.weakCenter, 6f)
            fill(dark)
            circle(x + GameCore.WALL_WIDTH / 2f, wall.weakCenter, 2.5f)
        } else {
            val half = core.gapHeight(wall) / 2f
            val upper = gap - half
            val lower = gap + half
            drawPaperPanel(x, GameCore.TOP, upper, true, false)
            drawPaperPanel(x, lower, GameCore.BOTTOM, false, true)
            // Curl lines grow away from the doorway without changing its hitbox.
            val curl = (wall.openAge / 0.24f).coerceIn(0f, 1f) * 15f
            fill(coral, 145)
            roundRect(x + 3f, upper - curl, x + GameCore.WALL_WIDTH - 3f, upper, 2f)
            roundRect(x + 3f, lower, x + GameCore.WALL_WIDTH - 3f, lower + curl, 2f)
            stroke(cyan, 220, 2.5f)
            line(x - 3f, upper, x + GameCore.WALL_WIDTH + 3f, upper)
            line(x - 3f, lower, x + GameCore.WALL_WIDTH + 3f, lower)
            fill(cyan, (125 + 45 * sin(ambient * 5f)).toInt().coerceIn(60, 200))
            circle(x + GameCore.WALL_WIDTH / 2f, upper, 3f)
            circle(x + GameCore.WALL_WIDTH / 2f, lower, 3f)
        }
    }

    private fun drawPaperPanel(
        x: Float,
        top: Float,
        bottom: Float,
        openAtBottom: Boolean,
        openAtTop: Boolean,
    ) {
        if (bottom <= top) return
        fill(dark)
        rect(x, top, x + GameCore.WALL_WIDTH, bottom)
        fill(blue, 220)
        rect(x + 4f, top, x + GameCore.WALL_WIDTH - 4f, bottom)
        fill(midnight, 220)
        rect(x + 10f, top, x + GameCore.WALL_WIDTH - 10f, bottom)
        stroke(cream, 75, 1f)
        line(x + 8f, top, x + 8f, bottom)
        line(x + GameCore.WALL_WIDTH - 8f, top, x + GameCore.WALL_WIDTH - 8f, bottom)
        fill(gold, 90)
        var y = top + 19f
        while (y < bottom - 16f) {
            circle(x + GameCore.WALL_WIDTH / 2f, y, 2f)
            y += 30f
        }
        if (openAtBottom || openAtTop) {
            fill(coral, 150)
            val edgeY = if (openAtBottom) bottom - 5f else top
            rect(x, edgeY, x + GameCore.WALL_WIDTH, edgeY + 5f)
        }
    }

    private fun drawGuideAndPulses(
        core: GameCore,
        ambient: Float,
    ) {
        if (core.gliding && core.phase == GamePhase.PLAYING) {
            val target = core.targetWall()
            val ready = core.canFire()
            val aligned = target != null && abs(core.birdY - target.weakCenter) <= target.weakHalfHeight
            val aimColor =
                if (!ready) {
                    cream
                } else if (aligned) {
                    cyan
                } else {
                    coral
                }
            val aimAlpha = if (ready) 195 else 70
            val endX = target?.x?.coerceAtMost(350f) ?: 346f
            stroke(aimColor, aimAlpha, 1.5f)
            var lineX = GameCore.BIRD_X + 19f
            while (lineX < endX) {
                line(lineX, core.birdY, minOf(lineX + 8f, endX), core.birdY)
                lineX += 14f
            }
            if (target != null) {
                stroke(aimColor, aimAlpha, 2.2f)
                circle(target.x, core.birdY, 7f + sin(ambient * 9f))
            }
        }
        if (core.guideLife > 0f) {
            stroke(cyan, (core.guideLife * 140).toInt().coerceIn(0, 150), 1f)
            line(GameCore.BIRD_X, core.guideY, 360f, core.guideY)
        }
        core.pulses.forEach { pulse ->
            stroke(cyan, 140, 1.5f)
            line(GameCore.BIRD_X + 13f, pulse.y, pulse.x, pulse.y)
            stroke(cyan, 230, 2.2f)
            circle(pulse.x, pulse.y, 9f + 2f * sin(ambient * 18f))
            fill(cream)
            circle(pulse.x, pulse.y, 3.5f)
        }
    }

    private fun drawMoth(
        core: GameCore,
        ambient: Float,
    ) {
        val y = core.birdY + if (core.phase == GamePhase.READY) sin(ambient * 2f) * 6f else 0f
        ctx.save()
        ctx.translate(GameCore.BIRD_X.toDouble(), y.toDouble())
        val tilt = (core.birdVy / 430f * 24f + if (core.gliding) 8f else 0f).coerceIn(-23f, 35f)
        ctx.rotate(tilt * PI / 180.0)
        val flutter = if (core.gliding) sin(ambient * 4f) else sin(ambient * 22f) * 5f
        val wingSpan = if (core.gliding) 42f else 31f
        fill(cyan, 33)
        circle(0f, 0f, 25f)
        fill(cream)
        wing(-1, wingSpan, flutter)
        wing(1, wingSpan, flutter)
        fill(coral)
        circle(-15f, -1f, 4f)
        circle(15f, -1f, 4f)
        fill(gold)
        oval(-7f, -12f, 7f, 12f)
        fill(dark)
        circle(2f, -4f, 1.8f)
        stroke(gold, 220, 1.5f)
        line(-3f, -10f, -8f, -17f)
        line(3f, -10f, 8f, -17f)
        ctx.restore()
    }

    private fun drawHud(core: GameCore) {
        val level = core.level
        if (level == null) {
            val seconds = core.secondsToDawn()
            val time = if (seconds == 0) "Sunrise" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
            label(if (seconds == 0) "ENDLESS" else "DAWN", 47f, 22f, 10f, cyan, bodyFace)
            label(time, 47f, 43f, 17f, cream, titleFace)
        } else {
            label("LEVEL", 47f, 22f, 10f, cyan, bodyFace)
            label("${level.ordinal + 1}", 47f, 43f, 17f, cream, titleFace)
        }
        val score = if (level == null) "${core.score}" else "${core.score}/${level.wallCount}"
        label(score, 180f, 35f, 29f, cream, titleFace)
        fill(cream, 45)
        roundRect(108f, 47f, 272f, 52f, 3f)
        fill(gold)
        roundRect(108f, 47f, 108f + 164f * core.dawnProgress(), 52f, 3f)
        if (core.phase == GamePhase.PLAYING) {
            val burstLabel =
                when {
                    core.pulses.isNotEmpty() -> "GUST IN FLIGHT"
                    core.shotCooldown > 0f -> "GUST RECHARGING"
                    core.gliding && core.canFire() -> "RELEASE TO FIRE"
                    core.gliding -> "GLIDING"
                    else -> "GUST READY"
                }
            val charged = core.shotCooldown <= 0f && core.pulses.isEmpty()
            label(burstLabel, 180f, 72f, 10f, if (charged) cyan else cream, bodyFace)
            fill(cream, 45)
            roundRect(133f, 77f, 227f, 80f, 2f)
            fill(if (charged) cyan else gold, 220)
            roundRect(
                133f,
                77f,
                133f + 94f * (1f - core.shotCooldown / GameCore.SHOT_COOLDOWN).coerceIn(0f, 1f),
                80f,
                2f,
            )
        }
    }

    private fun wing(
        direction: Int,
        span: Float,
        flutter: Float,
    ) {
        ctx.beginPath()
        ctx.moveTo(direction * 2.0, -2.0)
        ctx.bezierCurveTo(
            direction * span.toDouble(),
            -25.0 - flutter,
            direction * (span + 4.0),
            11.0 + flutter,
            direction * 3.0,
            8.0,
        )
        ctx.closePath()
        ctx.fill()
    }

    private fun label(
        value: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Int,
        weight: String,
    ) {
        fill(color)
        ctx.font = "$weight ${size}px system-ui, sans-serif"
        ctx.textAlign = CanvasTextAlign.CENTER
        ctx.fillText(value, x.toDouble(), baseline.toDouble())
    }

    private fun fill(
        color: Int,
        alpha: Int = 255,
    ) {
        outlined = false
        ctx.fillStyle = color.css()
        ctx.globalAlpha = alpha / 255.0
    }

    private fun stroke(
        color: Int,
        alpha: Int,
        width: Float,
    ) {
        outlined = true
        ctx.strokeStyle = color.css()
        ctx.globalAlpha = alpha / 255.0
        ctx.lineWidth = width.toDouble()
    }

    private fun rect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        ctx.fillRect(left.toDouble(), top.toDouble(), (right - left).toDouble(), (bottom - top).toDouble())
    }

    private fun roundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
    ) {
        if (left >= right || top >= bottom) return
        val r = minOf(radius, (right - left) / 2f, (bottom - top) / 2f)
        ctx.beginPath()
        ctx.moveTo((left + r).toDouble(), top.toDouble())
        ctx.arcTo(right.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble(), r.toDouble())
        ctx.arcTo(right.toDouble(), bottom.toDouble(), left.toDouble(), bottom.toDouble(), r.toDouble())
        ctx.arcTo(left.toDouble(), bottom.toDouble(), left.toDouble(), top.toDouble(), r.toDouble())
        ctx.arcTo(left.toDouble(), top.toDouble(), right.toDouble(), top.toDouble(), r.toDouble())
        ctx.closePath()
        paintPath()
    }

    private fun circle(
        x: Float,
        y: Float,
        radius: Float,
    ) {
        ctx.beginPath()
        ctx.arc(x.toDouble(), y.toDouble(), radius.toDouble(), 0.0, 2 * PI)
        paintPath()
    }

    private fun oval(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        ctx.beginPath()
        ctx.ellipse(
            (left + right) / 2.0,
            (top + bottom) / 2.0,
            (right - left) / 2.0,
            (bottom - top) / 2.0,
            0.0,
            0.0,
            2 * PI,
        )
        paintPath()
    }

    private fun line(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ) {
        ctx.beginPath()
        ctx.moveTo(x1.toDouble(), y1.toDouble())
        ctx.lineTo(x2.toDouble(), y2.toDouble())
        ctx.stroke()
    }

    private fun paintPath() {
        if (outlined) ctx.stroke() else ctx.fill()
    }

    private fun rgb(
        red: Int,
        green: Int,
        blue: Int,
    ) = (red shl 16) or (green shl 8) or blue

    private fun Int.css() = "#" + toString(16).padStart(6, '0')
}
