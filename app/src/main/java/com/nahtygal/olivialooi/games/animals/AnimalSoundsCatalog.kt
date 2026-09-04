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
    val localAudioAssetName: String,
)

object AnimalSoundsCatalog {
    val animals: List<AnimalSound> = listOf(
        AnimalSound(AnimalId.Cow, "Cow", "🐮", "The cow says...", "Moo!", "animal_cow_moo"),
        AnimalSound(AnimalId.Dog, "Dog", "🐶", "The dog says...", "Woof!", "animal_dog_bark"),
        AnimalSound(AnimalId.Cat, "Cat", "🐱", "The cat says...", "Meow!", "animal_cat_meow"),
        AnimalSound(AnimalId.Pig, "Pig", "🐷", "The pig says...", "Oink!", "animal_pig_oink"),
        AnimalSound(AnimalId.Duck, "Duck", "🦆", "The duck says...", "Quack!", "animal_duck_quack"),
        AnimalSound(AnimalId.Sheep, "Sheep", "🐑", "The sheep says...", "Baa!", "animal_sheep_baa"),
        AnimalSound(AnimalId.Horse, "Horse", "🐴", "The horse says...", "Neigh!", "animal_horse_neigh"),
        AnimalSound(AnimalId.Frog, "Frog", "🐸", "The frog says...", "Ribbit!", "animal_frog_ribbit"),
    )

    fun findById(stableId: String): AnimalSound? =
        animals.firstOrNull { it.id.stableId == stableId }
}
