package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.model.PageDimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererHelper(private val context: Context) {

    private var currentFileDescriptor: ParcelFileDescriptor? = null
    private var currentRenderer: PdfRenderer? = null
    private var currentPageCount: Int = 0
    private var pageDimensions: MutableList<PageDimension> = mutableListOf()

    fun isReady(): Boolean = currentRenderer != null

    fun getPageCount(): Int = currentPageCount

    fun getPageDimension(pageIndex: Int): PageDimension {
        if (pageIndex in pageDimensions.indices) {
            return pageDimensions[pageIndex]
        }
        return PageDimension(595, 842) // standard A4
    }

    suspend fun loadPdfFile(file: File): Boolean = withContext(Dispatchers.IO) {
        close()
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            currentFileDescriptor = pfd
            currentRenderer = renderer
            currentPageCount = renderer.pageCount
            pageDimensions.clear()

            for (i in 0 until currentPageCount) {
                val page = renderer.openPage(i)
                pageDimensions.add(PageDimension(page.width, page.height))
                page.close()
            }
            true
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error loading PDF file: ${file.path}", e)
            close()
            false
        }
    }

    suspend fun loadPdfUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            if (loadPdfFile(tempFile)) {
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error loading PDF URI: $uri", e)
            null
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = currentRenderer ?: return@withContext null
        if (pageIndex !in 0 until currentPageCount) return@withContext null

        try {
            val page = renderer.openPage(pageIndex)
            val originalWidth = page.width
            val originalHeight = page.height

            // Calculate scaled dimensions (crisp rendering)
            val scale = (targetWidth.toFloat() / originalWidth.toFloat()).coerceIn(1.0f, 3.0f)
            val renderWidth = (originalWidth * scale).toInt()
            val renderHeight = (originalHeight * scale).toInt()

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            // Fill with pure white background before rendering
            bitmap.eraseColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error rendering page $pageIndex", e)
            null
        }
    }

    fun close() {
        try {
            currentRenderer?.close()
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error closing renderer", e)
        }
        currentRenderer = null

        try {
            currentFileDescriptor?.close()
        } catch (e: Exception) {
            Log.e("PdfRendererHelper", "Error closing descriptor", e)
        }
        currentFileDescriptor = null
        currentPageCount = 0
        pageDimensions.clear()
    }
}
