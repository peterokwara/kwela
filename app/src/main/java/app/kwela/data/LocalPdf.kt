package app.kwela.data

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import app.kwela.domain.DocBlock
import app.kwela.domain.DocumentBlocks
import java.io.ByteArrayOutputStream

/** A plain but clean PDF made on the phone. Used when the full design can't be fetched, for example with no internet. */
object LocalPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun bytes(body: String): ByteArray {
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.5f; color = android.graphics.Color.rgb(28, 39, 51) }
        val muted = Paint(text).apply { color = android.graphics.Color.rgb(90, 98, 110) }
        val title = Paint(text).apply { textSize = 20f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        val heading = Paint(text).apply { textSize = 11.5f; typeface = Typeface.create("sans-serif", Typeface.BOLD); letterSpacing = 0.06f }
        val width = PAGE_WIDTH - 2 * MARGIN

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var y = MARGIN

        fun draw(line: String, paint: Paint, indent: Float, leading: Float, before: Float = 0f) {
            y += before
            var rest = line
            while (rest.isNotEmpty()) {
                val count = paint.breakText(rest, true, width - indent, null).coerceAtLeast(1)
                val cut = if (count < rest.length) rest.lastIndexOf(' ', count).takeIf { it > 0 } ?: count else count
                if (y > PAGE_HEIGHT - MARGIN) {
                    pdf.finishPage(page)
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++pageNumber).create())
                    y = MARGIN
                }
                page.canvas.drawText(rest.substring(0, cut).trim(), MARGIN + indent, y, paint)
                y += leading
                rest = rest.substring(cut).trim()
            }
        }

        DocumentBlocks.parse(body).forEach { block ->
            when (block) {
                is DocBlock.Title -> draw(block.text, title, 0f, 26f)
                is DocBlock.Subtitle -> draw(block.text, muted, 0f, 18f)
                is DocBlock.Heading -> draw(block.text, heading, 0f, 17f, before = 14f)
                is DocBlock.Paragraph -> draw(block.text, text, 0f, 16f, before = 3f)
                is DocBlock.Bullet -> draw("•  ${block.text}", text, 10f, 16f)
            }
        }
        pdf.finishPage(page)
        return ByteArrayOutputStream().also { pdf.writeTo(it); pdf.close() }.toByteArray()
    }
}
