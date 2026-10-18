package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.TextAlignmentType
import com.example.model.TextElement
import com.example.ui.components.ColorPickerRow

@Composable
fun TextEditDialog(
    element: TextElement,
    onDismiss: () -> Unit,
    onSave: (TextElement) -> Unit
) {
    var text by remember { mutableStateOf(element.text) }
    var fontSize by remember { mutableFloatStateOf(element.fontSizePt) }
    var isBold by remember { mutableStateOf(element.isBold) }
    var isItalic by remember { mutableStateOf(element.isItalic) }
    var alignment by remember { mutableStateOf(element.alignment) }
    var fontFamily by remember { mutableStateOf(element.fontFamilyType) }
    var textColor by remember { mutableStateOf(Color(element.colorArgb)) }
    var hasWhiteoutBacking by remember { mutableStateOf(element.backgroundColorArgb != null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Text Element",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Modify content, font style, and whiteout backing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Input Field
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text Content") },
                    placeholder = { Text("Type here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_edit_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card
                Text(
                    text = "Live Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(
                            if (hasWhiteoutBacking) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val previewFamily = when (fontFamily) {
                        "SERIF" -> FontFamily.Serif
                        "MONOSPACE" -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                    }
                    val previewAlign = when (alignment) {
                        TextAlignmentType.CENTER -> TextAlign.Center
                        TextAlignmentType.RIGHT -> TextAlign.End
                        else -> TextAlign.Start
                    }
                    Text(
                        text = if (text.isNotBlank()) text else "Sample preview text",
                        fontSize = (fontSize * 0.9f).sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = previewFamily,
                        color = textColor,
                        textAlign = previewAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Font Size: ${fontSize.toInt()} pt",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row {
                        IconButton(
                            onClick = { fontSize = (fontSize - 2f).coerceAtLeast(8f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { fontSize = (fontSize + 2f).coerceAtMost(64f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Larger", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 8f..56f,
                    steps = 24,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Style Controls: Bold, Italic, Alignment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bold & Italic
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold },
                            label = { Text("B", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = isItalic,
                            onClick = { isItalic = !isItalic },
                            label = { Text("I", fontStyle = FontStyle.Italic) }
                        )
                    }

                    // Alignment
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { alignment = TextAlignmentType.LEFT },
                            colors = if (alignment == TextAlignmentType.LEFT) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Left", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { alignment = TextAlignmentType.CENTER },
                            colors = if (alignment == TextAlignmentType.CENTER) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { alignment = TextAlignmentType.RIGHT },
                            colors = if (alignment == TextAlignmentType.RIGHT) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                        ) {
                            Icon(Icons.Default.FormatAlignRight, contentDescription = "Right", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Font Family Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = fontFamily == "SANS_SERIF",
                        onClick = { fontFamily = "SANS_SERIF" },
                        label = { Text("Sans", fontFamily = FontFamily.SansSerif) }
                    )
                    FilterChip(
                        selected = fontFamily == "SERIF",
                        onClick = { fontFamily = "SERIF" },
                        label = { Text("Serif", fontFamily = FontFamily.Serif) }
                    )
                    FilterChip(
                        selected = fontFamily == "MONOSPACE",
                        onClick = { fontFamily = "MONOSPACE" },
                        label = { Text("Mono", fontFamily = FontFamily.Monospace) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Whiteout Backing Toggle (for seamless replacement of original PDF text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Whiteout Background Patch",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Covers original text underneath cleanly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasWhiteoutBacking,
                        onCheckedChange = { hasWhiteoutBacking = it },
                        modifier = Modifier.testTag("whiteout_patch_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Text Color Palette
                Text(
                    text = "Text Color",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                ColorPickerRow(
                    selectedColor = textColor,
                    onColorSelected = { textColor = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val updated = element.copy(
                                text = text,
                                fontSizePt = fontSize,
                                isBold = isBold,
                                isItalic = isItalic,
                                alignment = alignment,
                                fontFamilyType = fontFamily,
                                colorArgb = textColor.toArgb().toLong(),
                                backgroundColorArgb = if (hasWhiteoutBacking) 0xFFFFFFFFL else null
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.testTag("save_text_btn")
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
