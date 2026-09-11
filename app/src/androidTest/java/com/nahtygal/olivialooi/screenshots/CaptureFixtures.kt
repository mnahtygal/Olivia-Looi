package com.nahtygal.olivialooi.screenshots

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import com.nahtygal.olivialooi.games.math.*
import com.nahtygal.olivialooi.games.abc.*
import com.nahtygal.olivialooi.games.shapes.*
import com.nahtygal.olivialooi.games.piano.*
import com.nahtygal.olivialooi.games.drums.*
import com.nahtygal.olivialooi.games.puzzles.*
import com.nahtygal.olivialooi.games.memory.*
import com.nahtygal.olivialooi.games.counting.*
import com.nahtygal.olivialooi.games.spelling.*
import com.nahtygal.olivialooi.games.tictactoe.*
import com.nahtygal.olivialooi.games.billiards.*
import com.nahtygal.olivialooi.stories.*
import com.nahtygal.olivialooi.ui.LooLooApp
import com.nahtygal.olivialooi.ui.apps.AppsScreen
import com.nahtygal.olivialooi.ui.games.memory.*
import com.nahtygal.olivialooi.ui.games.counting.*
import com.nahtygal.olivialooi.ui.games.spelling.*
import com.nahtygal.olivialooi.ui.games.coloring.*
import com.nahtygal.olivialooi.ui.games.animals.*
import com.nahtygal.olivialooi.ui.games.math.*
import com.nahtygal.olivialooi.ui.games.abc.*
import com.nahtygal.olivialooi.ui.games.shapes.*
import com.nahtygal.olivialooi.ui.games.puzzles.*
import com.nahtygal.olivialooi.ui.games.piano.*
import com.nahtygal.olivialooi.ui.games.drums.*
import com.nahtygal.olivialooi.ui.games.billiards.*
import com.nahtygal.olivialooi.ui.games.tictactoe.*
import com.nahtygal.olivialooi.ui.stories.StoryTimeScreen
import kotlin.random.Random

/** Test-only saved-state injection. No reflection into game state or production mutation.
 * Existing private Savers are invoked only to serialize valid pure-engine fixture states.
 * The ordered restore slots are an explicit contract; the capture test checks consumption.
 */
sealed interface FixtureSlot {
    val payload: Any?
    fun restoredValue(): Any
    fun savedPayload(value: Any?): Any?
}

data class FixtureMutableState(override val payload: Any?) : FixtureSlot {
    override fun restoredValue(): Any = mutableStateOf(payload)
    override fun savedPayload(value: Any?): Any? {
        check(value is MutableState<*>) { "Expected MutableState saver output, found ${value?.javaClass?.simpleName ?: "null"}" }
        return value.value
    }
}

data class FixtureMutableIntState(override val payload: Int) : FixtureSlot {
    override fun restoredValue(): Any = mutableIntStateOf(payload)
    override fun savedPayload(value: Any?): Any? {
        check(value is MutableIntState) { "Expected MutableIntState saver output, found ${value?.javaClass?.simpleName ?: "null"}" }
        return value.intValue
    }
}

data class FixtureMutableLongState(override val payload: Long) : FixtureSlot {
    override fun restoredValue(): Any = mutableLongStateOf(payload)
    override fun savedPayload(value: Any?): Any? {
        check(value is MutableLongState) { "Expected MutableLongState saver output, found ${value?.javaClass?.simpleName ?: "null"}" }
        return value.longValue
    }
}

class FixtureRegistry(private val values: List<FixtureSlot>) : SaveableStateRegistry {
    private val delegate = SaveableStateRegistry(null) { true }
    // Compose can legally register multiple rememberSaveable providers under one
    // generated key. Preserve each occurrence instead of letting a later slot replace it.
    private val restored = linkedMapOf<String, MutableList<FixtureSlot>>()
    var consumed = 0
        private set
    override fun consumeRestored(key: String): Any? {
        if (consumed >= values.size) return null
        val slot = values[consumed++]
        restored.getOrPut(key) { mutableListOf() }.add(slot)
        return slot.restoredValue()
    }
    override fun registerProvider(key: String, valueProvider: () -> Any?): SaveableStateRegistry.Entry = delegate.registerProvider(key, valueProvider)
    override fun canBeSaved(value: Any) = true
    override fun performSave(): Map<String, List<Any?>> = delegate.performSave()
    fun assertConsumed() {
        check(consumed == values.size) { "Saved-state fixture contract changed" }
        val saved = performSave()
        restored.forEach { (key, slots) ->
            val savedValues = saved[key].orEmpty()
            slots.forEachIndexed { occurrence, slot ->
                check(occurrence < savedValues.size) {
                    "Restored fixture save slot missing: $key occurrence=$occurrence"
                }
                val actual = slot.savedPayload(savedValues[occurrence])
                // Story narration and Drum replay identities are asynchronous guards, not
                // rendered state. Every other persisted field remains strict.
                check(FixtureEquivalence.matches(slot.payload, actual)) {
                    "Restored fixture did not survive its production saver: $key occurrence=$occurrence; ${FixtureEquivalence.difference(slot.payload, actual)}"
                }
            }
        }
    }
}

/** Narrow equivalence for production-restored state whose identity is deliberately regenerated. */
object FixtureEquivalence {
    @JvmStatic
    fun matches(expected: Any?, actual: Any?): Boolean {
        if (expected !is String || actual !is String) return expected == actual
        val story = expected.split('|').let { it.size == 6 && it[0] == "1" && StoryCatalog.find(it[1]) != null }
        if (story) return actual.substringBeforeLast('|') == expected.substringBeforeLast('|')
        val expectedDrum = DrumStateCodec.decode(expected)
        val actualDrum = DrumStateCodec.decode(actual)
        if (expectedDrum != null && actualDrum != null) {
            return expectedDrum.copy(replayIdentity = 0) == actualDrum.copy(replayIdentity = 0)
        }
        return actual == expected
    }

    @JvmStatic
    fun difference(expected: Any?, actual: Any?): String {
        if (expected is String && actual is String) {
            val expectedDrum = DrumStateCodec.decode(expected)
            val actualDrum = DrumStateCodec.decode(actual)
            if (expectedDrum != null && actualDrum != null) {
                val fields = listOf(
                    "mode" to (expectedDrum.mode to actualDrum.mode),
                    "pattern" to (expectedDrum.pattern.map { it.stableId } to actualDrum.pattern.map { it.stableId }),
                    "completedRounds" to (expectedDrum.completedRounds to actualDrum.completedRounds),
                    "position" to (expectedDrum.position to actualDrum.position),
                    "phase" to (expectedDrum.phase to actualDrum.phase),
                    "replayIdentity" to (expectedDrum.replayIdentity to actualDrum.replayIdentity),
                )
                return fields.filter { (_, pair) -> pair.first != pair.second }
                    .joinToString { (name, pair) -> "$name expected=${pair.first} actual=${pair.second}" }
                    .ifEmpty { "no decoded Drum field difference" }
            }
        }
        return "expected=${expected?.javaClass?.simpleName}:${expected.toString().take(120)} actual=${actual?.javaClass?.simpleName}:${actual.toString().take(120)}"
    }
}

class CaptureFixture(val registry: FixtureRegistry, val content: @Composable () -> Unit)

object CaptureFixtures {
    private fun state(value: Any?) = FixtureMutableState(value)
    private fun intState(value: Int) = FixtureMutableIntState(value)
    private fun longState(value: Long) = FixtureMutableLongState(value)

    private fun saved(screen: String, field: String, state: Any): Any {
        val owner = Class.forName("com.nahtygal.olivialooi.ui.games.$screen")
        val member = owner.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val saver = member.get(null) as Saver<Any, Any>
        val encoded = with(saver) { object : SaverScope { override fun canBeSaved(value: Any) = true }.save(state) }!!
        check(saver.restore(encoded) == state) { "Production saver rejected fixture" }
        return encoded
    }
    fun create(id: String): CaptureFixture {
        val seed = Random(26)
        var values: List<FixtureSlot> = emptyList()
        val content: @Composable () -> Unit
        when {
            id.startsWith("home_") && id != "home_story_entry" -> content = { LooLooApp() }
            id.startsWith("games_main") -> { values = listOf(state("Games")); content = { LooLooApp() } }
            id == "apps_main" -> content = { AppsScreen({ false }, {}) }
            id == "settings_main" -> content = { LooLooApp() }
            id.startsWith("memory_match_") -> {
                var s = MemoryMatchEngine.newGame(MemoryGameSize.Little, seed)
                if (id.endsWith("in_progress")) s = MemoryMatchEngine.selectCard(s,0)
                values = listOf(state(saved("memory.MemoryMatchGameScreenKt","MemoryMatchStateSaver",s)))
                content = { MemoryMatchGameScreen(MemoryGameSize.Little,{}, {}) }
            }
            id == "coloring_main" -> content = { ColoringPictureScreen({}, {}) }
            id.startsWith("speak_spell_") -> {
                if (id.endsWith("main")) content = { SpeakAndSpellLevelScreen({}, {}) }
                else {
                    val s = SpeakAndSpellEngine.newGame(SpellingLevel.entries.first(),seed)
                    values = listOf(state(saved("spelling.SpeakAndSpellGameScreenKt","SpeakAndSpellStateSaver",s)))
                    content = { SpeakAndSpellGameScreen(SpellingLevel.entries.first(),{}, {}) }
                }
            }
            id.startsWith("animal_sounds_") -> {
                if(id.endsWith("selected")) values = listOf(state("cow"),intState(0),state(true),state(AnimalPlayMode.FREE_PLAY.name),state(AnimalId.Cow.name),intState(0),state(null))
                if(id.endsWith("who_sound")) values = listOf(state(null),intState(0),state(false),state(AnimalPlayMode.WHO_MAKES_THIS_SOUND.name),state(AnimalId.Cow.name),intState(0),state(null))
                if(id.endsWith("find_animal")) values = listOf(state(null),intState(0),state(false),state(AnimalPlayMode.FIND_THE_ANIMAL.name),state(AnimalId.Cow.name),intState(0),state(null))
                content = { AnimalSoundsScreen({}) }
            }
            id.startsWith("count_") -> {
                if(id.endsWith("main")) content = { CountingLevelScreen({}, {}) }
                else {
                    var s = CountingEngine.newGame(CountingLevel.entries.first(),seed)
                    while (s.targetCount < 2) s = CountingEngine.newGame(CountingLevel.entries.first(),seed)
                    s = CountingEngine.selectObject(s,s.displayedObjects.first().id)
                    values = listOf(state(saved("counting.CountingGameScreenKt","CountingStateSaver",s)))
                    content = { CountingGameScreen(CountingLevel.entries.first(),{}, {}) }
                }
            }
            id.startsWith("math_") -> {
                if(id.endsWith("picker")) content = { MathLevelScreen({}, {}) }
                else {
                    val level = when(id) { "math_double_digit" -> MathLevel.DOUBLE_DIGIT; "math_triple_digit" -> MathLevel.TRIPLE_DIGIT; "math_mixed" -> MathLevel.MIXED; else -> MathLevel.SINGLE_DIGIT }
                    var s = MathEngine.newGame(level,seed)
                    if(id.endsWith("completion")) {
                        repeat(5) { s = MathEngine.selectAnswer(s,s.currentProblem.correctAnswer); s=MathEngine.finishEvaluation(s,s.attemptIdentity); if(!s.sessionComplete) s=MathEngine.nextProblem(s,seed) }
                        check(s.sessionComplete)
                    }
                    values = listOf(state(saved("math.MathGameScreenKt","MathStateSaver",s)))
                    content = { MathGameScreen(level,26,{}, {}) }
                }
            }
            id.startsWith("abc_") -> {
                if(id.endsWith("picker")) content = { AbcAdventureScreen({}, {}) }
                else {
                    val mode=when(id) { "abc_learn" -> AbcMode.LEARN; "abc_starts_with" -> AbcMode.STARTS_WITH; else -> AbcMode.FIND_LETTER }
                    var s=AbcEngine.newGame(mode,seed)
                    if(id.endsWith("completion")) {
                        repeat(5) { s=AbcEngine.selectChoice(s,s.currentTarget.stableId); s=AbcEngine.finishEvaluation(s,s.attemptIdentity); if(!s.sessionComplete) s=AbcEngine.nextRound(s,seed) }
                        check(s.sessionComplete)
                    }
                    values=listOf(state(saved("abc.AbcActivityScreenKt","AbcStateSaver",s)))
                    content={ AbcActivityScreen(mode,26,{}, {}) }
                }
            }
            id.startsWith("shapes_") -> {
                if(id.endsWith("picker")) content={ ShapesAdventureScreen({}, {}) }
                else {
                    val mode=when(id) { "shapes_learn" -> ShapesMode.LEARN; "shapes_find_color_shape" -> ShapesMode.FIND_COLOR_SHAPE; else -> ShapesMode.FIND_SHAPE }
                    var s=ShapesEngine.newGame(mode,seed)
                    if(id.endsWith("completion")) {
                        repeat(5) { s=ShapesEngine.selectChoice(s,s.currentTarget.stableId,s.attemptIdentity); s=ShapesEngine.finishEvaluation(s,s.attemptIdentity); if(!s.sessionComplete) s=ShapesEngine.nextRound(s,seed) }
                        check(s.sessionComplete)
                    }
                    values=listOf(state(ShapesStateCodec.encode(s).also { check(ShapesStateCodec.decode(it) != null) }))
                    content={ ShapesActivityScreen(mode,26,{}, {}) }
                }
            }
            id.startsWith("puzzle_") -> {
                val picture=PuzzlePicture.BUTTERFLY
                if(id=="puzzle_picture_picker") content={ PuzzlePictureScreen({}, {}) }
                else if(id=="puzzle_piece_picker") content={ PuzzleDifficultyScreen(picture,{}, {}) }
                else {
                    val difficulty=when { "16_piece" in id -> PuzzleDifficulty.BIG; "9_piece" in id -> PuzzleDifficulty.MEDIUM; "12_piece" in id -> PuzzleDifficulty.HARD; else -> PuzzleDifficulty.EASY }
                    var s=PuzzleEngine.newPuzzle(picture,difficulty,seed)
                    val placed=if(id.endsWith("completion")) s.totalPieceCount else if(id.endsWith("progress")) 2 else 0
                    s=s.copy(pieces=s.pieces.mapIndexed { i,p -> p.copy(isPlaced=i<placed) }, trayOrder=s.trayOrder.filter { it>=placed })
                    s=PuzzleEngine.acknowledgeMilestones(s)
                    values=listOf(state(PuzzleStateCodec.encode(s).also { check(PuzzleStateCodec.decode(it) != null) }))
                    content={ PuzzleBoardScreen(picture,difficulty,26,{}, {}, {}) }
                }
            }
            id.startsWith("piano_") -> {
                var s=PianoState(openingSpoken=true)
                if(id!="piano_free_play") {
                    s=PianoEngine.startSong(PianoEngine.selectSong(s,PianoCatalog.songs.first().stableId))
                    if(id.endsWith("complete")) s.song!!.notes.forEach { s=PianoEngine.press(s,it) }
                    s=s.copy(completionSpoken=s.isComplete)
                }
                values=listOf(state(PianoStateCodec.encode(s).also { check(PianoStateCodec.decode(it) != null) })); content={ PianoScreen({}) }
            }
            id.startsWith("drums_") -> {
                var s=DrumState()
                if(id!="drums_free_play") {
                    s=DrumEngine.copyBeat(s,seed)
                    if(id.endsWith("completion")) {
                        repeat(5) { s=DrumEngine.finishReplay(s,s.replayIdentity); s.pattern.forEach { s=DrumEngine.hit(s,it) }; if(!s.sessionComplete) s=DrumEngine.nextBeat(s,seed) }
                    } else if(id.endsWith("your_turn")) s=DrumEngine.finishReplay(s,s.replayIdentity)
                }
                values=listOf(state(DrumStateCodec.encode(s).also { check(DrumStateCodec.decode(it) != null) }),state(true)); content={ DrumScreen({}) }
            }
            id.startsWith("billiards_") -> {
                if(id.endsWith("picker")) content={ BilliardsModeScreen({}, {}) }
                else {
                    val mode=when { "versus" in id -> BilliardsMode.VERSUS; "try_pocket" in id -> BilliardsMode.TRY_POCKET; else -> BilliardsMode.FREE_PLAY }
                    var s=BilliardsEngine.newGame(mode,seed)
                    if(id.endsWith("looloo_turn")) s=BilliardsEngine.prepareAi(s.copy(turn=TurnOwner.LOOLOO),s.turnIdentity,seed)
                    values=listOf(state(BilliardsStateCodec.encode(s).also { check(BilliardsStateCodec.decode(it) != null) }),longState(s.turnIdentity))
                    content={ BilliardsTableScreen(mode,26,{}, {}, {}) }
                }
            }
            id.startsWith("story_") || id=="home_story_entry" -> {
                if(id=="story_library" || id=="story_expanded_library" || id=="home_story_entry") values=listOf(state(true))
                else {
                    val index=when(id) { "story_eliana_duck_page" -> 1; "story_rainbow_page" -> 2; "story_sleepy_puppy_page" -> 3; "story_little_star_page" -> 4; "story_new_barnyard_page" -> 6; else -> 0 }
                    var s=StoryEngine.open(StoryCatalog.stories[index].id)
                    if(!id.startsWith("story_opening")) {
                        s=StoryEngine.chooseMode(s,if(id=="story_read_to_me") ReadingMode.TO_ME else ReadingMode.MYSELF)
                        if(id.endsWith("_page")) repeat(3) { s=StoryEngine.next(s) }
                        if(id=="story_completion") repeat(6) { s=StoryEngine.next(s) }
                    }
                    values=listOf(state(false),state(StoryEngine.encode(s)))
                }
                content={ StoryTimeScreen({}) }
            }
            id.startsWith("tic_tac_toe_") -> {
                var s=TicTacToeEngine.newGame()
                val moves=if(id.endsWith("completion")) listOf(0,3,1,4,2) else if(id.endsWith("in_progress")) listOf(0,4,2) else emptyList()
                moves.forEach { s=TicTacToeEngine.playMove(s,it) }
                values=listOf(state(TicTacToeDifficulty.Easy.name),state(saved("tictactoe.TicTacToeGameScreenKt","TicTacToeStateSaver",s)))
                content={ TicTacToeGameScreen(TicTacToeGameMode.PersonVsPerson,{}, {}) }
            }
            else -> error("No fixture for $id")
        }
        return CaptureFixture(FixtureRegistry(values),content)
    }
}
