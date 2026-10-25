package app.kwela.domain

/** Words for the writer's quick review questions and the person's tapped answers. */
object ReviewPrompts {
    private const val MAX_QUOTED = 60
    private const val ASKED_PREFIX = "Quick one:"
    private const val MAX_QUESTIONS = 2

    /** One short bubble quoting the person's own line. */
    fun bubble(finding: ReviewFinding): String {
        val quoted = if (finding.line.length > MAX_QUOTED) finding.line.take(MAX_QUOTED - 3) + "…" else finding.line
        return "Quick one: “$quoted” ${finding.question}"
    }

    /** The message sent when the person taps one of the quick answers. */
    fun answer(finding: ReviewFinding, option: String): String = "About “${finding.line}”: $option"

    /** True while fewer than two quick questions appear among the recent messages. After that the writer stops asking and lets the person decide. */
    fun shouldAsk(recentAssistantTexts: List<String>): Boolean = recentAssistantTexts.count { it.startsWith(ASKED_PREFIX) } < MAX_QUESTIONS
}
