package app.kwela.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.kwela.domain.AttachmentPolicy
import app.kwela.domain.OutgoingFile
import java.io.ByteArrayOutputStream

/** Describes a file the user picked, before it is sent. */
data class PickedFile(val name: String, val mediaType: String, val sizeBytes: Long)

/** Reads picked files from Android's content provider. */
class AttachmentReader(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun describe(uri: Uri): PickedFile {
        var name = "Selected file"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { cursor.getString(it)?.let { value -> name = value } }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!cursor.isNull(it)) size = cursor.getLong(it) }
            }
        }
        return PickedFile(name, AttachmentPolicy.mediaType(name, resolver.getType(uri)), size)
    }

    /** Reads the file as a data URL, or returns null when it is missing, unreadable or too large. */
    fun read(id: String, uri: Uri): OutgoingFile? = runCatching {
        val file = describe(uri)
        val output = ByteArrayOutputStream()
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                if (!AttachmentPolicy.isSmallEnough(output.size().toLong())) return null
            }
        } ?: return null
        OutgoingFile(id, file.name, file.mediaType, AttachmentPolicy.dataUrl(file.mediaType, output.toByteArray()))
    }.getOrNull()
}
