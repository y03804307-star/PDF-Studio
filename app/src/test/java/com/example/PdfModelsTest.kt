package com.example

import com.example.model.ImageFilterType
import com.example.model.TextAlignmentType
import com.example.model.TextElement
import com.example.model.WhiteoutElement
import org.junit.Assert.*
import org.junit.Test

class PdfModelsTest {

    @Test
    fun testTextElementCreationAndModification() {
        val element = TextElement(
            pageIndex = 0,
            text = "Initial Text",
            normalizedX = 0.2f,
            normalizedY = 0.3f,
            fontSizePt = 16f
        )

        assertEquals("Initial Text", element.text)
        assertEquals(0, element.pageIndex)
        assertEquals(0.2f, element.normalizedX, 0.001f)
        assertEquals(0.3f, element.normalizedY, 0.001f)

        val modified = element.copy(
            text = "Updated Heading",
            fontSizePt = 24f,
            isBold = true,
            alignment = TextAlignmentType.CENTER,
            backgroundColorArgb = 0xFFFFFFFFL
        )

        assertEquals("Updated Heading", modified.text)
        assertEquals(24f, modified.fontSizePt, 0.001f)
        assertTrue(modified.isBold)
        assertEquals(TextAlignmentType.CENTER, modified.alignment)
        assertNotNull(modified.backgroundColorArgb)
    }

    @Test
    fun testWhiteoutElementPatch() {
        val whiteout = WhiteoutElement(
            pageIndex = 1,
            normalizedX = 0.1f,
            normalizedY = 0.5f,
            normalizedWidth = 0.4f,
            normalizedHeight = 0.06f,
            colorArgb = 0xFFFFFFFFL
        )

        assertEquals(1, whiteout.pageIndex)
        assertEquals(0.4f, whiteout.normalizedWidth, 0.001f)
        assertEquals(0xFFFFFFFFL, whiteout.colorArgb)
    }
}
