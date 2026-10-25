package app.kwela.domain

/** Safe file names for the PDFs a person downloads. */
object FileNames {
    private val unsafe = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
    private val spaces = Regex("\\s+")

    fun pdfName(title: String): String {
        val cleaned = title.replace(unsafe, " ").replace(spaces, " ").trim().removeSuffix(".pdf").trim().trim('.', ' ')
        return "${cleaned.take(80).trim().ifEmpty { "Kwela document" }}.pdf"
    }
}
