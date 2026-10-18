package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.InteractiveElementBox
import com.example.ui.components.SignaturePadDialog
import com.example.ui.dialogs.*
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.PdfEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: PdfEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    var brushColor by remember { mutableStateOf(Color(0xFFE11D48)) }
    var highlighterColor by remember { mutableStateOf(Color(0xFFFFD54F)) }

    // Canvas dimensions for normalized coordinates
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    var canvasHeightPx by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            renameText = state.documentTitle
                            showRenameDialog = true
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.documentTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Page ${state.currentPageIndex + 1} of ${state.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_to_home_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = state.canUndo,
                        modifier = Modifier.testTag("undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = state.canRedo,
                        modifier = Modifier.testTag("redo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) MaterialTheme.colorScheme.onSurface else Color.LightGray
                        )
                    }

                    if (!state.isAdsRemoved) {
                        IconButton(
                            onClick = { viewModel.openRemoveAdsDialog() },
                            modifier = Modifier.testTag("editor_remove_ads_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Remove Ads",
                                tint = Color(0xFFF59E0B)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openPageManager() },
                        modifier = Modifier.testTag("page_manager_btn")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Pages")
                    }

                    Button(
                        onClick = {
                            if (activity != null) {
                                viewModel.handleExportClick(activity)
                            } else {
                                viewModel.exportDocument()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_pdf_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                // Secondary contextual action strip based on active tool
                when (state.currentTool) {
                    EditorTool.TEXT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.startAddText(0.2f, 0.3f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(42.dp)
                                    .testTag("add_text_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Text Box", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    EditorTool.IMAGE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.openImagePicker() },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(42.dp)
                                    .testTag("add_image_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Insert Photo", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    EditorTool.WHITEOUT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.addWhiteout(0.25f, 0.35f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(42.dp)
                                    .testTag("add_whiteout_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Whiteout Patch", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    EditorTool.DRAW -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorPickerRow(
                                selectedColor = brushColor,
                                onColorSelected = { brushColor = it },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextButton(onClick = { viewModel.clearDrawingsOnCurrentPage() }) {
                                Text("Clear")
                            }
                        }
                    }
                    EditorTool.HIGHLIGHT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorPickerRow(
                                selectedColor = highlighterColor,
                                onColorSelected = { highlighterColor = it },
                                colors = listOf(
                                    Color(0xFFFFD54F), // Yellow
                                    Color(0xFF81C784), // Green
                                    Color(0xFF64B5F6), // Blue
                                    Color(0xFFFF8A80), // Pink
                                    Color(0xFFBA68C8)  // Purple
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextButton(onClick = { viewModel.clearDrawingsOnCurrentPage() }) {
                                Text("Clear")
                            }
                        }
                    }
                    EditorTool.SIGNATURE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.openSignaturePad() },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(42.dp)
                                    .testTag("open_signature_dialog_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Document", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    else -> {}
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Main Tool Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.height(68.dp)
                ) {
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.BROWSE,
                        onClick = { viewModel.setTool(EditorTool.BROWSE) },
                        icon = { Icon(Icons.Default.PanTool, contentDescription = "Browse") },
                        label = { Text("View", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.TEXT,
                        onClick = { viewModel.setTool(EditorTool.TEXT) },
                        icon = { Icon(Icons.Default.Title, contentDescription = "Text") },
                        label = { Text("Text", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.IMAGE,
                        onClick = { viewModel.setTool(EditorTool.IMAGE) },
                        icon = { Icon(Icons.Default.Image, contentDescription = "Image") },
                        label = { Text("Photo", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.WHITEOUT,
                        onClick = { viewModel.setTool(EditorTool.WHITEOUT) },
                        icon = { Icon(Icons.Default.AutoFixNormal, contentDescription = "Whiteout") },
                        label = { Text("Erase", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.DRAW,
                        onClick = { viewModel.setTool(EditorTool.DRAW) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Draw") },
                        label = { Text("Pen", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.HIGHLIGHT,
                        onClick = { viewModel.setTool(EditorTool.HIGHLIGHT) },
                        icon = { Icon(Icons.Default.BorderColor, contentDescription = "Highlight") },
                        label = { Text("Marker", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = state.currentTool == EditorTool.SIGNATURE,
                        onClick = { viewModel.setTool(EditorTool.SIGNATURE) },
                        icon = { Icon(Icons.Default.Gesture, contentDescription = "Sign") },
                        label = { Text("Sign", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Page Switcher Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectPage(state.currentPageIndex - 1) },
                        enabled = state.currentPageIndex > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                    }

                    Text(
                        text = "Page ${state.currentPageIndex + 1} of ${state.pageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = { viewModel.selectPage(state.currentPageIndex + 1) },
                        enabled = state.currentPageIndex < state.pageCount - 1
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            val currentPageBitmap = state.pageBitmaps[state.currentPageIndex]

            if (state.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.statusMessage ?: "Processing PDF...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (currentPageBitmap != null) {
                // PDF Document Viewport
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Aspect-fit sheet canvas
                    val aspectRatio = currentPageBitmap.width.toFloat() / currentPageBitmap.height.toFloat()

                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.96f)
                            .aspectRatio(aspectRatio)
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                            .onGloballyPositioned { coordinates ->
                                canvasWidthPx = coordinates.size.width.toFloat()
                                canvasHeightPx = coordinates.size.height.toFloat()
                            }
                            .pointerInput(state.currentTool, canvasWidthPx, canvasHeightPx) {
                                detectTapGestures { offset ->
                                    if (canvasWidthPx > 0 && canvasHeightPx > 0) {
                                        val normX = offset.x / canvasWidthPx
                                        val normY = offset.y / canvasHeightPx
                                        when (state.currentTool) {
                                            EditorTool.TEXT -> {
                                                viewModel.startAddText(normX - 0.1f, normY - 0.03f)
                                            }
                                            EditorTool.WHITEOUT -> {
                                                viewModel.addWhiteout(normX - 0.15f, normY - 0.02f)
                                            }
                                            EditorTool.BROWSE -> {
                                                viewModel.selectElement(null)
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                    ) {
                        // 1. Rendered PDF page bitmap
                        Image(
                            bitmap = currentPageBitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 2. Whiteouts on current page
                        val currentWhiteouts = state.whiteouts.filter { it.pageIndex == state.currentPageIndex }
                        for (whiteout in currentWhiteouts) {
                            val isSelected = (state.selectedElement as? SelectedElement.Whiteout)?.element?.id == whiteout.id
                            InteractiveElementBox(
                                isSelected = isSelected,
                                normalizedX = whiteout.normalizedX,
                                normalizedY = whiteout.normalizedY,
                                normalizedWidth = whiteout.normalizedWidth,
                                normalizedHeight = whiteout.normalizedHeight,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                                onMove = { nx, ny -> viewModel.updateWhiteoutPosition(whiteout.id, nx, ny) },
                                onResize = { nw, nh -> viewModel.updateWhiteoutSize(whiteout.id, nw, nh) },
                                onSelect = { viewModel.selectElement(SelectedElement.Whiteout(whiteout)) },
                                onDelete = { viewModel.deleteWhiteout(whiteout.id) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(whiteout.colorArgb))
                                )
                            }
                        }

                        // 3. Images on current page
                        val currentImages = state.images.filter { it.pageIndex == state.currentPageIndex }
                        for (img in currentImages) {
                            val isSelected = (state.selectedElement as? SelectedElement.Image)?.element?.id == img.id
                            InteractiveElementBox(
                                isSelected = isSelected,
                                normalizedX = img.normalizedX,
                                normalizedY = img.normalizedY,
                                normalizedWidth = img.normalizedWidth,
                                normalizedHeight = img.normalizedHeight,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                                rotation = img.rotation,
                                onMove = { nx, ny -> viewModel.updateImagePosition(img.id, nx, ny) },
                                onResize = { nw, nh -> viewModel.updateImageSize(img.id, nw, nh) },
                                onSelect = { viewModel.selectElement(SelectedElement.Image(img)) },
                                onEdit = { viewModel.openImagePicker() },
                                onDelete = { viewModel.deleteImage(img.id) }
                            ) {
                                if (img.bitmap != null && !img.bitmap.isRecycled) {
                                    val filterColorMatrix = when (img.filter) {
                                        ImageFilterType.NONE -> null
                                        ImageFilterType.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
                                        ImageFilterType.SEPIA -> {
                                            val m = ColorMatrix()
                                            m.setToSaturation(0f)
                                            val sepia = ColorMatrix(
                                                floatArrayOf(
                                                    1f, 0f, 0f, 0f, 0f,
                                                    0f, 0.95f, 0f, 0f, 0f,
                                                    0f, 0f, 0.82f, 0f, 0f,
                                                    0f, 0f, 0f, 1f, 0f
                                                )
                                            )
                                            m.timesAssign(sepia)
                                            m
                                        }
                                        ImageFilterType.HIGH_CONTRAST -> {
                                            val contrast = 1.5f
                                            val t = (-0.5f * contrast + 0.5f) * 255f
                                            ColorMatrix(
                                                floatArrayOf(
                                                    contrast, 0f, 0f, 0f, t,
                                                    0f, contrast, 0f, 0f, t,
                                                    0f, 0f, contrast, 0f, t,
                                                    0f, 0f, 0f, 1f, 0f
                                                )
                                            )
                                        }
                                    }

                                    Image(
                                        bitmap = img.bitmap.asImageBitmap(),
                                        contentDescription = "Inserted Image",
                                        contentScale = ContentScale.FillBounds,
                                        alpha = img.opacity,
                                        colorFilter = filterColorMatrix?.let { ColorFilter.colorMatrix(it) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // 4. Signatures on current page
                        val currentSignatures = state.signatures.filter { it.pageIndex == state.currentPageIndex }
                        for (sig in currentSignatures) {
                            val isSelected = (state.selectedElement as? SelectedElement.Signature)?.element?.id == sig.id
                            InteractiveElementBox(
                                isSelected = isSelected,
                                normalizedX = sig.normalizedX,
                                normalizedY = sig.normalizedY,
                                normalizedWidth = sig.normalizedWidth,
                                normalizedHeight = sig.normalizedHeight,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                                onMove = { nx, ny -> viewModel.updateSignaturePosition(sig.id, nx, ny) },
                                onResize = { _, _ -> },
                                onSelect = { viewModel.selectElement(SelectedElement.Signature(sig)) },
                                onDelete = { viewModel.deleteSignature(sig.id) }
                            ) {
                                if (!sig.bitmap.isRecycled) {
                                    Image(
                                        bitmap = sig.bitmap.asImageBitmap(),
                                        contentDescription = "Signature",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // 5. Text elements on current page
                        val currentTexts = state.texts.filter { it.pageIndex == state.currentPageIndex }
                        for (txt in currentTexts) {
                            val isSelected = (state.selectedElement as? SelectedElement.Text)?.element?.id == txt.id
                            InteractiveElementBox(
                                isSelected = isSelected,
                                normalizedX = txt.normalizedX,
                                normalizedY = txt.normalizedY,
                                normalizedWidth = txt.normalizedWidth,
                                normalizedHeight = txt.normalizedHeight,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                                rotation = txt.rotation,
                                onMove = { nx, ny -> viewModel.updateTextPosition(txt.id, nx, ny) },
                                onResize = { nw, nh -> viewModel.updateTextSize(txt.id, nw, nh) },
                                onSelect = { viewModel.selectElement(SelectedElement.Text(txt)) },
                                onEdit = { viewModel.startEditText(txt) },
                                onDelete = { viewModel.deleteText(txt.id) }
                            ) {
                                val fontFam = when (txt.fontFamilyType) {
                                    "SERIF" -> FontFamily.Serif
                                    "MONOSPACE" -> FontFamily.Monospace
                                    else -> FontFamily.SansSerif
                                }
                                val textAl = when (txt.alignment) {
                                    TextAlignmentType.CENTER -> TextAlign.Center
                                    TextAlignmentType.RIGHT -> TextAlign.End
                                    else -> TextAlign.Start
                                }

                                val scaleFactor = if (canvasWidthPx > 0) canvasWidthPx / 595f else 1f
                                val displayFontSize = (txt.fontSizePt * scaleFactor).sp

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (txt.backgroundColorArgb != null) {
                                                Modifier.background(Color(txt.backgroundColorArgb))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = txt.text,
                                        fontSize = displayFontSize,
                                        fontWeight = if (txt.isBold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (txt.isItalic) FontStyle.Italic else FontStyle.Normal,
                                        fontFamily = fontFam,
                                        color = Color(txt.colorArgb),
                                        textAlign = textAl,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // 6. Freehand Drawing & Highlighter Canvas Layer
                        DrawingCanvas(
                            tool = state.currentTool,
                            pageIndex = state.currentPageIndex,
                            strokes = state.drawStrokes,
                            onAddStroke = { stroke -> viewModel.addDrawStroke(stroke) },
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            brushColor = brushColor,
                            highlighterColor = highlighterColor
                        )
                    }
                }
            } else {
                Text(
                    text = "No Document Loaded",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Dialogs & Sheets
    if (state.isTextEditDialogOpen && state.editingTextElement != null) {
        TextEditDialog(
            element = state.editingTextElement!!,
            onDismiss = { viewModel.dismissTextEditDialog() },
            onSave = { updated -> viewModel.saveTextElement(updated) }
        )
    }

    if (state.isImagePickerSheetOpen) {
        val selectedImage = (state.selectedElement as? SelectedElement.Image)?.element
        ImagePickerSheet(
            selectedImageElement = selectedImage,
            onDismiss = { viewModel.dismissImagePicker() },
            onImageSelected = { bitmap ->
                if (selectedImage != null) {
                    viewModel.replaceSelectedImage(bitmap)
                } else {
                    viewModel.addImageBitmap(bitmap)
                }
            },
            onFilterChanged = { filter ->
                if (selectedImage != null) {
                    viewModel.updateImageFilter(selectedImage.id, filter)
                }
            },
            onOpacityChanged = { op ->
                if (selectedImage != null) {
                    viewModel.updateImageOpacity(selectedImage.id, op)
                }
            },
            onDeleteImage = {
                if (selectedImage != null) {
                    viewModel.deleteImage(selectedImage.id)
                    viewModel.dismissImagePicker()
                }
            }
        )
    }

    if (state.isSignaturePadOpen) {
        SignaturePadDialog(
            onDismiss = { viewModel.dismissSignaturePad() },
            onSaveSignature = { bmp -> viewModel.addSignature(bmp) }
        )
    }

    if (state.isPageManagerOpen) {
        PageManagerSheet(
            pageCount = state.pageCount,
            currentPageIndex = state.currentPageIndex,
            pageBitmaps = state.pageBitmaps,
            onDismiss = { viewModel.dismissPageManager() },
            onSelectPage = { idx -> viewModel.selectPage(idx) },
            onAddBlankPage = { viewModel.addBlankPage() },
            onDuplicatePage = { viewModel.duplicateCurrentPage() },
            onDeletePage = { viewModel.deleteCurrentPage() }
        )
    }

    if (state.isExportSuccessDialogOpen) {
        ExportSuccessDialog(
            file = state.exportedFile,
            pageCount = state.pageCount,
            onDismiss = { viewModel.dismissExportSuccessDialog() },
            onShare = { viewModel.shareExportedPdf() },
            onPrint = { viewModel.printExportedPdf() }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.updateTitle(renameText.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
