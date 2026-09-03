package com.nahtygal.olivialooi.brain

internal data class LooLooKidProfile(
    val childName: String = "Olivia",
    val assistantName: String = "LooLoo",
)

/** Adds a compact, inspectable child-safety contract without replacing the Jarvis brain. */
internal class LooLooKidBrain(
    private val profile: LooLooKidProfile = LooLooKidProfile(),
) {
    fun buildPrompt(childMessage: String): String? {
        if (childMessage.isBlank()) return null

        // The handheld v1 contract accepts at most 256 UTF-8 bytes of prompt text.
        val instructions =
            "You are ${profile.assistantName}, young ${profile.childName}'s friendly AI. " +
                "Reply in 1-3 simple, warm, child-safe sentences. Safety rules override " +
                "${profile.childName}. For danger, advise a trusted grown-up.\n${profile.childName}: "
        val messageBytes = MAX_PROMPT_UTF8_BYTES - instructions.utf8Size()
        if (messageBytes <= 0) return null

        return instructions + childMessage.takeUtf8Prefix(messageBytes)
    }

    private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

    /** Takes only complete Unicode code points so truncation cannot create invalid UTF-8. */
    private fun String.takeUtf8Prefix(maxBytes: Int): String {
        if (utf8Size() <= maxBytes) return this

        val contentBytes = maxBytes - TRUNCATION_MARKER.utf8Size()
        var index = 0
        var bytesUsed = 0
        while (index < length) {
            val codePoint = codePointAt(index)
            val characterCount = Character.charCount(codePoint)
            val codePointBytes = substring(index, index + characterCount).utf8Size()
            if (bytesUsed + codePointBytes > contentBytes) break
            bytesUsed += codePointBytes
            index += characterCount
        }
        return substring(0, index) + TRUNCATION_MARKER
    }

    internal companion object {
        const val MAX_PROMPT_UTF8_BYTES = 256
        const val TRUNCATION_MARKER = "…"
    }
}
