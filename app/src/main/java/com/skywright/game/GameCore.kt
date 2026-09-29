package com.skywright.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.random.Random

enum class GamePhase { READY, PLAYING, PAUSED, OVER, WON }
enum class GameEvent { FLAP, FIRE, OPEN, MISS, SCORE, HIT, WIN }

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
        const val HOLD_SECONDS = 0.18f
        const val SHOT_COOLDOWN = 0.9f
        const val GLIDE_GRAVITY = 350f
        const val GLIDE_FALL_LIMIT = 185f
        const val GLIDE_BOOST_MAX = 40f
        const val GLIDE_BOOST_ACCEL = 30f
        const val GLIDE_BOOST_DECEL = 100f
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
    var distanceTravelled = 0f
        private set
    var guideY = 350f
        private set
    var guideLife = 0f
        private set
    var lastCutY = 350f
        private set
    var gliding = false
        private set
    var shotCooldown = 0f
        private set
    var glideBoost = 0f
        private set
    private var pressed = false
    private var pressTime = 0f

    val walls = mutableListOf<Wall>()
    val pulses = mutableListOf<Pulse>()
    val sparks = mutableListOf<Spark>()
    val events = mutableListOf<GameEvent>()
    private var nextWallId = 0

    init { reset() }

    fun dawnProgress(): Float = (elapsed / winSeconds).coerceIn(0f, 1f)
    fun baseWallSpeed(): Float = 110f + 100f * dawnProgress()
    fun wallSpeed(): Float = baseWallSpeed() + glideBoost
    fun secondsToDawn(): Int = kotlin.math.ceil((winSeconds - elapsed).coerceAtLeast(0f)).toInt()

    fun reset() {
        phase = GamePhase.READY
        birdY = 350f
        birdVy = 0f
        score = 0
        elapsed = 0f
        distanceTravelled = 0f
        guideLife = 0f
        pressed = false
        gliding = false
        pressTime = 0f
        shotCooldown = 0f
        glideBoost = 0f
        walls.clear()
        pulses.clear()
        sparks.clear()
        events.clear()
        nextWallId = 0
        val introCenters = floatArrayOf(350f, 392f, 328f)
        repeat(3) { index ->
            walls += Wall(nextWallId++, 340f + index * 400f, weakCenter = introCenters[index])
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
        if (phase == GamePhase.PLAYING) {
            cancelPress()
            phase = GamePhase.PAUSED
        }
    }

    fun resume() {
        if (phase == GamePhase.PAUSED) phase = GamePhase.PLAYING
    }

    /** A touch starts with a flap; after the hold threshold it becomes a glide. */
    fun beginPress() {
        if (pressed || phase == GamePhase.PAUSED) return
        when (phase) {
            GamePhase.READY -> start()
            GamePhase.OVER, GamePhase.WON -> restart()
            GamePhase.PLAYING -> flap()
            GamePhase.PAUSED -> return
        }
        pressed = true
        pressTime = 0f
        gliding = false
    }

    /** The touch duration is supplied by Android so short, dropped frames do not misclassify a hold. */
    fun endPress(heldSeconds: Float) {
        if (!pressed) return
        val wasGlide = phase == GamePhase.PLAYING && (gliding || heldSeconds >= HOLD_SECONDS)
        cancelPress()
        if (wasGlide) fire()
    }

    fun cancelPress() {
        pressed = false
        pressTime = 0f
        gliding = false
    }

    /** Quick taps and the start of holds move the moth without spending a shot. */
    fun flap() {
        if (phase != GamePhase.PLAYING) return
        birdVy = FLAP_SPEED
        events += GameEvent.FLAP
    }

    fun targetWall(): Wall? = walls.firstOrNull {
        it.x > BIRD_X + BIRD_RADIUS && it.x <= WIDTH && it.gapCenter == null
    }

    fun canFire(): Boolean = phase == GamePhase.PLAYING && shotCooldown <= 0f &&
        pulses.isEmpty() && targetWall()?.pulsePending == false

    private fun fire() {
        if (!canFire()) return
        val target = targetWall() ?: return
        target.pulsePending = true
        pulses += Pulse(target.id, BIRD_X + 14f, birdY)
        guideY = birdY
        guideLife = 0.8f
        shotCooldown = SHOT_COOLDOWN
        events += GameEvent.FIRE
    }

    fun update(dt: Float) {
        if (phase != GamePhase.PLAYING || dt <= 0f) return
        elapsed += dt
        guideLife = max(0f, guideLife - dt)
        shotCooldown = max(0f, shotCooldown - dt)
        if (pressed && !gliding) {
            pressTime += dt
            if (pressTime >= HOLD_SECONDS) {
                gliding = true
                birdVy = max(birdVy, -105f)
            }
        }
        glideBoost = if (gliding) {
            min(GLIDE_BOOST_MAX, glideBoost + GLIDE_BOOST_ACCEL * dt)
        } else {
            max(0f, glideBoost - GLIDE_BOOST_DECEL * dt)
        }
        birdVy = min(if (gliding) GLIDE_FALL_LIMIT else 430f,
            birdVy + (if (gliding) GLIDE_GRAVITY else GRAVITY) * dt)
        birdY += birdVy * dt

        val speed = wallSpeed()
        distanceTravelled += speed * dt
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
                        wall.gapCenter = wall.weakCenter
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
            cancelPress()
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
            val spacing = 390f + random.nextInt(0, 3) * 18f
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
            cancelPress()
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
