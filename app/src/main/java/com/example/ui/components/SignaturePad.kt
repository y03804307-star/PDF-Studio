package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaveSignature: (Bitmap) -> Unit
) {
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var penColor by remember { mutableStateOf(Color(0xFF0F172A)) } // Deep Ink Black
    var penWidth by remember { mutableFloatStateOf(6f) }

    val colors = listOf(
        Color(0xFF0F172A), // Ink Black
        Color(0xFF1D4ED8), // Royal Navy Blue
        Color(0xFFE11D48)  // Crimson Red
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign Document",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Draw your official signature with your finger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Signature Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFAFAFA))
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(penColor, penWidth) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStroke = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke = currentStroke + change.position
                                    },
                                    onDragEnd = {
                                        if (currentStroke.size >= 2) {
                                            strokes = strokes + listOf(currentStroke)
                                        }
                                        currentStroke = emptyList()
                                    },
                                    onDragCancel = {
                                        currentStroke = emptyList()
                                    }
                                )
                            }
                    ) {
                        // Base signature guide line
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(30f, size.height - 40f),
                            end = Offset(size.width - 30f, size.height - 40f),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw saved strokes
                        for (stroke in strokes) {
                            if (stroke.size < 2) continue
                            val p = Path()
                            p.moveTo(stroke.first().x, stroke.first().y)
                            for (i in 1 until stroke.size) {
                                p.lineTo(stroke[i].x, stroke[i].y)
                            }
                            drawPath(
                                path = p,
                                color = penColor,
                                style = Stroke(width = penWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Draw current active stroke
                        if (currentStroke.size >= 2) {
                            val p = Path()
                            p.moveTo(currentStroke.first().x, currentStroke.first().y)
                            for (i in 1 until currentStroke.size) {
                                p.lineTo(currentStroke[i].x, currentStroke[i].y)
                            }
                            drawPath(
                                path = p,
                                color = penColor,
                                style = Stroke(width = penWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color picker & Clear button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (penColor == c) 2.5.dp else 1.dp,
                                        color = if (penColor == c) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .pointerInput(c) {
                                        detectDragGestures { _, _ -> }
                                    }
                                    .padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = { penColor = c },
                                    modifier = Modifier.fillMaxSize()
                                ) {}
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            strokes = emptyList()
                            currentStroke = emptyList()
                        },
                        modifier = Modifier.testTag("clear_signature_btn")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                val bmp = renderStrokesToBitmap(strokes, penColor, penWidth)
                                onSaveSignature(bmp)
                            } else {
                                onDismiss()
                            }
                        },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.testTag("insert_signature_btn")
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Insert", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Insert Signature")
                    }
                }
            }
        }
    }
}

private fun renderStrokesToBitmap(
    strokes: List<List<Offset>>,
    color: Color,
    strokeWidth: Float
): Bitmap {
    val width = 600
    val height = 300
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.strokeWidth = strokeWidth * 1.5f
    }

    for (stroke in strokes) {
        if (stroke.size < 2) continue
        val path = android.graphics.Path()
        path.moveTo(stroke.first().x, stroke.first().y)
        for (i in 1 until stroke.size) {
            path.lineTo(stroke[i].x, stroke[i].y)
        }
        canvas.drawPath(path, paint)
    }

    return bitmap
}
