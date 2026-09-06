package com.nahtygal.olivialooi.ui.home

import kotlin.math.min

enum class HomeArtworkAction { TALK, GAMES, MUSIC, STORIES, LEARN }
enum class HomeActivity { PIANO, DRUMS, ABC, MATH, COUNTING, SHAPES, ANIMALS, SPELLING }

data class ArtworkRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun contains(x: Float, y: Float) = x >= left && x < left + width && y >= top && y < top + height
}

/** Coordinates are fractions of the approved bitmap, never screen coordinates. */
object HomeArtwork {
    const val ASPECT_RATIO = 1145f / 1374f
    private val regions = mapOf(
        HomeArtworkAction.TALK to ArtworkRect(.235f, .735f, .53f, .11f),
        HomeArtworkAction.GAMES to ArtworkRect(.16f, .86f, .16f, .125f),
        HomeArtworkAction.MUSIC to ArtworkRect(.33f, .86f, .16f, .125f),
        HomeArtworkAction.STORIES to ArtworkRect(.50f, .86f, .17f, .125f),
        HomeArtworkAction.LEARN to ArtworkRect(.68f, .86f, .17f, .125f),
    )
    fun fit(width: Float, height: Float): ArtworkRect {
        require(width.isFinite() && height.isFinite() && width >= 0 && height >= 0)
        val imageWidth = min(width, height * ASPECT_RATIO)
        val imageHeight = imageWidth / ASPECT_RATIO
        return ArtworkRect((width - imageWidth) / 2, (height - imageHeight) / 2, imageWidth, imageHeight)
    }
    fun region(action: HomeArtworkAction, image: ArtworkRect): ArtworkRect = regions.getValue(action).let {
        ArtworkRect(image.left + it.left * image.width, image.top + it.top * image.height, it.width * image.width, it.height * image.height)
    }
    fun actionAt(x: Float, y: Float, image: ArtworkRect): HomeArtworkAction? =
        HomeArtworkAction.entries.firstOrNull { region(it, image).contains(x, y) }
}
