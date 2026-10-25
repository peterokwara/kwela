package app.kwela.domain

/** Asking for a change, the way a person would circle a line and say what is wrong with it. */
object ChangeRequest {
    /** Quick one-tap requests shown under finished documents. */
    val chips = listOf("Make it shorter", "Make it simpler", "Add something I forgot", "Something else")

    fun message(documentLabel: String, line: String, note: String): String = "Change this line in my $documentLabel: “$line” — ${note.trim()}"
}
