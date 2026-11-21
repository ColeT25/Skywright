package com.skywright.game

internal data class LevelWall(
    val x: Float,
    val center: Float,
    val wind: Int = 0,
)

enum class FlightLevel(
    val title: String,
    internal val speed: Float,
    internal val openingHeight: Float,
    internal val walls: List<LevelWall>,
) {
    FIRST_FLIGHT(
        "First Flight",
        110f,
        220f,
        listOf(
            LevelWall(320f, 350f),
            LevelWall(740f, 365f),
            LevelWall(1160f, 345f),
            LevelWall(1580f, 355f),
            LevelWall(2000f, 340f),
            LevelWall(2420f, 350f),
        ),
    ),
    PAPER_MAZE(
        "Paper Maze",
        125f,
        172f,
        listOf(
            LevelWall(320f, 350f),
            LevelWall(740f, 283f),
            LevelWall(1160f, 383f),
            LevelWall(1580f, 313f),
            LevelWall(2000f, 397f),
            LevelWall(2420f, 308f),
            LevelWall(2840f, 365f),
        ),
    ),
    CROSSWINDS(
        "Crosswinds",
        135f,
        184f,
        listOf(
            LevelWall(320f, 350f),
            LevelWall(760f, 358f),
            LevelWall(1380f, 310f, wind = -1),
            LevelWall(1840f, 358f),
            LevelWall(2460f, 402f, wind = 1),
            LevelWall(2920f, 344f),
            LevelWall(3380f, 355f),
        ),
    ),
    ;

    val wallCount: Int get() = walls.size
    val next: FlightLevel? get() = entries.getOrNull(ordinal + 1)
}
