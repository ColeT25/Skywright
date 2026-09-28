package com.skywright.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCoreTest {
    @Test fun pulseUsesHeightAtTapRatherThanHeightAtArrival() {
        val game = GameCore()
        game.start()
        val sampled = game.pulses.single().y
        repeat(25) { game.update(1f / 60f) }
        assertEquals(sampled, game.walls.first().gapCenter!!, 0.01f)
        assertNotEquals(sampled, game.birdY)
    }

    @Test fun aWallAcceptsOnlyOnePulse() {
        val game = GameCore()
        game.start()
        game.flap()
        assertEquals(1, game.pulses.size)
        repeat(25) { game.update(1f / 60f) }
        val first = game.walls.first()
        val opening = first.gapCenter
        game.flap()
        assertEquals(opening, first.gapCenter)
        assertFalse(first.pulsePending)
    }

    @Test fun aPulseOutsideTheWeakBandDoesNotOpenTheWallAndCanBeRetried() {
        val game = GameCore()
        game.walls.clear()
        game.walls += Wall(99, 340f, weakCenter = 500f, weakHalfHeight = 30f)
        game.start()
        repeat(25) { game.update(1f / 60f) }
        assertEquals(null, game.walls.first().gapCenter)
        assertTrue(GameEvent.MISS in game.events)
        assertFalse(game.walls.first().pulsePending)
        game.flap()
        assertEquals(99, game.pulses.single().targetId)
    }

    @Test fun pulseThatArrivesAfterWallIsTooCloseCannotSaveBird() {
        val game = GameCore()
        game.walls.clear()
        game.walls += Wall(99, 109f)
        game.start()
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
        repeat(600) { frame ->
            val target = game.walls.firstOrNull {
                it.x + GameCore.WALL_WIDTH >= GameCore.BIRD_X - GameCore.BIRD_RADIUS && it.x <= GameCore.WIDTH
            }
            val desiredY = target?.gapCenter ?: target?.weakCenter ?: 350f
            if (frame > 0 && game.birdY > desiredY + 8f && game.birdVy > 0f) game.flap()
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
        repeat(18_100) { frame ->
            if (game.phase == GamePhase.PLAYING) {
                val wall = game.walls.firstOrNull {
                    it.x + GameCore.WALL_WIDTH >= GameCore.BIRD_X - GameCore.BIRD_RADIUS && it.x <= GameCore.WIDTH
                }
                val desiredY = wall?.gapCenter ?: wall?.weakCenter ?: 350f
                if (frame > 0 && game.birdY > desiredY + 8f && game.birdVy > 0f) game.flap()
                game.update(1f / 60f)
                if (game.phase == GamePhase.OVER) {
                    failureState = "frame=$frame score=${game.score} y=${game.birdY} wall=$wall"
                }
            }
        }
        assertEquals(failureState, GamePhase.WON, game.phase)
        assertTrue(game.score > 100)
    }
}
