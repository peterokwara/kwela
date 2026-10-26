package app.kwela.domain

/** Kwela says she is a digital helper once, at the first handover. After that the line is only noise. */
object HonestyLine {
    private const val PHRASE = "digital helper"
    private val sentenceEnd = Regex("(?<=[.!?])\\s+")

    fun alreadySaid(earlierAssistantTexts: List<String>): Boolean = earlierAssistantTexts.any { it.contains(PHRASE, ignoreCase = true) }

    /** Removes every sentence that says it, drops bubbles left empty, and keeps everything else as it was. */
    fun strip(message: String): String =
        message.split("\n\n")
            .map { paragraph -> paragraph.split(sentenceEnd).filterNot { it.contains(PHRASE, ignoreCase = true) }.joinToString(" ") }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
}
