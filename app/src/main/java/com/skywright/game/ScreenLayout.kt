package com.skywright.game

/** Maps the fixed simulation to a phone while exposing extra vertical space for artwork. */
class ScreenLayout private constructor(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val hudShift: Float,
) {
    fun worldX(screenX: Float) = (screenX - offsetX) / scale
    fun worldY(screenY: Float) = (screenY - offsetY) / scale

    companion object {
        fun forSize(width: Int, height: Int, safeTopPixels: Int = 0): ScreenLayout {
            require(width > 0 && height > 0)
            val scale = minOf(width / GameCore.WIDTH, height / GameCore.HEIGHT)
            val offsetX = (width - GameCore.WIDTH * scale) / 2f
            val offsetY = (height - GameCore.HEIGHT * scale) / 2f
            val top = -offsetY / scale
            // Move the HUD into the extra sky while leaving room for a camera cutout.
            val hudShift = maxOf(top.coerceAtLeast(-25f), top + safeTopPixels / scale - 4f)
            return ScreenLayout(
                scale, offsetX, offsetY,
                -offsetX / scale, top,
                (width - offsetX) / scale, (height - offsetY) / scale,
                hudShift,
            )
        }
    }
}
