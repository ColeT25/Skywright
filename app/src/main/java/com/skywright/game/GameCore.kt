package com.skywright.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.random.Random

enum class GamePhase { READY, PLAYING, PAUSED, OVER, WON }
enum class GameEvent { FLAP, OPEN, MISS, SCORE, HIT, WIN }

data class Wall(
    val id: Int,
    var x: Float,
    var gapCenter: Float? = null,
    var pulsePending: Boolean = false,
    var scored: Boolean = false,
    var openAge: Float = 0f,
    val weakCenter: Float = 350f,
    val weakHalfHeight: Float = 80f,
    val openingHeight: Float = 164f,
    var missAge: Float = 0f,
)

data class Pulse(val targetId: Int, var x: Float, val y: Float)
data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val warm: Boolean = false)

/** Android-free fixed-step simulation. All mutations occur on the View's UI thread. */
class GameCore(private val random: Random = Random(19), private val winSeconds: Float = 300f) {
    companion object {
        const val WIDTH = 360f
        const val HEIGHT = 720f
        const val TOP = 58f
        const val BOTTOM = 660f
        const val BIRD_X = 96f
        const val BIRD_RADIUS = 12f
        const val WALL_WIDTH = 52f
        const val PULSE_SPEED = 520f
        const val GRAVITY = 850f
        const val FLAP_SPEED = -280f
    }

    init { require(winSeconds > 0f) }

    var phase = GamePhase.READY
        private set
    var birdY = 350f
        private set
    var birdVy = 0f
        private set
    var score = 0
        private set
    var best = 0
    var elapsed = 0f
        private set
    var guideY = 350f
        private set
    var guideLife = 0f
        private set
    var lastCutY = 350f
        private set

    val walls = mutableListOf<Wall>()
    val pulses = mutableListOf<Pulse>()
    val sparks = mutableListOf<Spark>()
    val events = mutableListOf<GameEvent>()
    private var nextWallId = 0

    init { reset() }

    fun dawnProgress(): Float = (elapsed / winSeconds).coerceIn(0f, 1f)
    fun wallSpeed(): Float = 120f + 90f * dawnProgress()
    fun secondsToDawn(): Int = kotlin.math.ceil((winSeconds - elapsed).coerceAtLeast(0f)).toInt()

    fun reset() {
        phase = GamePhase.READY
        birdY = 350f
        birdVy = 0f
        score = 0
        elapsed = 0f
        guideLife = 0f
        walls.clear()
        pulses.clear()
        sparks.clear()
        events.clear()
        nextWallId = 0
        val introCenters = floatArrayOf(350f, 392f, 328f)
        repeat(3) { index ->
            walls += Wall(nextWallId++, 340f + index * 310f, weakCenter = introCenters[index])
        }
    }

    fun start() {
        if (phase != GamePhase.READY) return
        phase = GamePhase.PLAYING
        flap()
    }

    fun restart() {
        reset()
        start()
    }

    fun pause() {
        if (phase == GamePhase.PLAYING) phase = GamePhase.PAUSED
    }

    fun resume() {
        if (phase == GamePhase.PAUSED) phase = GamePhase.PLAYING
    }

    /** A flap always moves the moth. Its pulse can cut the first visible unclaimed weak band. */
    fun flap() {
        if (phase != GamePhase.PLAYING) return
        birdVy = FLAP_SPEED
        guideY = birdY
        guideLife = 0.8f
        val target = walls.firstOrNull {
            it.x > BIRD_X + BIRD_RADIUS && it.x <= WIDTH && it.gapCenter == null && !it.pulsePending
        }
        if (target != null) {
            target.pulsePending = true
            pulses += Pulse(target.id, BIRD_X + 14f, birdY)
        }
        events += GameEvent.FLAP
    }

    fun update(dt: Float) {
        if (phase != GamePhase.PLAYING || dt <= 0f) return
        elapsed += dt
        guideLife = max(0f, guideLife - dt)
        birdVy = min(430f, birdVy + GRAVITY * dt)
        birdY += birdVy * dt

        val speed = wallSpeed()
        walls.forEach { wall ->
            wall.x -= speed * dt
            if (wall.gapCenter != null) wall.openAge += dt
            wall.missAge = max(0f, wall.missAge - dt)
        }

        val pulseIterator = pulses.iterator()
        while (pulseIterator.hasNext()) {
            val pulse = pulseIterator.next()
            pulse.x += PULSE_SPEED * dt
            val wall = walls.firstOrNull { it.id == pulse.targetId }
            if (wall == null) {
                pulseIterator.remove()
            } else if (pulse.x >= wall.x) {
                if (wall.x > BIRD_X + BIRD_RADIUS && wall.gapCenter == null) {
                    if (abs(pulse.y - wall.weakCenter) <= wall.weakHalfHeight) {
                        val halfGap = gapHeight(wall) / 2f
                        wall.gapCenter = pulse.y.coerceIn(TOP + halfGap + 16f, BOTTOM - halfGap - 16f)
                        wall.openAge = 0f
                        lastCutY = wall.gapCenter!!
                        burst(wall.x, wall.gapCenter!!)
                        events += GameEvent.OPEN
                    } else {
                        wall.missAge = 0.42f
                        burst(wall.x, pulse.y.coerceIn(TOP, BOTTOM), warm = true)
                        events += GameEvent.MISS
                    }
                }
                wall.pulsePending = false
                pulseIterator.remove()
            }
        }

        sparks.forEach {
            it.x += it.vx * dt
            it.y += it.vy * dt
            it.vy += 130f * dt
            it.life -= dt
        }
        sparks.removeAll { it.life <= 0f }

        if (birdY - BIRD_RADIUS < TOP || birdY + BIRD_RADIUS > BOTTOM || walls.any(::hitsWall)) {
            phase = GamePhase.OVER
            events += GameEvent.HIT
            return
        }

        walls.forEach { wall ->
            if (!wall.scored && wall.x + WALL_WIDTH < BIRD_X - BIRD_RADIUS) {
                wall.scored = true
                score++
                best = max(best, score)
                events += GameEvent.SCORE
            }
        }
        walls.removeAll { it.x + WALL_WIDTH < -40f }
        while (walls.last().x < WIDTH + 560f) {
            val spacing = 300f + random.nextInt(0, 3) * 18f
            val center = (walls.last().weakCenter + random.nextInt(-68, 69)).coerceIn(TOP + 120f, BOTTOM - 120f)
            val progress = dawnProgress()
            walls += Wall(
                nextWallId++, walls.last().x + spacing,
                weakCenter = center,
                weakHalfHeight = 80f - 44f * progress,
                openingHeight = 164f - 32f * progress,
            )
        }
        if (elapsed >= winSeconds) {
            phase = GamePhase.WON
            events += GameEvent.WIN
        }
    }

    fun gapHeight(wall: Wall): Float = wall.openingHeight

    private fun hitsWall(wall: Wall): Boolean {
        val left = wall.x
        val right = wall.x + WALL_WIDTH
        val gap = wall.gapCenter
        return if (gap == null) {
            circleRect(BIRD_X, birdY, BIRD_RADIUS, left, TOP, right, BOTTOM)
        } else {
            val half = gapHeight(wall) / 2f
            circleRect(BIRD_X, birdY, BIRD_RADIUS, left, TOP, right, gap - half) ||
                circleRect(BIRD_X, birdY, BIRD_RADIUS, left, gap + half, right, BOTTOM)
        }
    }

    private fun circleRect(cx: Float, cy: Float, radius: Float, left: Float, top: Float, right: Float, bottom: Float): Boolean {
        val dx = cx - cx.coerceIn(left, right)
        val dy = cy - cy.coerceIn(top, bottom)
        return dx * dx + dy * dy < radius * radius
    }

    private fun burst(x: Float, y: Float, warm: Boolean = false) {
        repeat(14) { index ->
            val angle = index * 6.283185f / 14f
            val force = 55f + random.nextFloat() * 105f
            sparks += Spark(
                x, y,
                kotlin.math.cos(angle) * force,
                kotlin.math.sin(angle) * force,
                0.4f + random.nextFloat() * 0.45f,
                warm,
            )
        }
    }
}
