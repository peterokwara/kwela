package app.kwela.domain

/** "Let me have a look at the CV": what a person wants to see, understood on the phone so it opens instantly. */
enum class ShowRequest(val label: String, val kind: String) {
    CV("CV", "resume"), LETTER("cover letter", "cover_letter"), JOB("job breakdown", "job"), ANSWERS("answers", "answers");

    /** Whether a saved file is the kind asked for. Older files saved without a kind are matched by their title. */
    fun matches(fileKind: String?, title: String): Boolean = when {
        !fileKind.isNullOrEmpty() -> fileKind == kind
        this == CV -> FileTab.of(null, title) == FileTab.RESUMES
        this == LETTER -> FileTab.of(null, title) == FileTab.LETTERS
        else -> false
    }

    companion object {
        private val looking = Regex("\\b(look|see|show|view|open|pull up|display|where|send me|give me|have a)\\b")
        private val changing = Regex("\\b(change|edit|make|write|draft|update|redo|rewrite|prepare|fix|shorter|simpler|add|do my|review|typo|weak|improve|tell me|about|why|how|better|good|mistake|wrong|score|proofread|read)\\b")

        /** The file being asked for, or null when this is not a "show me" request and the writer should answer. */
        fun parse(text: String): ShowRequest? {
            val said = text.lowercase()
            val words = said.split(Regex("\\s+")).size
            // A bare "cv pls" counts too: someone mid-task types as little as they can.
            if (words > 12 || (words > 4 && !looking.containsMatchIn(said)) || changing.containsMatchIn(said)) return null
            return when {
                Regex("\\b(job (description|breakdown|summary|ad|details)|the job|jd|the ad)\\b").containsMatchIn(said) -> JOB
                Regex("\\b(cover letter|letter)\\b").containsMatchIn(said) -> LETTER
                Regex("\\b(cv|resume|resum\\u00e9)\\b").containsMatchIn(said) -> CV
                Regex("\\banswers?\\b").containsMatchIn(said) -> ANSWERS
                else -> null
            }
        }
    }
}
