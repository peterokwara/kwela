package app.kwela.domain

import java.util.Base64

/** Which files Kwela accepts in chat. Mirrors the limits enforced by the Kwela API. */
object AttachmentPolicy {
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024
    private val mediaTypesByExtension = mapOf(
        "pdf" to "application/pdf",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "txt" to "text/plain",
        "md" to "text/markdown",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp"
    )
    private val acceptedMediaTypes = mediaTypesByExtension.values.toSet() + "image/jpg" + "application/octet-stream"

    fun isSupported(name: String, mediaType: String?): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in mediaTypesByExtension && (mediaType.isNullOrBlank() || mediaType in acceptedMediaTypes)
    }

    fun mediaType(name: String, reported: String?): String {
        if (!reported.isNullOrBlank() && reported != "application/octet-stream") return reported
        return mediaTypesByExtension[name.substringAfterLast('.', "").lowercase()] ?: "application/octet-stream"
    }

    fun dataUrl(mediaType: String, bytes: ByteArray): String =
        "data:$mediaType;base64," + Base64.getEncoder().encodeToString(bytes)

    fun isSmallEnough(bytes: Long): Boolean = bytes <= MAX_FILE_BYTES
}
