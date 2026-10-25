package app.kwela.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.data.remote.KwelaApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A PDF ready to save or share, and whether it has the full design. */
class PdfFile(val bytes: ByteArray, val fullDesign: Boolean)

/** Makes, saves and shares the PDF of a document. */
class DocumentPdf(private val context: Context, private val api: KwelaApi, private val repository: KwelaRepository) {
    /** The full-design PDF from the server when possible. Otherwise a plain PDF made on the phone, so this never fails. */
    suspend fun make(document: SavedDocumentEntity): PdfFile {
        val type = when (document.kind) {
            "resume" -> "resume"
            "cover_letter" -> "cover_letter"
            else -> null
        }
        val source = document.sourceJson
        val token = repository.sessionToken()
        if (type != null && source != null && token != null) {
            api.renderPdf(token, type, source)?.let { return PdfFile(it, fullDesign = true) }
        }
        return PdfFile(withContext(Dispatchers.Default) { LocalPdf.bytes(document.body) }, fullDesign = false)
    }

    /** Saves into the phone's Downloads folder. Returns null on Android 8 and 9, where the person picks the place instead. */
    suspend fun saveToDownloads(fileName: String, bytes: ByteArray): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        val written = runCatching { resolver.openOutputStream(uri)?.use { it.write(bytes) } != null }.getOrDefault(false)
        if (!written) {
            resolver.delete(uri, null, null)
            return@withContext null
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        uri
    }

    /** Writes the PDF where the share sheet can read it and returns its address. */
    suspend fun forSharing(document: SavedDocumentEntity, bytes: ByteArray): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(directory, "${document.id}.pdf").also { it.writeBytes(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
