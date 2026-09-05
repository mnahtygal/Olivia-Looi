package com.nahtygal.olivialooi.ui.games.puzzles

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.games.puzzles.PuzzlePicture
import com.nahtygal.olivialooi.games.puzzles.PuzzlePiece

private data class PaintedPath(val path: Path, val color: Color)

/** All six original scenes share a square 600-unit artboard. Paths are built once, not per drag. */
private object PuzzleArtwork {
    val scenes: Map<PuzzlePicture, List<PaintedPath>> by lazy {
        PuzzlePicture.entries.associateWith { picture -> Scene().apply { picture(picture) }.marks.toList() }
    }

    private class Scene {
        val marks = mutableListOf<PaintedPath>()
        fun path(color: Long, block: Path.() -> Unit) { marks += PaintedPath(Path().apply(block), Color(color)) }
        fun oval(x: Float, y: Float, w: Float, h: Float, color: Long) = path(color) { addOval(Rect(x, y, x + w, y + h)) }
        fun circle(x: Float, y: Float, r: Float, color: Long) = oval(x - r, y - r, r * 2, r * 2, color)
        fun rect(x: Float, y: Float, w: Float, h: Float, color: Long) = path(color) { addRect(Rect(x, y, x + w, y + h)) }
        fun cloud(x: Float, y: Float) {
            oval(x, y + 20, 125f, 48f, 0xFFFFFFFF)
            circle(x + 35, y + 25, 30f, 0xFFFFFFFF)
            circle(x + 76, y + 18, 37f, 0xFFFFFFFF)
        }
        fun ground(color: Long = 0xFF78C75C) = path(color) {
            moveTo(0f, 490f); cubicTo(150f, 440f, 380f, 535f, 600f, 470f)
            lineTo(600f, 600f); lineTo(0f, 600f); close()
        }
        fun blossom(x: Float, y: Float, size: Float, color: Long) {
            circle(x - size, y, size, color); circle(x + size, y, size, color)
            circle(x, y - size, size, color); circle(x, y + size, size, color)
            circle(x, y, size * .72f, 0xFFFFD846)
        }
        fun picture(picture: PuzzlePicture) {
            rect(0f, 0f, 600f, 600f, if (picture == PuzzlePicture.SNOWMAN) 0xFF9ACFF4 else 0xFFB9E9FF)
            when (picture) {
                PuzzlePicture.BUTTERFLY -> {
                    circle(545f, 65f, 54f, 0xFFFFD546); cloud(5f, 15f); ground()
                    path(0xFFF04FA1) {
                        moveTo(295f, 255f); cubicTo(155f, 22f, 12f, 78f, 60f, 257f)
                        cubicTo(78f, 340f, 212f, 365f, 295f, 305f); close()
                    }
                    path(0xFFFF9D38) {
                        moveTo(305f, 255f); cubicTo(445f, 22f, 588f, 78f, 540f, 257f)
                        cubicTo(522f, 340f, 388f, 365f, 305f, 305f); close()
                    }
                    path(0xFF8650D1) {
                        moveTo(290f, 298f); cubicTo(75f, 264f, 78f, 495f, 193f, 490f)
                        cubicTo(263f, 488f, 280f, 395f, 290f, 298f); close()
                    }
                    path(0xFF257FDE) {
                        moveTo(310f, 298f); cubicTo(525f, 264f, 522f, 495f, 407f, 490f)
                        cubicTo(337f, 488f, 320f, 395f, 310f, 298f); close()
                    }
                    oval(105f, 138f, 103f, 115f, 0xFFFFDB4D); oval(392f, 138f, 103f, 115f, 0xFFFFEE8E)
                    circle(185f, 401f, 38f, 0xFFF7AEE4); circle(415f, 401f, 38f, 0xFF80E6EE)
                    oval(279f, 190f, 42f, 248f, 0xFF44365F)
                    path(0xFF44365F) { moveTo(286f, 211f); lineTo(250f, 140f); lineTo(262f, 133f); lineTo(300f, 207f); close() }
                    path(0xFF44365F) { moveTo(300f, 207f); lineTo(338f, 133f); lineTo(350f, 140f); lineTo(314f, 211f); close() }
                    circle(254f, 135f, 13f, 0xFF44365F); circle(346f, 135f, 13f, 0xFF44365F)
                    blossom(65f, 550f, 22f, 0xFFF04FA1); blossom(526f, 541f, 27f, 0xFF8650D1)
                }
                PuzzlePicture.PUPPY -> {
                    circle(62f, 62f, 48f, 0xFFFFDA4A); cloud(432f, 37f); ground()
                    path(0xFFA96332) { moveTo(392f, 430f); cubicTo(553f, 410f, 533f, 283f, 485f, 322f); cubicTo(492f, 385f, 418f, 357f, 392f, 372f); close() }
                    oval(187f, 315f, 230f, 224f, 0xFFC5884C)
                    oval(195f, 401f, 73f, 157f, 0xFFFBE0B3); oval(332f, 401f, 73f, 157f, 0xFFFBE0B3)
                    oval(83f, 117f, 131f, 250f, 0xFF87502E); oval(386f, 117f, 131f, 250f, 0xFF87502E)
                    oval(142f, 78f, 316f, 297f, 0xFFC5884C)
                    oval(164f, 132f, 112f, 122f, 0xFFFBE0B3)
                    circle(230f, 207f, 20f, 0xFF302D3C); circle(370f, 207f, 20f, 0xFF302D3C)
                    circle(235f, 201f, 6f, 0xFFFFFFFF); circle(375f, 201f, 6f, 0xFFFFFFFF)
                    oval(213f, 238f, 174f, 119f, 0xFFFBE0B3)
                    oval(269f, 247f, 62f, 41f, 0xFF302D3C)
                    oval(280f, 305f, 40f, 62f, 0xFFEF7097)
                    rect(208f, 368f, 185f, 29f, 0xFF2479DE); circle(300f, 400f, 23f, 0xFFFFD442)
                    circle(74f, 526f, 41f, 0xFFF15D6A); rect(34f, 517f, 80f, 17f, 0xFFFFDB4D)
                    blossom(526f, 557f, 18f, 0xFFA050D7)
                }
                PuzzlePicture.SNOWMAN -> {
                    circle(517f, 75f, 43f, 0xFFFFF2A9); ground(0xFFEFFAFF)
                    path(0xFF388875) { moveTo(63f, 220f); lineTo(5f, 426f); lineTo(135f, 426f); close() }
                    rect(59f, 426f, 18f, 75f, 0xFF865234)
                    path(0xFF865234) { moveTo(220f, 343f); lineTo(92f, 288f); lineTo(84f, 305f); lineTo(213f, 365f); close() }
                    path(0xFF865234) { moveTo(378f, 344f); lineTo(513f, 266f); lineTo(522f, 283f); lineTo(387f, 365f); close() }
                    circle(300f, 434f, 122f, 0xFFFFFFFF); circle(300f, 306f, 98f, 0xFFFFFFFF); circle(300f, 196f, 76f, 0xFFFFFFFF)
                    rect(222f, 62f, 153f, 99f, 0xFF3D3C65); rect(197f, 150f, 204f, 23f, 0xFF3D3C65)
                    rect(222f, 128f, 153f, 22f, 0xFFEB5386)
                    circle(274f, 195f, 8f, 0xFF34334E); circle(325f, 195f, 8f, 0xFF34334E)
                    path(0xFFFF9531) { moveTo(300f, 208f); lineTo(300f, 230f); lineTo(366f, 221f); close() }
                    circle(281f, 239f, 5f, 0xFF34334E); circle(297f, 246f, 5f, 0xFF34334E); circle(314f, 239f, 5f, 0xFF34334E)
                    rect(220f, 261f, 163f, 31f, 0xFFEC5185); rect(344f, 279f, 33f, 91f, 0xFFEC5185)
                    listOf(326f, 392f, 463f).forEach { circle(300f, it, 12f, 0xFF3D3C65) }
                    circle(67f, 94f, 14f, 0xFFFFFFFF); circle(140f, 169f, 10f, 0xFFFFFFFF)
                    circle(496f, 374f, 16f, 0xFFFFFFFF); circle(546f, 516f, 22f, 0xFFB8DDF6)
                    oval(28f, 550f, 123f, 26f, 0xFFB8DDF6)
                }
                PuzzlePicture.FLOWER -> {
                    circle(532f, 62f, 48f, 0xFFFFD647); cloud(0f, 20f); ground()
                    rect(284f, 275f, 32f, 245f, 0xFF277F4C)
                    path(0xFF349F56) { moveTo(290f, 445f); cubicTo(105f, 464f, 106f, 320f, 132f, 328f); cubicTo(231f, 331f, 273f, 388f, 290f, 445f); close() }
                    path(0xFF56B956) { moveTo(313f, 468f); cubicTo(489f, 481f, 499f, 346f, 472f, 343f); cubicTo(377f, 347f, 338f, 415f, 313f, 468f); close() }
                    circle(300f, 136f, 76f, 0xFFF160AB); circle(205f, 202f, 76f, 0xFFDC4B9B)
                    circle(395f, 202f, 76f, 0xFFFA87C2); circle(244f, 306f, 76f, 0xFFF160AB)
                    circle(356f, 306f, 76f, 0xFFDC4B9B); circle(300f, 235f, 72f, 0xFFFFD13D)
                    circle(278f, 223f, 7f, 0xFF6C482E); circle(322f, 223f, 7f, 0xFF6C482E)
                    oval(282f, 250f, 36f, 15f, 0xFF6C482E)
                    path(0xFFD67640) { moveTo(224f, 489f); lineTo(376f, 489f); lineTo(351f, 587f); lineTo(249f, 587f); close() }
                    rect(211f, 482f, 178f, 28f, 0xFFFFAB63)
                    blossom(70f, 550f, 18f, 0xFF975DE0); circle(533f, 540f, 24f, 0xFFEC5D58)
                }
                PuzzlePicture.RAINBOW -> {
                    circle(78f, 65f, 51f, 0xFFFFD544); cloud(421f, 20f)
                    // Nested upper half-discs form seven crisp bands with no bitmap masks.
                    val colors = listOf(0xFFEF5363, 0xFFFF963C, 0xFFFFD94A, 0xFF52BB63, 0xFF318EDD, 0xFF6651BE, 0xFFA563CF, 0xFFB9E9FF)
                    colors.forEachIndexed { index, color ->
                        val radius = 285f - index * 27f
                        path(color) {
                            moveTo(300f - radius, 465f)
                            arcTo(Rect(300f - radius, 465f - radius, 300f + radius, 465f + radius), 180f, 180f, false)
                            close()
                        }
                    }
                    cloud(0f, 417f); cloud(464f, 417f); ground()
                    blossom(142f, 551f, 23f, 0xFFED68A7); blossom(453f, 542f, 29f, 0xFF9561D4)
                    oval(238f, 541f, 106f, 28f, 0xFF4BAE5A)
                }
                PuzzlePicture.HOUSE -> {
                    circle(533f, 66f, 48f, 0xFFFFD647); cloud(5f, 12f); ground()
                    rect(389f, 94f, 48f, 150f, 0xFFAC5949)
                    rect(116f, 251f, 372f, 273f, 0xFFFFDA84)
                    path(0xFFEE5A70) { moveTo(67f, 266f); lineTo(300f, 70f); lineTo(539f, 266f); close() }
                    path(0xFFD74259) { moveTo(300f, 70f); lineTo(539f, 266f); lineTo(502f, 266f); lineTo(300f, 100f); lineTo(103f, 266f); lineTo(67f, 266f); close() }
                    circle(301f, 193f, 39f, 0xFFFFF2C6); circle(301f, 193f, 26f, 0xFF4C9BE2)
                    rect(151f, 314f, 88f, 83f, 0xFFFFFFFF); rect(363f, 314f, 88f, 83f, 0xFFFFFFFF)
                    rect(158f, 321f, 74f, 69f, 0xFF54B8EA); rect(370f, 321f, 74f, 69f, 0xFF54B8EA)
                    rect(190f, 314f, 9f, 83f, 0xFFFFFFFF); rect(402f, 314f, 9f, 83f, 0xFFFFFFFF)
                    rect(151f, 351f, 88f, 9f, 0xFFFFFFFF); rect(363f, 351f, 88f, 9f, 0xFFFFFFFF)
                    rect(262f, 365f, 78f, 159f, 0xFF8860C3); circle(322f, 448f, 7f, 0xFFFFD648)
                    path(0xFFF1CDA3) { moveTo(262f, 524f); lineTo(340f, 524f); lineTo(400f, 600f); lineTo(201f, 600f); close() }
                    rect(34f, 381f, 24f, 157f, 0xFF9C653C); circle(47f, 343f, 57f, 0xFF49AC65)
                    blossom(536f, 543f, 24f, 0xFFF46CA8); blossom(115f, 566f, 18f, 0xFF9861D3)
                }
            }
        }
    }
}

/** Cropping scales/translates the same original art, then clips at the tile's bounds.
 * Tiles keep a square full-picture aspect: tile width/height = rows/columns.
 */
@Composable
fun PuzzleImage(
    picture: PuzzlePicture,
    modifier: Modifier = Modifier,
    piece: PuzzlePiece? = null,
    difficulty: PuzzleDifficulty = PuzzleDifficulty.EASY,
) {
    val marks = PuzzleArtwork.scenes.getValue(picture)
    Canvas(modifier.drawWithCache {
        val columns = if (piece == null) 1 else difficulty.columns
        val rows = if (piece == null) 1 else difficulty.rows
        val scaleX = size.width * columns / 600f
        val scaleY = size.height * rows / 600f
        onDrawBehind {
            clipRect {
                withTransform({
                    translate(-(piece?.sourceColumn ?: 0) * size.width, -(piece?.sourceRow ?: 0) * size.height)
                    scale(scaleX, scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero)
                }) { marks.forEach { drawPath(it.path, it.color) } }
            }
        }
    }) { }
}

@Composable
fun PuzzleGameGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier.drawWithCache {
        val path = Path().apply {
            moveTo(.16f, .2f); lineTo(.4f, .2f)
            cubicTo(.33f, -.01f, .67f, -.01f, .6f, .2f)
            lineTo(.83f, .2f); lineTo(.83f, .43f)
            cubicTo(1.04f, .36f, 1.04f, .7f, .83f, .63f)
            lineTo(.83f, .88f); lineTo(.16f, .88f); lineTo(.16f, .63f)
            cubicTo(.37f, .7f, .37f, .36f, .16f, .43f); close()
        }
        onDrawBehind {
            withTransform({ scale(size.width, size.height, pivot = androidx.compose.ui.geometry.Offset.Zero) }) {
                drawPath(path, Color(0xFF8B5BCD))
                drawCircle(Color(0xFFFFD95A), .13f, androidx.compose.ui.geometry.Offset(.57f, .53f))
            }
        }
    }) { }
}
