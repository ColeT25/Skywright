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

    @Test fun aSteadyFlapRhythmCanClearSeveralWalls() {
        val game = GameCore()
        game.start()
        repeat(600) { frame ->
            if (frame > 0 && frame % 40 == 0) game.flap()
            game.update(1f / 60f)
        }
        assertEquals(GamePhase.PLAYING, game.phase)
        assertTrue(game.score >= 3)
    }
}
