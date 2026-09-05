package com.nahtygal.olivialooi.games.puzzles

enum class PuzzlePicture(val stableId: String, val displayName: String, val spokenName: String) {
    BUTTERFLY("butterfly", "Butterfly", "butterfly"),
    PUPPY("puppy", "Puppy", "puppy"),
    SNOWMAN("snowman", "Snowman", "snowman"),
    FLOWER("flower", "Flower", "flower"),
    RAINBOW("rainbow", "Rainbow", "rainbow"),
    HOUSE("house", "House", "house"),
}

object PuzzleCatalog {
    val pictures: List<PuzzlePicture> = PuzzlePicture.entries
    fun findById(id: String): PuzzlePicture? = pictures.find { it.stableId == id }
}

enum class PuzzleDifficulty(val columns: Int, val rows: Int, val displayName: String) {
    EASY(2, 3, "Easy"),
    MEDIUM(3, 3, "Medium"),
    HARD(3, 4, "Hard");

    val pieceCount: Int get() = rows * columns
}
