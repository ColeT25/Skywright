package com.skywright.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenLayoutTest {
    @Test fun tallPhoneExposesSkyAboveAndForegroundBelowWithoutChangingWorldScale() {
        val layout = ScreenLayout.forSize(591, 1280)
        assertEquals(591f / 360f, layout.scale, 0.001f)
        assertEquals(0f, layout.offsetX, 0.001f)
        assertTrue(layout.top < 0f)
        assertTrue(layout.bottom > 720f)
        assertEquals(360f, layout.right - layout.left, 0.001f)
        assertEquals(96f, layout.worldX(layout.offsetX + 96f * layout.scale), 0.001f)
        assertEquals(350f, layout.worldY(layout.offsetY + 350f * layout.scale), 0.001f)
    }

    @Test fun cameraCutoutKeepsTheHudInsideItsSafeArea() {
        val layout = ScreenLayout.forSize(591, 1280, safeTopPixels = 56)
        val dawnLabelTop = 12f + layout.hudShift
        assertTrue(dawnLabelTop >= layout.top + 56f / layout.scale)
    }

    @Test fun shorterScreenStillFitsTheEntireSimulation() {
        val layout = ScreenLayout.forSize(360, 680)
        assertEquals(0f, layout.top, 0.001f)
        assertEquals(720f, layout.bottom, 0.001f)
        assertTrue(layout.left < 0f)
        assertTrue(layout.right > 360f)
    }
}
