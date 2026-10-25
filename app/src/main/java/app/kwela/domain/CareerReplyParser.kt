package app.kwela.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class ReplyDocument(val title: String, val body: String)

data class CareerReply(val message: String, val documents: List<ReplyDocument>, val candidateProfileJson: String?, val applicationJson: String? = null)

/** Reads the JSON reply from the Kwela API into chat text and readable documents. */
object CareerReplyParser {
    fun parse(body: String): CareerReply? {
        val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
        val message = root.text("message") ?: return null
        val profile = root["candidateProfile"]?.takeIf { it !is JsonNull }?.toString()
        val pack = root["applicationPack"] as? JsonObject
        return CareerReply(message, if (pack != null) documentsFrom(pack) else emptyList(), profile, pack?.toString())
    }

    private fun documentsFrom(pack: JsonObject): List<ReplyDocument> {
        val job = pack.obj("job")
        val label = listOf(job?.text("title"), job?.text("company")).filter { !it.isNullOrBlank() }.joinToString(", ").ifBlank { "Application" }
        val documents = mutableListOf<ReplyDocument>()
        pack.obj("resume")?.let { documents += ReplyDocument("CV - $label", formatResume(it)) }
        pack.obj("coverLetter")?.let { documents += ReplyDocument("Cover letter - $label", formatCoverLetter(it)) }
        val answers = (pack["applicationAnswers"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        if (answers.isNotEmpty()) {
            documents += ReplyDocument("Application answers - $label", answers.joinToString("\n\n") { "Q: ${it.text("question").orEmpty()}\nA: ${it.text("answer").orEmpty()}" })
        }
        return documents
    }

    private fun formatResume(resume: JsonObject): String {
        val details = resume.obj("personalDetails")
        val name = listOf(details?.text("firstName"), details?.text("lastName")).filter { !it.isNullOrBlank() }.joinToString(" ")
        val place = listOf(details?.text("city"), details?.text("country")).filter { !it.isNullOrBlank() }.joinToString(", ")
        val contact = listOf(details?.text("email"), details?.text("phoneNumber"), place).filter { !it.isNullOrBlank() }.joinToString(" · ")
        val sections = mutableListOf<String>()
        sections += listOf(name, contact).filter { it.isNotBlank() }.joinToString("\n")
        section("PROFESSIONAL SUMMARY", resume.text("professionalSummary"))?.let { sections += it }
        val jobs = resume.list("employments").map { job ->
            val heading = listOf(job.text("jobTitle"), job.text("employer")).filter { !it.isNullOrBlank() }.joinToString(", ") + dates(job)
            (listOf(heading) + job.strings("achievements").map { "- $it" }).joinToString("\n")
        }
        if (jobs.isNotEmpty()) sections += "EXPERIENCE\n" + jobs.joinToString("\n\n")
        val schools = resume.list("education").map { school ->
            listOf(school.text("degree"), school.text("institution")).filter { !it.isNullOrBlank() }.joinToString(", ") + dates(school)
        }
        if (schools.isNotEmpty()) sections += "EDUCATION\n" + schools.joinToString("\n")
        section("SKILLS", resume.text("skills"))?.let { sections += it }
        return sections.filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun formatCoverLetter(letter: JsonObject): String {
        val contact = listOf(letter.text("email"), letter.text("phoneNumber")).filter { !it.isNullOrBlank() }.joinToString(" · ")
        val header = listOf(letter.text("fullName").orEmpty(), contact).filter { it.isNotBlank() }.joinToString("\n")
        return listOf(header, letter.text("companyName").orEmpty(), letter.text("letterContent").orEmpty()).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun section(title: String, body: String?): String? = body?.takeIf { it.isNotBlank() }?.let { "$title\n$it" }

    private fun dates(entry: JsonObject): String {
        val start = entry.text("startDate").orEmpty()
        val present = (entry["isPresent"] as? JsonPrimitive)?.booleanOrNull == true
        val end = if (present) "Present" else entry.text("endDate").orEmpty()
        return when {
            start.isBlank() && end.isBlank() -> ""
            start.isBlank() -> " ($end)"
            end.isBlank() -> " ($start)"
            else -> " ($start - $end)"
        }
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.list(key: String): List<JsonObject> = (this[key] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    private fun JsonObject.strings(key: String): List<String> = (this[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
    private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
}
