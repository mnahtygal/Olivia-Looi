package com.nahtygal.olivialooi.network

internal sealed interface JarvisChatResult {
    data class Success(val response: String) : JarvisChatResult

    data class Failure(val reason: JarvisChatFailure) : JarvisChatResult
}

internal enum class JarvisChatFailure {
    Configuration,
    Timeout,
    Connection,
    Server,
    InvalidResponse,
}

internal fun serializeJarvisRequest(prompt: String): String =
    "{\"prompt\":\"${prompt.toJsonStringContent()}\"}"

internal fun mapJarvisResponse(statusCode: Int, responseBody: String): JarvisChatResult {
    if (statusCode !in 200..299) return JarvisChatResult.Failure(JarvisChatFailure.Server)

    val fields = JsonParser(responseBody).parseObjectDocument()
        ?: return JarvisChatResult.Failure(JarvisChatFailure.InvalidResponse)
    val isSuccessful = (fields["ok"] as? JsonValue.BooleanValue)?.value == true
    val response = (fields["response"] as? JsonValue.StringValue)?.value?.trim()

    return if (isSuccessful && !response.isNullOrEmpty()) {
        JarvisChatResult.Success(response)
    } else {
        JarvisChatResult.Failure(JarvisChatFailure.InvalidResponse)
    }
}

private fun String.toJsonStringContent(): String = buildString(length) {
    this@toJsonStringContent.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character < ' ') {
                append("\\u")
                append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
}

private sealed interface JsonValue {
    data class StringValue(val value: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data class ObjectValue(val value: Map<String, JsonValue>) : JsonValue
    data class ArrayValue(val value: List<JsonValue>) : JsonValue
    data object NullValue : JsonValue
    data object NumberValue : JsonValue
}

/** Small dependency-free JSON reader for the bounded Jarvis response contract. */
private class JsonParser(private val source: String) {
    private var index = 0

    fun parseObjectDocument(): Map<String, JsonValue>? = try {
        skipWhitespace()
        val result = readObject(depth = 0)
        skipWhitespace()
        result.takeIf { index == source.length }
    } catch (_: InvalidJson) {
        null
    }

    private fun readValue(depth: Int): JsonValue {
        if (depth > MAX_JSON_DEPTH) invalidJson()
        skipWhitespace()
        return when (peek()) {
            '"' -> JsonValue.StringValue(readString())
            '{' -> JsonValue.ObjectValue(readObject(depth + 1))
            '[' -> JsonValue.ArrayValue(readArray(depth + 1))
            't' -> readLiteral("true", JsonValue.BooleanValue(true))
            'f' -> readLiteral("false", JsonValue.BooleanValue(false))
            'n' -> readLiteral("null", JsonValue.NullValue)
            '-', in '0'..'9' -> {
                readNumber()
                JsonValue.NumberValue
            }
            else -> invalidJson()
        }
    }

    private fun readObject(depth: Int): Map<String, JsonValue> {
        expect('{')
        skipWhitespace()
        if (consume('}')) return emptyMap()

        val fields = linkedMapOf<String, JsonValue>()
        while (true) {
            skipWhitespace()
            val name = readString()
            if (fields.containsKey(name)) invalidJson()
            skipWhitespace()
            expect(':')
            fields[name] = readValue(depth)
            skipWhitespace()
            when {
                consume('}') -> return fields
                consume(',') -> Unit
                else -> invalidJson()
            }
        }
    }

    private fun readArray(depth: Int): List<JsonValue> {
        expect('[')
        skipWhitespace()
        if (consume(']')) return emptyList()

        val values = mutableListOf<JsonValue>()
        while (true) {
            values += readValue(depth)
            skipWhitespace()
            when {
                consume(']') -> return values
                consume(',') -> Unit
                else -> invalidJson()
            }
        }
    }

    private fun readString(): String {
        expect('"')
        return buildString {
            while (index < source.length) {
                val character = source[index++]
                when {
                    character == '"' -> return@buildString
                    character == '\\' -> append(readEscape())
                    character < ' ' -> invalidJson()
                    else -> append(character)
                }
            }
            invalidJson()
        }
    }

    private fun readEscape(): Char {
        val escaped = next()
        return when (escaped) {
            '"', '\\', '/' -> escaped
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (index + 4 > source.length) invalidJson()
                val codePoint = source.substring(index, index + 4).toIntOrNull(16)
                    ?: invalidJson()
                index += 4
                codePoint.toChar()
            }
            else -> invalidJson()
        }
    }

    private fun readNumber() {
        consume('-')
        when {
            consume('0') -> Unit
            peek() in '1'..'9' -> while (peek() in '0'..'9') index++
            else -> invalidJson()
        }
        if (consume('.')) {
            if (peek() !in '0'..'9') invalidJson()
            while (peek() in '0'..'9') index++
        }
        if (peek() == 'e' || peek() == 'E') {
            index++
            if (peek() == '+' || peek() == '-') index++
            if (peek() !in '0'..'9') invalidJson()
            while (peek() in '0'..'9') index++
        }
    }

    private fun <T : JsonValue> readLiteral(literal: String, value: T): T {
        if (!source.startsWith(literal, index)) invalidJson()
        index += literal.length
        return value
    }

    private fun skipWhitespace() {
        while (peek() == ' ' || peek() == '\n' || peek() == '\r' || peek() == '\t') index++
    }

    private fun expect(expected: Char) {
        if (!consume(expected)) invalidJson()
    }

    private fun consume(expected: Char): Boolean {
        if (peek() != expected) return false
        index++
        return true
    }

    private fun peek(): Char? = source.getOrNull(index)

    private fun next(): Char = source.getOrNull(index++) ?: invalidJson()

    private fun invalidJson(): Nothing = throw InvalidJson

    private data object InvalidJson : RuntimeException()

    private companion object {
        const val MAX_JSON_DEPTH = 12
    }
}
