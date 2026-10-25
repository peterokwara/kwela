package app.kwela.domain

/** The tabs of the Files screen, like WhatsApp's media, docs and links. Answers sit with the letters, since both are words for an application. */
enum class FileTab(val label: String) {
    JOBS("Jobs"), RESUMES("Resumes"), LETTERS("Letters");

    companion object {
        /** Which kind tab a document belongs to, or null when it fits none of them. */
        fun of(kind: String?, title: String): FileTab? = when (kind) {
            "resume" -> RESUMES
            "cover_letter" -> LETTERS
            "answers" -> LETTERS
            "job" -> JOBS
            else -> when {
                title.contains("letter", ignoreCase = true) -> LETTERS
                title.contains("cv", ignoreCase = true) || title.contains("resume", ignoreCase = true) -> RESUMES
                else -> null
            }
        }
    }
}
