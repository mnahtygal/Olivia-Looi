package com.nahtygal.olivialooi.stories

enum class StoryVisual { SNOWMAN, DUCK, RAINBOW, PUPPY, STAR }
data class StoryPage(val pageNumber: Int, val text: String, val visual: StoryVisual)
data class Story(val id: String, val title: String, val shortDescription: String, val coverVisual: StoryVisual, val pages: List<StoryPage>)

/** Original, bundled stories. No platform, speech, or resource dependencies. */
object StoryCatalog {
    private fun story(id: String, title: String, description: String, visual: StoryVisual, vararg text: String) =
        Story(id, title, description, visual, text.mapIndexed { index, words -> StoryPage(index + 1, words, visual) })
    val stories = listOf(
        story("moonlight_snowman", "Olivia and the Moonlight Snowman", "A warm friendship on a snowy evening.", StoryVisual.SNOWMAN,
            "Olivia looked out at the soft snow. A round snowman stood beside the garden gate, wearing a tiny blue hat.",
            "The moon made his buttons shine. Olivia brought a scarf and wrapped it gently around his snowy shoulders.",
            "LooLoo helped Olivia make a little snow rabbit. They gave it two long ears and a smooth pebble nose.",
            "Next, they made a snow bird beside the rabbit. Now the snowman had two friends to share the moonlight.",
            "Olivia drew a heart in the snow. She waved to her new friends, and their shadows waved across the garden.",
            "Inside, Olivia snuggled under her blanket. Outside, the moon watched over three snowy friends and one small heart until morning."),
        story("yellow_duck", "Eliana and the Little Yellow Duck", "Small acts of kindness beside a pond.", StoryVisual.DUCK,
            "Eliana sat on a blanket beside the pond. A little yellow duck paddled past, making tiny circles in the water.",
            "The duck nudged a leaf with its soft bill. Eliana pointed, and Olivia smiled at the little floating boat.",
            "A breeze carried the leaf toward the grass. Olivia gently pushed it back with a twig while Eliana watched.",
            "The duck followed its leaf around a flower. Two other ducks joined in, and their ripples made a wiggly path.",
            "Eliana clapped her hands softly. The little duck gave a happy quack, then paddled close to its warm duck family.",
            "The sisters waved goodbye to the pond. On the way home, they found a yellow flower as bright as their new friend."),
        story("rainbow_sisters", "Olivia and Eliana’s Rainbow Adventure", "Two sisters make something lovely together.", StoryVisual.RAINBOW,
            "After a little rain, a rainbow stretched over the garden. Olivia and Eliana wondered if they could make a rainbow too.",
            "Olivia found a red ribbon and an orange cloth. Eliana chose a yellow cushion with a big, happy smile.",
            "They laid their colors on a blanket. LooLoo brought a green scarf, and Olivia added a soft blue sock.",
            "One space was still waiting for a color. Eliana reached for her purple bunny, and Olivia tucked it beside the sock.",
            "Their rainbow was small enough to hug. The sisters sat beside it and pointed to each color, one by one.",
            "The sky rainbow faded, but their blanket rainbow stayed. Olivia hugged Eliana, glad that making things together was the brightest part."),
        story("sleepy_puppy", "LooLoo’s Sleepy Puppy", "A soft, slow journey to bedtime.", StoryVisual.PUPPY,
            "LooLoo’s puppy had spent the afternoon playing with a soft ball. Now his little paws were ready for a rest.",
            "He carried the ball to his basket. It rolled onto the rug, so he gave it one last gentle nudge.",
            "Olivia placed a blanket in the basket. Eliana waved goodnight while the puppy turned around to find his favorite spot.",
            "LooLoo sat nearby and took a slow breath. The puppy’s ears relaxed, and his tail grew still against the blanket.",
            "A moonbeam rested on the windowsill. The puppy closed one eye, then the other, listening to the quiet room.",
            "His ball could wait until morning. LooLoo whispered goodnight, and the puppy dreamed of soft grass beneath his little paws."),
        story("little_star", "The Little Star That Couldn’t Sleep", "A quiet sky and a reassuring friend.", StoryVisual.STAR,
            "A little star twinkled above the rooftops. All evening, she had counted windows, clouds, and birds returning to their nests.",
            "Now the sky was quiet, but she still felt wide awake. She asked the moon how to get ready for rest.",
            "The moon smiled and pointed to a drifting cloud. The star watched it float slowly, without hurrying anywhere at all.",
            "She let her light grow soft and small. Below her, Olivia and Eliana were cozy, with their blankets tucked around them.",
            "The moon said that resting could begin with being still. The star stopped counting and simply enjoyed the peaceful sky.",
            "Soon she felt as gentle as a sleepy sigh. The moon kept her company while the whole little neighborhood rested together."),
    )
    fun find(id: String): Story? = stories.firstOrNull { it.id == id }
}
