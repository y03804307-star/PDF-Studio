package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun InteractiveElementBox(
    isSelected: Boolean,
    normalizedX: Float,
    normalizedY: Float,
    normalizedWidth: Float,
    normalizedHeight: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    rotation: Float = 0f,
    onMove: (newX: Float, newY: Float) -> Unit,
    onResize: (newWidth: Float, newHeight: Float) -> Unit,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    if (canvasWidthPx <= 0f || canvasHeightPx <= 0f) return

    val density = LocalDensity.current
    val leftPx = (normalizedX * canvasWidthPx).coerceAtLeast(0f)
    val topPx = (normalizedY * canvasHeightPx).coerceAtLeast(0f)
    val widthPx = (normalizedWidth * canvasWidthPx).coerceAtLeast(30f)
    val heightPx = (normalizedHeight * canvasHeightPx).coerceAtLeast(20f)

    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .size(widthDp, heightDp)
            .rotate(rotation)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onSelect() }
            .pointerInput(canvasWidthPx, canvasHeightPx, normalizedX, normalizedY) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaX = dragAmount.x / canvasWidthPx
                    val deltaY = dragAmount.y / canvasHeightPx
                    onMove(normalizedX + deltaX, normalizedY + deltaY)
                }
            }
    ) {
        content()

        if (isSelected) {
            // Delete button at top-right
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
                    .testTag("element_delete_btn"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(14.dp)
                )
            }

            // Edit button if provided at top-left
            if (onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-10).dp, y = (-10).dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("element_edit_btn"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Resize handle at bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(canvasWidthPx, canvasHeightPx, normalizedWidth, normalizedHeight) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaW = dragAmount.x / canvasWidthPx
                            val deltaH = dragAmount.y / canvasHeightPx
                            onResize(normalizedWidth + deltaW, normalizedHeight + deltaH)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "Resize",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
