package com.skywright.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCoreTest {
    @Test fun quickTapFlapsWithoutFiring() {
        val game = GameCore()
        game.start()
        game.beginPress()
        game.endPress(0.1f)
        assertTrue(game.pulses.isEmpty())
        assertEquals(0f, game.shotCooldown, 0.001f)
        assertTrue(GameEvent.FLAP in game.events)
    }

    @Test fun releaseFiresAtCurrentHeightButOpensAtTargetCenter() {
        val game = GameCore()
        game.start()
        game.beginPress()
        repeat(12) { game.update(1f / 60f) }
        assertTrue(game.gliding)
        val sampled = game.birdY
        game.endPress(0.2f)
        assertFalse(game.gliding)
        assertEquals(sampled, game.pulses.single().y, 0.01f)
        assertTrue(game.shotCooldown > 0f)
        assertTrue(GameEvent.FIRE in game.events)
        repeat(25) { game.update(1f / 60f) }
        assertEquals(game.walls.first().weakCenter, game.walls.first().gapCenter!!, 0.01f)
        assertNotEquals(sampled, game.walls.first().gapCenter!!)
        assertNotEquals(sampled, game.birdY)
    }

    @Test fun aWallAcceptsOnlyOnePulseAndAnotherHoldCannotShootWhileTheFirstIsInFlight() {
        val game = GameCore()
        game.start()
        game.beginPress()
        game.endPress(0.2f)
        assertEquals(1, game.pulses.size)
        game.beginPress()
        game.endPress(0.2f)
        assertEquals(1, game.pulses.size)
        repeat(25) { game.update(1f / 60f) }
        val first = game.walls.first()
        val opening = first.gapCenter
        game.beginPress()
        game.endPress(0.2f)
        assertEquals(opening, first.gapCenter)
        assertFalse(first.pulsePending)
        assertTrue(game.pulses.isEmpty())
        assertTrue(game.shotCooldown > 0f)
    }

    @Test fun pausingDuringAGlideCancelsThePendingShot() {
        val game = GameCore()
        game.start()
        game.beginPress()
        repeat(12) { game.update(1f / 60f) }
        assertTrue(game.gliding)
        game.pause()
        game.endPress(1f)
        assertFalse(game.gliding)
        assertTrue(game.pulses.isEmpty())
        game.resume()
        assertEquals(GamePhase.PLAYING, game.phase)
    }

    @Test fun glideBuildsWorldSpeedAndReturnsToTheSunrisePaceAfterRelease() {
        val game = GameCore()
        game.start()
        game.beginPress()
        repeat(40) { game.update(1f / 60f) }
        assertTrue(game.gliding)
        assertTrue(game.glideBoost > 0f)
        assertTrue(game.wallSpeed() > game.baseWallSpeed())
        game.endPress(40f / 60f)
        val boostedSpeed = game.wallSpeed()
        repeat(20) { game.update(1f / 60f) }
        assertTrue(game.wallSpeed() < boostedSpeed)
        assertEquals(0f, game.glideBoost, 0.001f)
        assertEquals(game.baseWallSpeed(), game.wallSpeed(), 0.001f)
    }

    @Test fun aPulseOutsideTheWeakBandDoesNotOpenTheWallAndCanBeRetried() {
        val game = GameCore()
        game.walls.clear()
        game.walls += Wall(99, 340f, weakCenter = 500f, weakHalfHeight = 30f)
        game.start()
        game.beginPress()
        game.endPress(0.2f)
        repeat(25) { game.update(1f / 60f) }
        assertEquals(null, game.walls.first().gapCenter)
        assertTrue(GameEvent.MISS in game.events)
        assertFalse(game.walls.first().pulsePending)
        game.beginPress()
        game.endPress(0.2f)
        assertTrue(game.pulses.isEmpty())
        repeat(30) { game.update(1f / 60f) }
        game.beginPress()
        game.endPress(0.2f)
        assertEquals(99, game.pulses.single().targetId)
    }

    @Test fun pulseThatArrivesAfterWallIsTooCloseCannotSaveBird() {
        val game = GameCore()
        game.walls.clear()
        game.walls += Wall(99, 109f)
        game.start()
        game.beginPress()
        game.endPress(0.2f)
        game.update(1f / 60f)
        assertEquals(null, game.walls.first().gapCenter)
        assertEquals(GamePhase.OVER, game.phase)
    }

    @Test fun openGapAllowsPassageAndScoresExactlyOnce() {
        val game = GameCore()
        game.walls.clear()
        game.walls += Wall(99, 30f, gapCenter = 350f)
        game.start()
        repeat(3) { game.update(1f / 60f) }
        assertEquals(GamePhase.PLAYING, game.phase)
        assertEquals(1, game.score)
    }

    @Test fun solidWallKillsAndPauseFreezesSimulation() {
        val game = GameCore()
        game.start()
        game.pause()
        val y = game.birdY
        game.update(0.5f)
        assertEquals(y, game.birdY)
        game.resume()
        game.walls.clear()
        game.walls += Wall(99, 105f)
        game.update(1f / 60f)
        assertEquals(GamePhase.OVER, game.phase)
        assertTrue(GameEvent.HIT in game.events)
    }

    @Test fun aimingAtVisibleWeakBandsCanClearSeveralWalls() {
        val game = GameCore()
        game.start()
        var failureState = ""
        var heldFrames = 0
        repeat(600) { frame ->
            heldFrames = guide(game, frame, heldFrames)
            game.update(1f / 60f)
            if (game.phase == GamePhase.OVER && failureState.isEmpty()) {
                val wall = game.walls.firstOrNull { it.x + GameCore.WALL_WIDTH >= GameCore.BIRD_X - GameCore.BIRD_RADIUS }
                failureState = "frame=$frame score=${game.score} y=${game.birdY} wall=$wall"
            }
        }
        assertEquals(failureState, GamePhase.PLAYING, game.phase)
        assertTrue(game.score >= 3)
    }

    @Test fun sunriseAdvancesWithPlayTimeAndEndsTheRunAtFiveMinuteEquivalent() {
        val game = GameCore(winSeconds = 0.5f)
        assertEquals(110f, game.wallSpeed(), 0.001f)
        game.start()
        repeat(30) { game.update(1f / 60f) }
        assertEquals(GamePhase.WON, game.phase)
        assertEquals(1f, game.dawnProgress(), 0.001f)
        assertEquals(210f, game.wallSpeed(), 0.001f)
        assertTrue(GameEvent.WIN in game.events)
    }

    @Test fun aGuidedRouteCanReachTheActualFiveMinuteSunrise() {
        val game = GameCore()
        game.start()
        var failureState = ""
        var heldFrames = 0
        repeat(18_100) { frame ->
            if (game.phase == GamePhase.PLAYING) {
                heldFrames = guide(game, frame, heldFrames)
                game.update(1f / 60f)
                if (game.phase == GamePhase.OVER) {
                    failureState = "frame=$frame score=${game.score} y=${game.birdY} vy=${game.birdVy} held=$heldFrames cooldown=${game.shotCooldown} target=${game.targetWall()} wall=${game.walls.firstOrNull()}"
                }
            }
        }
        assertEquals(failureState, GamePhase.WON, game.phase)
        assertTrue(game.score > 100)
    }

    /** A simple one-thumb controller: quick flaps for height, a real 0.2 s hold to aim. */
    private fun guide(game: GameCore, frame: Int, heldFrames: Int): Int {
        if (heldFrames > 0) {
            if (heldFrames >= 12) {
                game.endPress(heldFrames / 60f)
                return 0
            }
            return heldFrames + 1
        }
        val wall = game.walls.firstOrNull {
            it.x + GameCore.WALL_WIDTH >= GameCore.BIRD_X - GameCore.BIRD_RADIUS && it.x <= GameCore.WIDTH
        }
        val desiredY = wall?.gapCenter ?: wall?.weakCenter?.plus(35f) ?: 350f
        val target = game.targetWall()
        if (target != null && game.canFire() &&
            game.birdY in (target.weakCenter + 20f)..(target.weakCenter + 70f)) {
            game.beginPress()
            return 1
        }
        if (frame > 0 && game.birdY > desiredY + 8f && game.birdVy > 0f) game.flap()
        return 0
    }
}
