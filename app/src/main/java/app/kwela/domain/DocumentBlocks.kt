package app.kwela.domain

/** One tidy piece of a document, as shown on the full-page preview. */
sealed interface DocBlock {
    val text: String

    /** Whether a person can tap this piece to ask for a change. */
    val canChange: Boolean

    data class Title(override val text: String) : DocBlock { override val canChange = false }
    data class Subtitle(override val text: String) : DocBlock { override val canChange = false }
    data class Heading(override val text: String) : DocBlock { override val canChange = false }
    data class Paragraph(override val text: String) : DocBlock { override val canChange = true }
    data class Bullet(override val text: String) : DocBlock { override val canChange = true }
}

/** Turns the saved text of a CV or letter into blocks that are easy to scan on a phone. */
object DocumentBlocks {
    private val heading = Regex("^[A-Z][A-Z &/,'-]{2,}$")

    fun parse(body: String): List<DocBlock> {
        val blocks = mutableListOf<DocBlock>()
        var titleLine = -1
        body.lines().map { it.trim() }.forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            when {
                titleLine == -1 -> { blocks += DocBlock.Title(line); titleLine = index }
                index == titleLine + 1 && !heading.matches(line) && !line.startsWith("- ") -> blocks += DocBlock.Subtitle(line)
                heading.matches(line) -> blocks += DocBlock.Heading(line)
                line.startsWith("- ") -> blocks += DocBlock.Bullet(line.removePrefix("- ").trim())
                else -> blocks += DocBlock.Paragraph(line)
            }
        }
        return blocks
    }
}
