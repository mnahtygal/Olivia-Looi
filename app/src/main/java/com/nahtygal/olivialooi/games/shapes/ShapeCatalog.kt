package com.nahtygal.olivialooi.games.shapes

enum class ShapeId(val stableId: String, val displayName: String, val spokenName: String, val accessibilityName: String) {
    CIRCLE("circle", "Circle", "circle", "circle"),
    SQUARE("square", "Square", "square", "square"),
    TRIANGLE("triangle", "Triangle", "triangle", "triangle"),
    STAR("star", "Star", "star", "star"),
    HEART("heart", "Heart", "heart", "heart"),
    RECTANGLE("rectangle", "Rectangle", "rectangle", "rectangle"),
}

object ShapeCatalog {
    val entries: List<ShapeId> = ShapeId.entries
    fun findById(id: String): ShapeId? = entries.find { it.stableId == id }
}

enum class ColorId(val stableId: String, val displayName: String, val spokenName: String, val accessibilityName: String) {
    RED("red", "Red", "red", "red"),
    BLUE("blue", "Blue", "blue", "blue"),
    GREEN("green", "Green", "green", "green"),
    YELLOW("yellow", "Yellow", "yellow", "yellow"),
    PURPLE("purple", "Purple", "purple", "purple"),
    PINK("pink", "Pink", "pink", "pink"),
}

object ColorCatalog {
    val entries: List<ColorId> = ColorId.entries
    fun findById(id: String): ColorId? = entries.find { it.stableId == id }
}

data class ShapeChoice(val shape: ShapeId, val color: ColorId) {
    val stableId: String get() = "${shape.stableId}:${color.stableId}"
    val accessibilityName: String get() = "${color.displayName} ${shape.accessibilityName}"
}
