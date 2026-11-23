package com.nahtygal.olivialooi.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceActivityRouterTest {
    @Test fun ticTacToeVariants() {
        for (phrase in listOf("Let's play tic-tac-toe", "Can we play tic tac toe?", "play tick tack toe")) {
            assertEquals(VoiceRoute.AskOpponent, VoiceActivityRouter.route(phrase))
        }
    }

    @Test fun opponentsAndFollowUps() {
        assertEquals(VoiceRoute.Open("tictactoe", Opponent.LOOLOO), VoiceActivityRouter.route("I want to play with you"))
        assertEquals(VoiceRoute.Open("tictactoe", Opponent.PERSON), VoiceActivityRouter.route("Let's play tic tac toe with Daddy"))
        assertEquals(VoiceRoute.Open("tictactoe", Opponent.LOOLOO), VoiceActivityRouter.route("You!", true))
        assertEquals(VoiceRoute.Open("tictactoe", Opponent.PERSON), VoiceActivityRouter.route("Daddy", true))
        assertEquals(VoiceRoute.Cancel, VoiceActivityRouter.route("never mind", true))
        assertEquals(VoiceRoute.Conversation, VoiceActivityRouter.route("Daddy"))
        assertEquals(VoiceRoute.Conversation, VoiceActivityRouter.route("You!"))
    }

    @Test fun activitiesAndConversation() {
        assertEquals(VoiceRoute.Open("story"), VoiceActivityRouter.route("Can you tell me a story?"))
        assertEquals(VoiceRoute.Open("tracing"), VoiceActivityRouter.route("Let's trace letters"))
        assertEquals(VoiceRoute.Open("tracing"), VoiceActivityRouter.route("I want to trace words"))
        assertEquals(VoiceRoute.Open("animals"), VoiceActivityRouter.route("Open the animal game"))
        assertEquals(VoiceRoute.ChooseGame, VoiceActivityRouter.route("Let's play a game"))
        assertEquals(VoiceRoute.Conversation, VoiceActivityRouter.route("Play the dragon game"))
        assertEquals(VoiceRoute.Conversation, VoiceActivityRouter.route("Tell me about tic-tac-toe"))
        assertEquals(VoiceRoute.Conversation, VoiceActivityRouter.route("Why is the sky blue?"))
    }
}
