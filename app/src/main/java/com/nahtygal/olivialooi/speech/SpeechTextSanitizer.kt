package com.nahtygal.olivialooi.speech

/** Produces natural spoken text while leaving the source text unchanged for display. */
internal object SpeechTextSanitizer {
    fun sanitize(text: String): String {
        if (text.isEmpty()) return text

        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val emojiEnd = emojiSequenceEnd(text, index)
            if (emojiEnd > index) {
                appendSeparator(result)
                index = emojiEnd
                continue
            }

            val codePoint = Character.codePointAt(text, index)
            if (isEmojiArtifact(codePoint)) {
                appendSeparator(result)
            } else {
                result.appendCodePoint(codePoint)
            }
            index += Character.charCount(codePoint)
        }

        return result.toString()
            .replace(HORIZONTAL_WHITESPACE, " ")
            .replace(SPACE_BEFORE_PUNCTUATION, "$1")
            .replace(SPACES_AROUND_LINE_BREAK, "\n")
            .trim()
    }

    private fun emojiSequenceEnd(text: String, start: Int): Int {
        keycapSequenceEnd(text, start)?.let { return it }

        var index = start
        val first = Character.codePointAt(text, index)
        if (!isEmojiBase(first, text, index)) return start
        index += Character.charCount(first)

        if (isRegionalIndicator(first) && index < text.length) {
            val second = Character.codePointAt(text, index)
            if (isRegionalIndicator(second)) index += Character.charCount(second)
            return index
        }

        index = consumeEmojiSuffix(text, index)
        if (first == BLACK_FLAG) index = consumeTagSequence(text, index)

        while (index < text.length && Character.codePointAt(text, index) == ZERO_WIDTH_JOINER) {
            val joinerEnd = index + Character.charCount(ZERO_WIDTH_JOINER)
            if (joinerEnd >= text.length) break

            val keycapEnd = keycapSequenceEnd(text, joinerEnd)
            if (keycapEnd != null) {
                index = keycapEnd
                continue
            }

            val component = Character.codePointAt(text, joinerEnd)
            if (!isEmojiBase(component, text, joinerEnd)) break
            index = joinerEnd + Character.charCount(component)
            index = consumeEmojiSuffix(text, index)
            if (component == BLACK_FLAG) index = consumeTagSequence(text, index)
        }
        return index
    }

    private fun keycapSequenceEnd(text: String, start: Int): Int? {
        val base = Character.codePointAt(text, start)
        if (base !in '0'.code..'9'.code && base != '#'.code && base != '*'.code) return null

        var index = start + Character.charCount(base)
        if (index < text.length && Character.codePointAt(text, index) in VARIATION_SELECTORS) {
            index += Character.charCount(Character.codePointAt(text, index))
        }
        if (index >= text.length || Character.codePointAt(text, index) != COMBINING_KEYCAP) return null
        return index + Character.charCount(COMBINING_KEYCAP)
    }

    private fun consumeEmojiSuffix(text: String, start: Int): Int {
        var index = start
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (codePoint !in VARIATION_SELECTORS && codePoint !in SKIN_TONES) break
            index += Character.charCount(codePoint)
        }
        return index
    }

    private fun consumeTagSequence(text: String, start: Int): Int {
        var index = start
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (codePoint !in TAG_CHARACTERS && codePoint != CANCEL_TAG) break
            index += Character.charCount(codePoint)
            if (codePoint == CANCEL_TAG) break
        }
        return index
    }

    private fun isEmojiBase(codePoint: Int, text: String, index: Int): Boolean {
        if (codePoint in DEFAULT_EMOJI_RANGES || codePoint in SUPPLEMENTARY_EMOJI_RANGES) return true
        if (codePoint in TEXT_EMOJI_RANGES) return true
        if (codePoint !in VARIATION_REQUIRED_BASES) return false

        val nextIndex = index + Character.charCount(codePoint)
        return nextIndex < text.length && Character.codePointAt(text, nextIndex) == EMOJI_VARIATION_SELECTOR
    }

    private fun isEmojiArtifact(codePoint: Int): Boolean =
        codePoint in SKIN_TONES || codePoint in VARIATION_SELECTORS ||
            codePoint in TAG_CHARACTERS || codePoint == CANCEL_TAG

    private fun isRegionalIndicator(codePoint: Int): Boolean = codePoint in REGIONAL_INDICATORS

    private fun appendSeparator(result: StringBuilder) {
        if (result.isNotEmpty() && !result.last().isWhitespace()) result.append(' ')
    }

    private operator fun Array<IntRange>.contains(value: Int): Boolean = any { value in it }

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val COMBINING_KEYCAP = 0x20E3
    private const val EMOJI_VARIATION_SELECTOR = 0xFE0F
    private const val BLACK_FLAG = 0x1F3F4
    private const val CANCEL_TAG = 0xE007F

    private val VARIATION_SELECTORS = 0xFE0E..0xFE0F
    private val SKIN_TONES = 0x1F3FB..0x1F3FF
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
    private val TAG_CHARACTERS = 0xE0020..0xE007E

    // These characters have text presentation unless U+FE0F explicitly requests emoji presentation.
    private val VARIATION_REQUIRED_BASES = arrayOf(
        0x00A9..0x00A9,
        0x00AE..0x00AE,
        0x2122..0x2122,
    )

    // Emoji with default emoji presentation in the BMP.
    private val DEFAULT_EMOJI_RANGES = arrayOf(
        0x231A..0x231B,
        0x23E9..0x23EC,
        0x23F0..0x23F0,
        0x23F3..0x23F3,
        0x25FD..0x25FE,
        0x2614..0x2615,
        0x2648..0x2653,
        0x267F..0x267F,
        0x2693..0x2693,
        0x26A1..0x26A1,
        0x26AA..0x26AB,
        0x26BD..0x26BE,
        0x26C4..0x26C5,
        0x26CE..0x26CE,
        0x26D4..0x26D4,
        0x26EA..0x26EA,
        0x26F2..0x26F3,
        0x26F5..0x26F5,
        0x26FA..0x26FA,
        0x26FD..0x26FD,
        0x2705..0x2705,
        0x270A..0x270B,
        0x2728..0x2728,
        0x274C..0x274C,
        0x274E..0x274E,
        0x2753..0x2755,
        0x2757..0x2757,
        0x2795..0x2797,
        0x27B0..0x27B0,
        0x27BF..0x27BF,
        0x2B1B..0x2B1C,
        0x2B50..0x2B50,
        0x2B55..0x2B55,
    )

    // Text-default characters that Unicode defines as emoji-capable pictographs.
    private val TEXT_EMOJI_RANGES = arrayOf(
        0x203C..0x203C,
        0x2049..0x2049,
        0x2139..0x2139,
        0x2194..0x2199,
        0x21A9..0x21AA,
        0x2328..0x2328,
        0x23CF..0x23CF,
        0x23ED..0x23EF,
        0x23F1..0x23F2,
        0x23F8..0x23FA,
        0x24C2..0x24C2,
        0x25AA..0x25AB,
        0x25B6..0x25B6,
        0x25C0..0x25C0,
        0x2600..0x2604,
        0x260E..0x260E,
        0x2611..0x2611,
        0x2618..0x2618,
        0x261D..0x261D,
        0x2620..0x2620,
        0x2622..0x2623,
        0x2626..0x2626,
        0x262A..0x262A,
        0x262E..0x262F,
        0x2638..0x263A,
        0x2640..0x2640,
        0x2642..0x2642,
        0x265F..0x2660,
        0x2663..0x2663,
        0x2665..0x2666,
        0x2668..0x2668,
        0x267B..0x267B,
        0x267E..0x267E,
        0x2692..0x2692,
        0x2694..0x2697,
        0x2699..0x2699,
        0x269B..0x269C,
        0x26A0..0x26A0,
        0x26A7..0x26A7,
        0x26B0..0x26B1,
        0x26C8..0x26C8,
        0x26CF..0x26CF,
        0x26D1..0x26D1,
        0x26D3..0x26D3,
        0x26E9..0x26E9,
        0x26F0..0x26F1,
        0x26F4..0x26F4,
        0x26F7..0x26F9,
        0x2702..0x2702,
        0x2708..0x2709,
        0x270C..0x270D,
        0x270F..0x270F,
        0x2712..0x2712,
        0x2714..0x2714,
        0x2716..0x2716,
        0x271D..0x271D,
        0x2721..0x2721,
        0x2733..0x2734,
        0x2744..0x2744,
        0x2747..0x2747,
        0x2763..0x2764,
        0x27A1..0x27A1,
        0x2934..0x2935,
        0x2B05..0x2B07,
        0x3030..0x3030,
        0x303D..0x303D,
        0x3297..0x3297,
        0x3299..0x3299,
    )

    private val SUPPLEMENTARY_EMOJI_RANGES = arrayOf(
        0x1F004..0x1F004,
        0x1F0CF..0x1F0CF,
        0x1F170..0x1F171,
        0x1F17E..0x1F17F,
        0x1F18E..0x1F18E,
        0x1F191..0x1F19A,
        0x1F1E6..0x1F1FF,
        0x1F201..0x1F202,
        0x1F21A..0x1F21A,
        0x1F22F..0x1F22F,
        0x1F232..0x1F23A,
        0x1F250..0x1F251,
        0x1F300..0x1FAFF,
    )

    private val HORIZONTAL_WHITESPACE = Regex("[\\t\\u000B\\u000C\\r ]+")
    private val SPACE_BEFORE_PUNCTUATION = Regex(" +([,.;:!?])")
    private val SPACES_AROUND_LINE_BREAK = Regex(" *\\n *")
}
