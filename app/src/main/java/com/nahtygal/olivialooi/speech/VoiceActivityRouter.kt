package com.nahtygal.olivialooi.speech

/** Destinations are identifiers resolved by the existing Compose navigation host. */
internal data class VoiceActivity(
    val id: String,
    val displayName: String,
    val aliases: List<String>,
    val category: String,
    val supportsOpponent: Boolean = false,
)

internal sealed interface VoiceRoute {
    data class Open(val id: String, val opponent: Opponent? = null) : VoiceRoute
    data object AskOpponent : VoiceRoute
    data object ChooseGame : VoiceRoute
    data object Cancel : VoiceRoute
    data object Conversation : VoiceRoute
}

enum class Opponent { LOOLOO, PERSON }

internal object VoiceActivityRouter {
    val activities = listOf(
        VoiceActivity("tictactoe", "Tic-Tac-Toe", listOf("tic tac toe", "tick tack toe", "noughts and crosses"), "game", true),
        VoiceActivity("animals", "Animal Sounds", listOf("animal game", "animal sounds", "animals"), "game"),
        VoiceActivity("memory", "Memory Match", listOf("memory match", "memory game"), "game"),
        VoiceActivity("coloring", "Coloring", listOf("coloring", "color game"), "game"),
        VoiceActivity("spelling", "Speak and Spell", listOf("spelling", "spell game", "speak and spell"), "game"),
        VoiceActivity("counting", "Counting", listOf("counting", "counting game"), "game"),
        VoiceActivity("math", "Math", listOf("math game", "math"), "game"),
        VoiceActivity("billiards", "Billiards", listOf("billiards", "pool game"), "game"),
        VoiceActivity("drums", "Drums", listOf("drums", "drum game"), "game"),
        VoiceActivity("piano", "Piano", listOf("piano", "piano game"), "game"),
        VoiceActivity("puzzles", "Puzzles", listOf("puzzles", "puzzle game"), "game"),
        VoiceActivity("shapes", "Shapes", listOf("shapes", "shape game"), "game"),
        VoiceActivity("abc", "ABC", listOf("abc", "alphabet game"), "game"),
        VoiceActivity("tracing", "Letter and Word Tracing", listOf("trace letters", "tracing letters", "trace words", "tracing words", "letter tracing", "word tracing"), "learning"),
        VoiceActivity("story", "Story Time", listOf("story", "stories"), "story"),
    )

    fun route(speech: String, pendingOpponent: Boolean = false): VoiceRoute {
        val words = speech.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
        fun has(phrase: String) = Regex("(?:^| )${Regex.escape(phrase)}(?: |$)").containsMatchIn(words)
        if (pendingOpponent && listOf("cancel", "stop", "never mind", "nevermind").any(::has)) return VoiceRoute.Cancel
        val person = listOf("daddy", "dad", "mommy", "mom", "someone else", "another person", "friend", "two players", "with you daddy").any(::has)
        if (pendingOpponent && words in listOf("you", "looloo", "with you", "you please")) return VoiceRoute.Open("tictactoe", Opponent.LOOLOO)
        if (pendingOpponent && words in listOf("daddy", "dad", "mommy", "mom", "someone else", "another person", "my friend", "friend", "two players", "with daddy", "with mommy")) return VoiceRoute.Open("tictactoe", Opponent.PERSON)
        val action = listOf("play", "open", "start", "launch", "trace", "tell me", "read me", "show me", "let s", "i want").any(::has)
        if (!action || listOf("tell me about", "what is", "how do", "why ").any { words.startsWith(it) }) return VoiceRoute.Conversation
        val activity = activities.firstOrNull { entry -> entry.aliases.any(::has) }
        if (activity != null) {
            if (activity.supportsOpponent) return when {
                person -> VoiceRoute.Open(activity.id, Opponent.PERSON)
                has("with you") || has("with looloo") || has("against you") -> VoiceRoute.Open(activity.id, Opponent.LOOLOO)
                else -> VoiceRoute.AskOpponent
            }
            return VoiceRoute.Open(activity.id)
        }
        if (has("play with you")) return VoiceRoute.Open("tictactoe", Opponent.LOOLOO)
        if (has("play with daddy") || has("play with mommy")) return VoiceRoute.Open("tictactoe", Opponent.PERSON)
        if (has("play a game") || has("play games")) return VoiceRoute.ChooseGame
        return VoiceRoute.Conversation
    }
}
