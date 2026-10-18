package com.example.model

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import java.util.UUID

enum class EditorTool(val title: String) {
    BROWSE("Browse"),
    TEXT("Text"),
    IMAGE("Image"),
    WHITEOUT("Whiteout"),
    DRAW("Draw"),
    HIGHLIGHT("Highlight"),
    SIGNATURE("Signature"),
    PAGES("Pages")
}

enum class TextAlignmentType {
    LEFT, CENTER, RIGHT
}

enum class ImageFilterType {
    NONE, GRAYSCALE, SEPIA, HIGH_CONTRAST
}

data class PageDimension(
    val width: Int,
    val height: Int
)

data class TextElement(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val text: String,
    val normalizedX: Float, // 0.0 .. 1.0 relative to page width
    val normalizedY: Float, // 0.0 .. 1.0 relative to page height
    val normalizedWidth: Float = 0.35f,
    val normalizedHeight: Float = 0.08f,
    val fontSizePt: Float = 16f,
    val colorArgb: Long = 0xFF1E293BL, // dark slate
    val backgroundColorArgb: Long? = null, // null for transparent, or e.g. 0xFFFFFFFF for whiteout backing
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val alignment: TextAlignmentType = TextAlignmentType.LEFT,
    val fontFamilyType: String = "SANS_SERIF", // SANS_SERIF, SERIF, MONOSPACE
    val rotation: Float = 0f
)

data class ImageElement(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val bitmap: Bitmap? = null,
    val uriString: String? = null,
    val drawableResId: Int? = null,
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedWidth: Float = 0.35f,
    val normalizedHeight: Float = 0.25f,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f,
    val filter: ImageFilterType = ImageFilterType.NONE
)

data class WhiteoutElement(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedWidth: Float = 0.3f,
    val normalizedHeight: Float = 0.05f,
    val colorArgb: Long = 0xFFFFFFFFL
)

data class DrawPoint(
    val normalizedX: Float,
    val normalizedY: Float
)

data class DrawStroke(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val points: List<DrawPoint>,
    val colorArgb: Long = 0xFFE11D48L,
    val strokeWidth: Float = 4f, // in points
    val isHighlighter: Boolean = false
)

data class SignatureElement(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val bitmap: Bitmap,
    val normalizedX: Float = 0.6f,
    val normalizedY: Float = 0.8f,
    val normalizedWidth: Float = 0.28f,
    val normalizedHeight: Float = 0.10f
)

sealed class SelectedElement {
    data class Text(val element: TextElement) : SelectedElement()
    data class Image(val element: ImageElement) : SelectedElement()
    data class Whiteout(val element: WhiteoutElement) : SelectedElement()
    data class Signature(val element: SignatureElement) : SelectedElement()
}

data class SampleDocument(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val previewResId: Int? = null
)
