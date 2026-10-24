package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPolicyTest {
    @Test
    fun acceptsCvDocumentsAndPhotos() {
        assertTrue(AttachmentPolicy.isSupported("cv.pdf", "application/pdf"))
        assertTrue(AttachmentPolicy.isSupported("My CV.DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertTrue(AttachmentPolicy.isSupported("photo.jpg", "image/jpeg"))
        assertTrue(AttachmentPolicy.isSupported("notes.txt", "text/plain"))
    }

    @Test
    fun refusesOtherFileTypes() {
        assertFalse(AttachmentPolicy.isSupported("virus.exe", "application/octet-stream"))
        assertFalse(AttachmentPolicy.isSupported("movie.mp4", "video/mp4"))
        assertFalse(AttachmentPolicy.isSupported("cv.pdf", "video/mp4"))
    }

    @Test
    fun fillsInAMissingMediaTypeFromTheFileName() {
        assertEquals("application/pdf", AttachmentPolicy.mediaType("cv.pdf", null))
        assertEquals("image/png", AttachmentPolicy.mediaType("scan.png", "application/octet-stream"))
        assertEquals("text/plain", AttachmentPolicy.mediaType("notes.txt", "text/plain"))
    }

    @Test
    fun encodesBytesAsABase64DataUrl() {
        assertEquals("data:text/plain;base64,aGVsbG8=", AttachmentPolicy.dataUrl("text/plain", "hello".toByteArray()))
    }

    @Test
    fun limitsEachFileToFiveMegabytes() {
        assertTrue(AttachmentPolicy.isSmallEnough(5L * 1024 * 1024))
        assertFalse(AttachmentPolicy.isSmallEnough(5L * 1024 * 1024 + 1))
    }
}
