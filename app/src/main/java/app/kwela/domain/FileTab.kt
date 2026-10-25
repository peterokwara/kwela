package app.kwela.domain

/** The tabs of the Files screen. Jobs shows everything grouped per job, so it has no kind of its own. */
enum class FileTab(val label: String) {
    JOBS("Jobs"), RESUMES("Resumes"), LETTERS("Letters");

    companion object {
        /** Which kind tab a document belongs to, or null when it only appears under Jobs. */
        fun of(kind: String?, title: String): FileTab? = when (kind) {
            "resume" -> RESUMES
            "cover_letter" -> LETTERS
            "answers" -> null
            else -> when {
                title.contains("letter", ignoreCase = true) -> LETTERS
                title.contains("cv", ignoreCase = true) || title.contains("resume", ignoreCase = true) -> RESUMES
                else -> null
            }
        }
    }
}
