package app.kwela.domain

import java.time.ZoneId

data class FileEntry(val id: String, val title: String, val groupId: String?, val groupTitle: String?, val createdAt: Long)

/** The documents Kwela wrote for one job application. */
data class ApplicationFiles(val id: String, val title: String, val files: List<FileEntry>, val at: Long)

/** Applications that share a date heading, such as "Today" or "Last week". */
data class DatedFiles(val label: String, val applications: List<ApplicationFiles>)

/** Arranges saved documents the way a distracted person can find them: newest first, one card per job, under date headings. */
object FileGroups {
    fun group(files: List<FileEntry>, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<DatedFiles> {
        val applications = files.groupBy { it.groupId ?: "single-${it.id}" }
            .map { (key, members) ->
                val ordered = members.sortedBy { it.createdAt }
                ApplicationFiles(key, ordered.first().groupTitle ?: ordered.first().title, ordered, ordered.maxOf { it.createdAt })
            }
            .sortedByDescending { it.at }
        return applications.groupBy { DateSection.label(it.at, now, zone) }.map { (label, apps) -> DatedFiles(label, apps) }
    }
}
