package app.kwela.domain

/** Splits one reply from Kwela into the short bubbles it is shown as. */
object BubbleSplitter {
    private val blankLine = Regex("\\n\\s*\\n")

    fun split(text: String): List<String> = text.split(blankLine).map { it.trim() }.filter { it.isNotEmpty() }
}
