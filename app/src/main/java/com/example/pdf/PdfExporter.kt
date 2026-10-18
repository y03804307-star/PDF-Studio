package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PdfExporter(private val context: Context) {

    suspend fun exportPdf(
        pageCount: Int,
        pageDimensions: List<PageDimension>,
        baseBitmaps: Map<Int, Bitmap>,
        texts: List<TextElement>,
        images: List<ImageElement>,
        whiteouts: List<WhiteoutElement>,
        drawStrokes: List<DrawStroke>,
        signatures: List<SignatureElement>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        try {
            for (pageIndex in 0 until pageCount) {
                val dimension = if (pageIndex in pageDimensions.indices) {
                    pageDimensions[pageIndex]
                } else {
                    PageDimension(595, 842)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(dimension.width, dimension.height, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw base page bitmap if available
                val baseBitmap = baseBitmaps[pageIndex]
                if (baseBitmap != null) {
                    val destRect = RectF(0f, 0f, dimension.width.toFloat(), dimension.height.toFloat())
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    canvas.drawBitmap(baseBitmap, null, destRect, paint)
                } else {
                    canvas.drawColor(Color.WHITE)
                }

                val pageW = dimension.width.toFloat()
                val pageH = dimension.height.toFloat()

                // 2. Draw whiteouts (patches covering underlying elements)
                val pageWhiteouts = whiteouts.filter { it.pageIndex == pageIndex }
                val whiteoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                }
                for (whiteout in pageWhiteouts) {
                    whiteoutPaint.color = whiteout.colorArgb.toInt()
                    val left = whiteout.normalizedX * pageW
                    val top = whiteout.normalizedY * pageH
                    val right = left + (whiteout.normalizedWidth * pageW)
                    val bottom = top + (whiteout.normalizedHeight * pageH)
                    canvas.drawRect(left, top, right, bottom, whiteoutPaint)
                }

                // 3. Draw images
                val pageImages = images.filter { it.pageIndex == pageIndex }
                for (img in pageImages) {
                    val bmp = img.bitmap
                    if (bmp != null && !bmp.isRecycled) {
                        val left = img.normalizedX * pageW
                        val top = img.normalizedY * pageH
                        val w = img.normalizedWidth * pageW
                        val h = img.normalizedHeight * pageH

                        canvas.save()
                        val cx = left + w / 2f
                        val cy = top + h / 2f
                        if (img.rotation != 0f) {
                            canvas.rotate(img.rotation, cx, cy)
                        }

                        val imagePaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
                            alpha = (img.opacity * 255).toInt().coerceIn(0, 255)
                            colorFilter = createColorFilter(img.filter)
                        }

                        val destRect = RectF(left, top, left + w, top + h)
                        canvas.drawBitmap(bmp, null, destRect, imagePaint)
                        canvas.restore()
                    }
                }

                // 4. Draw freehand drawing strokes and highlighters
                val pageStrokes = drawStrokes.filter { it.pageIndex == pageIndex }
                for (stroke in pageStrokes) {
                    if (stroke.points.size < 2) continue

                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = stroke.strokeWidth
                        if (stroke.isHighlighter) {
                            color = stroke.colorArgb.toInt()
                            alpha = 110 // semi-transparent highlighter
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                        } else {
                            color = stroke.colorArgb.toInt()
                        }
                    }

                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(first.normalizedX * pageW, first.normalizedY * pageH)

                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.normalizedX * pageW, pt.normalizedY * pageH)
                    }
                    canvas.drawPath(path, strokePaint)
                }

                // 5. Draw signatures
                val pageSignatures = signatures.filter { it.pageIndex == pageIndex }
                for (sig in pageSignatures) {
                    if (!sig.bitmap.isRecycled) {
                        val left = sig.normalizedX * pageW
                        val top = sig.normalizedY * pageH
                        val w = sig.normalizedWidth * pageW
                        val h = sig.normalizedHeight * pageH
                        val destRect = RectF(left, top, left + w, top + h)
                        val sigPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                        canvas.drawBitmap(sig.bitmap, null, destRect, sigPaint)
                    }
                }

                // 6. Draw text elements
                val pageTexts = texts.filter { it.pageIndex == pageIndex }
                for (txt in pageTexts) {
                    val left = txt.normalizedX * pageW
                    val top = txt.normalizedY * pageH
                    val boxW = (txt.normalizedWidth * pageW).coerceAtLeast(30f)
                    val boxH = (txt.normalizedHeight * pageH).coerceAtLeast(20f)

                    canvas.save()
                    val cx = left + boxW / 2f
                    val cy = top + boxH / 2f
                    if (txt.rotation != 0f) {
                        canvas.rotate(txt.rotation, cx, cy)
                    }

                    // Background box if present
                    if (txt.backgroundColorArgb != null) {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = txt.backgroundColorArgb.toInt()
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(left, top, left + boxW, top + boxH, bgPaint)
                    }

                    val typefaceStyle = when {
                        txt.isBold && txt.isItalic -> Typeface.BOLD_ITALIC
                        txt.isBold -> Typeface.BOLD
                        txt.isItalic -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    }

                    val baseTypeface = when (txt.fontFamilyType) {
                        "SERIF" -> Typeface.SERIF
                        "MONOSPACE" -> Typeface.MONOSPACE
                        else -> Typeface.SANS_SERIF
                    }

                    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = txt.colorArgb.toInt()
                        textSize = txt.fontSizePt
                        typeface = Typeface.create(baseTypeface, typefaceStyle)
                    }

                    val textAlignment = when (txt.alignment) {
                        TextAlignmentType.CENTER -> Layout.Alignment.ALIGN_CENTER
                        TextAlignmentType.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                        else -> Layout.Alignment.ALIGN_NORMAL
                    }

                    val staticLayout = StaticLayout.Builder
                        .obtain(txt.text, 0, txt.text.length, textPaint, boxW.toInt().coerceAtLeast(10))
                        .setAlignment(textAlignment)
                        .setIncludePad(false)
                        .build()

                    canvas.save()
                    canvas.translate(left, top)
                    staticLayout.draw(canvas)
                    canvas.restore()

                    canvas.restore()
                }

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            true
        } catch (e: Exception) {
            Log.e("PdfExporter", "Error exporting PDF", e)
            false
        } finally {
            document.close()
        }
    }

    private fun createColorFilter(filter: ImageFilterType): ColorFilter? {
        return when (filter) {
            ImageFilterType.NONE -> null
            ImageFilterType.GRAYSCALE -> {
                val matrix = ColorMatrix().apply { setSaturation(0f) }
                ColorMatrixColorFilter(matrix)
            }
            ImageFilterType.SEPIA -> {
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val sepiaMatrix = ColorMatrix().apply {
                    setScale(1f, 0.95f, 0.82f, 1.0f)
                }
                matrix.postConcat(sepiaMatrix)
                ColorMatrixColorFilter(matrix)
            }
            ImageFilterType.HIGH_CONTRAST -> {
                val contrast = 1.5f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val matrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorMatrixColorFilter(matrix)
            }
        }
    }
}
