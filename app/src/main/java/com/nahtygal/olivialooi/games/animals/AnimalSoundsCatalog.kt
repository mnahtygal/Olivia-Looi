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
    val soundWord: String,
    val localAudioAssetName: String,
) {
    val spokenPhrase: String
        get() {
            val sound = soundWord.removeSuffix("!").lowercase()
            val spokenSound = if (id in repeatedSoundAnimals) "$sound $sound" else sound
            return "The ${displayName.lowercase()} says $spokenSound!"
        }

    private companion object {
        val repeatedSoundAnimals = setOf(AnimalId.Dog, AnimalId.Pig, AnimalId.Duck)
    }
}

object AnimalSoundsCatalog {
    val animals: List<AnimalSound> = listOf(
        AnimalSound(AnimalId.Cow, "Cow", "🐮", "Moo!", "animal_cow_moo"),
        AnimalSound(AnimalId.Dog, "Dog", "🐶", "Woof!", "animal_dog_bark"),
        AnimalSound(AnimalId.Cat, "Cat", "🐱", "Meow!", "animal_cat_meow"),
        AnimalSound(AnimalId.Pig, "Pig", "🐷", "Oink!", "animal_pig_oink"),
        AnimalSound(AnimalId.Duck, "Duck", "🦆", "Quack!", "animal_duck_quack"),
        AnimalSound(AnimalId.Sheep, "Sheep", "🐑", "Baa!", "animal_sheep_baa"),
        AnimalSound(AnimalId.Horse, "Horse", "🐴", "Neigh!", "animal_horse_neigh"),
        AnimalSound(AnimalId.Frog, "Frog", "🐸", "Ribbit!", "animal_frog_ribbit"),
    )

    fun findById(stableId: String): AnimalSound? =
        animals.firstOrNull { it.id.stableId == stableId }
}
