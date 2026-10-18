package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.DrawPoint
import com.example.model.DrawStroke
import com.example.model.EditorTool

@Composable
fun DrawingCanvas(
    tool: EditorTool,
    pageIndex: Int,
    strokes: List<DrawStroke>,
    onAddStroke: (DrawStroke) -> Unit,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    brushColor: Color = Color(0xFFE11D48),
    highlighterColor: Color = Color(0xFFFFD54F),
    strokeWidthPt: Float = 4f
) {
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val isDrawingActive = tool == EditorTool.DRAW || tool == EditorTool.HIGHLIGHT

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDrawingActive && canvasWidthPx > 0 && canvasHeightPx > 0) {
                    Modifier.pointerInput(tool, pageIndex, canvasWidthPx, canvasHeightPx) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activePoints = activePoints + change.position
                            },
                            onDragEnd = {
                                if (activePoints.size >= 2) {
                                    val isHighlighter = tool == EditorTool.HIGHLIGHT
                                    val colorLong = if (isHighlighter) {
                                        (highlighterColor.value.toLong())
                                    } else {
                                        (brushColor.value.toLong())
                                    }
                                    val normalized = activePoints.map {
                                        DrawPoint(
                                            normalizedX = (it.x / canvasWidthPx).coerceIn(0f, 1f),
                                            normalizedY = (it.y / canvasHeightPx).coerceIn(0f, 1f)
                                        )
                                    }
                                    val newStroke = DrawStroke(
                                        pageIndex = pageIndex,
                                        points = normalized,
                                        colorArgb = colorLong,
                                        strokeWidth = if (isHighlighter) strokeWidthPt * 3.5f else strokeWidthPt,
                                        isHighlighter = isHighlighter
                                    )
                                    onAddStroke(newStroke)
                                }
                                activePoints = emptyList()
                            },
                            onDragCancel = {
                                activePoints = emptyList()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw completed strokes for this page
        val pageStrokes = strokes.filter { it.pageIndex == pageIndex }
        for (stroke in pageStrokes) {
            if (stroke.points.size < 2) continue
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.normalizedX * w, first.normalizedY * h)

            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                path.lineTo(pt.normalizedX * w, pt.normalizedY * h)
            }

            val strokeColor = Color(stroke.colorArgb)
            drawPath(
                path = path,
                color = if (stroke.isHighlighter) strokeColor.copy(alpha = 0.45f) else strokeColor,
                style = Stroke(
                    width = stroke.strokeWidth * (w / 595f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = if (stroke.isHighlighter) BlendMode.Darken else BlendMode.SrcOver
            )
        }

        // 2. Draw live currently drawing stroke
        if (activePoints.size >= 2) {
            val livePath = Path()
            livePath.moveTo(activePoints.first().x, activePoints.first().y)
            for (i in 1 until activePoints.size) {
                livePath.lineTo(activePoints[i].x, activePoints[i].y)
            }
            val isHighlighter = tool == EditorTool.HIGHLIGHT
            val activeColor = if (isHighlighter) highlighterColor.copy(alpha = 0.45f) else brushColor
            drawPath(
                path = livePath,
                color = activeColor,
                style = Stroke(
                    width = (if (isHighlighter) strokeWidthPt * 3.5f else strokeWidthPt) * (w / 595f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = if (isHighlighter) BlendMode.Darken else BlendMode.SrcOver
            )
        }
    }
}
