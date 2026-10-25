package app.kwela.domain

/** Keeps one CV, one cover letter and one job breakdown per job, so a revision updates a file instead of piling up copies. */
object DocumentUpdates {
    /** [groupId] is the existing job to file under (null for a new job). [replace] maps a kind to the saved file it overwrites. */
    data class Plan(val groupId: String?, val replace: Map<String, String>)

    private const val UNNAMED = "application"

    fun plan(saved: List<FileEntry>, groupTitle: String, incomingKinds: List<String>): Plan {
        val wanted = groupTitle.trim().lowercase()
        if (wanted.isEmpty() || wanted == UNNAMED) return Plan(null, emptyMap())
        val sameJob = saved.filter { it.groupId != null && it.groupTitle?.trim()?.lowercase() == wanted }
        val groupId = sameJob.maxByOrNull { it.createdAt }?.groupId ?: return Plan(null, emptyMap())
        val inGroup = sameJob.filter { it.groupId == groupId }
        val replace = incomingKinds.mapNotNull { kind -> inGroup.filter { it.kind == kind }.maxByOrNull { it.createdAt }?.let { kind to it.id } }.toMap()
        return Plan(groupId, replace)
    }
}
