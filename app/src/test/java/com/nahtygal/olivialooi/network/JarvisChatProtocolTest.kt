package com.nahtygal.olivialooi.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisChatProtocolTest {
    @Test
    fun requestSerializationEscapesTextWithoutChangingUnicode() {
        assertEquals(
            "{\"prompt\":\"Tell \\\"LooLoo\\\" \\\\ a joke\\n❄️\"}",
            serializeJarvisRequest("Tell \"LooLoo\" \\ a joke\n❄️"),
        )
    }

    @Test
    fun successfulResponseReturnsOnlyAssistantText() {
        val result = mapJarvisResponse(
            200,
            """{"ok":true,"response":" Snow day! ","truncated":false,"metadata":{"id":7}}""",
        )

        assertEquals(JarvisChatResult.Success("Snow day!"), result)
    }

    @Test
    fun responseParserDecodesJsonEscapes() {
        val result = mapJarvisResponse(
            200,
            """{"ok":true,"response":"Line one\nSnowman: \u2603","truncated":false}""",
        )

        assertEquals(JarvisChatResult.Success("Line one\nSnowman: ☃"), result)
    }

    @Test
    fun malformedOrUnsuccessfulResponsesAreRejected() {
        val invalidBodies = listOf(
            "not json",
            "[]",
            """{"ok":false,"response":"No"}""",
            """{"ok":true,"response":"  "}""",
            """{"ok":true,"response":7}""",
            """{"ok":true,"response":"first","response":"second"}""",
            """{"ok":true,"response":"answer"} trailing""",
        )

        invalidBodies.forEach { body ->
            assertEquals(
                JarvisChatResult.Failure(JarvisChatFailure.InvalidResponse),
                mapJarvisResponse(200, body),
            )
        }
    }

    @Test
    fun httpErrorsMapToServerFailureWithoutParsingTheirBody() {
        val result = mapJarvisResponse(
            504,
            """{"ok":false,"error":{"code":"model_timeout"}}""",
        )

        assertTrue(result is JarvisChatResult.Failure)
        assertEquals(
            JarvisChatFailure.Server,
            (result as JarvisChatResult.Failure).reason,
        )
    }
}
