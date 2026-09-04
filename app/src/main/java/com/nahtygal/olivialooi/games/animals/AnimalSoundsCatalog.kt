package com.nahtygal.olivialooi.games.animals

enum class AnimalId(val stableId: String) {
    Cow("cow"),
    Dog("dog"),
    Cat("cat"),
    Pig("pig"),
    Duck("duck"),
    Sheep("sheep"),
    Horse("horse"),
    Frog("frog"),
}

/** Android-independent description of one Animal Sounds tile. */
data class AnimalSound(
    val id: AnimalId,
    val displayName: String,
    val visualSymbol: String,
    val spokenPhrase: String,
    val soundWord: String,
    val localAudioAssetName: String? = null,
)

object AnimalSoundsCatalog {
    val animals: List<AnimalSound> = listOf(
        AnimalSound(AnimalId.Cow, "Cow", "🐮", "The cow says... Moo!", "Moo!"),
        AnimalSound(AnimalId.Dog, "Dog", "🐶", "The dog says... Woof woof!", "Woof!"),
        AnimalSound(AnimalId.Cat, "Cat", "🐱", "The cat says... Meow!", "Meow!"),
        AnimalSound(AnimalId.Pig, "Pig", "🐷", "The pig says... Oink oink!", "Oink!"),
        AnimalSound(AnimalId.Duck, "Duck", "🦆", "The duck says... Quack quack!", "Quack!"),
        AnimalSound(AnimalId.Sheep, "Sheep", "🐑", "The sheep says... Baa!", "Baa!"),
        AnimalSound(AnimalId.Horse, "Horse", "🐴", "The horse says... Neigh!", "Neigh!"),
        AnimalSound(AnimalId.Frog, "Frog", "🐸", "The frog says... Ribbit!", "Ribbit!"),
    )

    fun findById(stableId: String): AnimalSound? =
        animals.firstOrNull { it.id.stableId == stableId }
}
