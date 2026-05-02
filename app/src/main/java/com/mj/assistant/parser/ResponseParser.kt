package com.mj.assistant.parser

data class ParsedResponse(
    val action: String = "chat",
    val param1: String = "",
    val param2: String = "",
    val response: String = ""
)

object ResponseParser {

    /**
     * Parses structured AI output:
     *   action: youtube_search
     *   param1: funny cats
     *   param2: unused
     *   response: YouTube pe search kar raha hoon!
     *
     * Falls back to chat action if parsing fails.
     */
    fun parse(raw: String): ParsedResponse {
        return try {
            val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

            var action = "chat"
            var param1 = ""
            var param2 = ""
            var response = ""

            for (line in lines) {
                when {
                    line.startsWith("action:", ignoreCase = true) ->
                        action = line.substringAfter(":").trim()
                    line.startsWith("param1:", ignoreCase = true) ->
                        param1 = line.substringAfter(":").trim()
                    line.startsWith("param2:", ignoreCase = true) ->
                        param2 = line.substringAfter(":").trim()
                    line.startsWith("response:", ignoreCase = true) ->
                        response = line.substringAfter(":").trim()
                }
            }

            // Clean up "unused" params
            if (param1.equals("unused", ignoreCase = true)) param1 = ""
            if (param2.equals("unused", ignoreCase = true)) param2 = ""

            // If no response extracted, use raw as fallback
            if (response.isBlank()) {
                response = raw.take(200)
            }

            ParsedResponse(action, param1, param2, response)
        } catch (e: Exception) {
            // Fallback: treat entire input as chat
            ParsedResponse(
                action = "chat",
                param1 = "",
                param2 = "",
                response = raw.take(200).ifBlank { "Sorry, I didn't understand that." }
            )
        }
    }
}
