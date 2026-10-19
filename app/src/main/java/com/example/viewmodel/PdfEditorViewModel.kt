package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.monetization.AdMobManager
import com.example.monetization.BillingManager
import com.example.pdf.PdfExporter
import com.example.pdf.PdfRendererHelper
import com.example.pdf.SamplePdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class EditorStateSnapshot(
    val texts: List<TextElement>,
    val images: List<ImageElement>,
    val whiteouts: List<WhiteoutElement>,
    val drawStrokes: List<DrawStroke>,
    val signatures: List<SignatureElement>
)

data class EditorUiState(
    val isLoading: Boolean = false,
    val documentTitle: String = "Untitled Document",
    val documentFile: File? = null,
    val pageCount: Int = 1,
    val currentPageIndex: Int = 0,
    val pageBitmaps: Map<Int, Bitmap> = emptyMap(),
    val currentTool: EditorTool = EditorTool.BROWSE,
    val selectedElement: SelectedElement? = null,
    val texts: List<TextElement> = emptyList(),
    val images: List<ImageElement> = emptyList(),
    val whiteouts: List<WhiteoutElement> = emptyList(),
    val drawStrokes: List<DrawStroke> = emptyList(),
    val signatures: List<SignatureElement> = emptyList(),
    val isTextEditDialogOpen: Boolean = false,
    val editingTextElement: TextElement? = null,
    val isImagePickerSheetOpen: Boolean = false,
    val isSignaturePadOpen: Boolean = false,
    val isPageManagerOpen: Boolean = false,
    val isExportSuccessDialogOpen: Boolean = false,
    val exportedFile: File? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isAdsRemoved: Boolean = false,
    val isRewardedAdReady: Boolean = false,
    val isRemoveAdsDialogOpen: Boolean = false,
    val isRewardedAdPromptOpen: Boolean = false,
    val statusMessage: String? = null
)

class PdfEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val rendererHelper = PdfRendererHelper(context)
    private val exporter = PdfExporter(context)
    private val sampleGenerator = SamplePdfGenerator(context)
    val adMobManager = AdMobManager(context)
    val billingManager = BillingManager(context)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<EditorStateSnapshot>()
    private val redoStack = mutableListOf<EditorStateSnapshot>()

    init {
        // Preload sample proposal by default so the app is immediately ready with rich content
        loadSampleProposal()

        // Observe monetization and ad states
        viewModelScope.launch {
            billingManager.isAdsRemoved.collect { removed ->
                _uiState.update { it.copy(isAdsRemoved = removed) }
            }
        }
        viewModelScope.launch {
            adMobManager.isAdLoaded.collect { loaded ->
                _uiState.update { it.copy(isRewardedAdReady = loaded) }
            }
        }
    }

    private fun takeSnapshot(): EditorStateSnapshot {
        val s = _uiState.value
        return EditorStateSnapshot(
            texts = s.texts.toList(),
            images = s.images.toList(),
            whiteouts = s.whiteouts.toList(),
            drawStrokes = s.drawStrokes.toList(),
            signatures = s.signatures.toList()
        )
    }

    private fun pushUndo() {
        undoStack.add(takeSnapshot())
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoStatus()
    }

    private fun updateUndoRedoStatus() {
        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentSnapshot = takeSnapshot()
        redoStack.add(currentSnapshot)
        val prev = undoStack.removeAt(undoStack.lastIndex)

        _uiState.update {
            it.copy(
                texts = prev.texts,
                images = prev.images,
                whiteouts = prev.whiteouts,
                drawStrokes = prev.drawStrokes,
                signatures = prev.signatures,
                selectedElement = null
            )
        }
        updateUndoRedoStatus()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentSnapshot = takeSnapshot()
        undoStack.add(currentSnapshot)
        val next = redoStack.removeAt(redoStack.lastIndex)

        _uiState.update {
            it.copy(
                texts = next.texts,
                images = next.images,
                whiteouts = next.whiteouts,
                drawStrokes = next.drawStrokes,
                signatures = next.signatures,
                selectedElement = null
            )
        }
        updateUndoRedoStatus()
    }

    fun loadSampleProposal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Loading Proposal...") }
            val file = sampleGenerator.getOrCreateSampleProposal()
            openFileInternal(file, "Business Proposal & Strategy")
        }
    }

    fun loadSampleInvoice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Loading Invoice...") }
            val file = sampleGenerator.getOrCreateSampleInvoice()
            openFileInternal(file, "Craft Studio Invoice #INV-2026")
        }
    }

    fun loadSampleAgreement() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Loading Agreement...") }
            val file = sampleGenerator.getOrCreateSampleAgreement()
            openFileInternal(file, "Non-Disclosure & Service Agreement")
        }
    }

    fun loadSampleResume() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Loading Resume...") }
            val file = sampleGenerator.getOrCreateSampleResume()
            openFileInternal(file, "Executive Resume & Portfolio")
        }
    }

    fun createBlankDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Creating blank document...") }
            val file = sampleGenerator.createBlankDocument()
            openFileInternal(file, "Untitled Document")
        }
    }

    fun openPdfFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Opening PDF...") }
            val file = rendererHelper.loadPdfUri(uri)
            if (file != null) {
                val title = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".pdf") ?: "Imported PDF"
                openFileInternal(file, title)
            } else {
                _uiState.update { it.copy(isLoading = false, statusMessage = "Failed to open PDF") }
            }
        }
    }

    private suspend fun openFileInternal(file: File, title: String) {
        val success = rendererHelper.loadPdfFile(file)
        if (!success) {
            _uiState.update { it.copy(isLoading = false, statusMessage = "Error parsing PDF") }
            return
        }

        val totalPages = rendererHelper.getPageCount().coerceAtLeast(1)
        val pageBitmaps = mutableMapOf<Int, Bitmap>()

        // Render first page immediately
        val firstPage = rendererHelper.renderPage(0, targetWidth = 1400)
        if (firstPage != null) {
            pageBitmaps[0] = firstPage
        }

        // Render remaining pages in background
        for (i in 1 until totalPages) {
            val bmp = rendererHelper.renderPage(i, targetWidth = 1400)
            if (bmp != null) {
                pageBitmaps[i] = bmp
            }
        }

        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                isLoading = false,
                documentTitle = title,
                documentFile = file,
                pageCount = totalPages,
                currentPageIndex = 0,
                pageBitmaps = pageBitmaps,
                currentTool = EditorTool.BROWSE,
                selectedElement = null,
                texts = emptyList(),
                images = emptyList(),
                whiteouts = emptyList(),
                drawStrokes = emptyList(),
                signatures = emptyList(),
                canUndo = false,
                canRedo = false,
                statusMessage = null
            )
        }
    }

    fun setTool(tool: EditorTool) {
        _uiState.update {
            it.copy(
                currentTool = tool,
                selectedElement = if (tool == EditorTool.BROWSE) null else it.selectedElement
            )
        }
    }

    fun selectPage(index: Int) {
        if (index in 0 until _uiState.value.pageCount) {
            _uiState.update {
                it.copy(
                    currentPageIndex = index,
                    selectedElement = null
                )
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(documentTitle = newTitle) }
    }

    fun selectElement(element: SelectedElement?) {
        _uiState.update { it.copy(selectedElement = element) }
    }

    // --- TEXT OPERATIONS ---
    fun startAddText(x: Float = 0.3f, y: Float = 0.4f) {
        val newText = TextElement(
            pageIndex = _uiState.value.currentPageIndex,
            text = "Double tap to edit text",
            normalizedX = x.coerceIn(0.05f, 0.7f),
            normalizedY = y.coerceIn(0.05f, 0.85f),
            normalizedWidth = 0.45f,
            normalizedHeight = 0.08f,
            fontSizePt = 16f,
            colorArgb = 0xFF0F172AL
        )
        _uiState.update {
            it.copy(
                editingTextElement = newText,
                isTextEditDialogOpen = true
            )
        }
    }

    fun startEditText(element: TextElement) {
        _uiState.update {
            it.copy(
                editingTextElement = element,
                isTextEditDialogOpen = true
            )
        }
    }

    fun dismissTextEditDialog() {
        _uiState.update {
            it.copy(
                isTextEditDialogOpen = false,
                editingTextElement = null
            )
        }
    }

    fun saveTextElement(updated: TextElement) {
        pushUndo()
        _uiState.update { state ->
            val exists = state.texts.any { it.id == updated.id }
            val newTexts = if (exists) {
                state.texts.map { if (it.id == updated.id) updated else it }
            } else {
                state.texts + updated
            }
            state.copy(
                texts = newTexts,
                isTextEditDialogOpen = false,
                editingTextElement = null,
                selectedElement = SelectedElement.Text(updated)
            )
        }
    }

    fun updateTextPosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val updatedList = state.texts.map {
                if (it.id == id) it.copy(normalizedX = newX.coerceIn(0f, 0.95f), normalizedY = newY.coerceIn(0f, 0.95f))
                else it
            }
            state.copy(
                texts = updatedList,
                selectedElement = (state.selectedElement as? SelectedElement.Text)?.let {
                    SelectedElement.Text(updatedList.first { item -> item.id == id })
                } ?: state.selectedElement
            )
        }
    }

    fun updateTextSize(id: String, newWidth: Float, newHeight: Float) {
        _uiState.update { state ->
            val updatedList = state.texts.map {
                if (it.id == id) it.copy(
                    normalizedWidth = newWidth.coerceIn(0.1f, 0.9f),
                    normalizedHeight = newHeight.coerceIn(0.03f, 0.9f)
                ) else it
            }
            state.copy(texts = updatedList)
        }
    }

    fun deleteText(id: String) {
        pushUndo()
        _uiState.update { state ->
            state.copy(
                texts = state.texts.filterNot { it.id == id },
                selectedElement = null
            )
        }
    }

    // --- WHITEOUT / ERASER OPERATIONS ---
    fun addWhiteout(x: Float = 0.2f, y: Float = 0.3f, width: Float = 0.35f, height: Float = 0.05f) {
        pushUndo()
        val whiteout = WhiteoutElement(
            pageIndex = _uiState.value.currentPageIndex,
            normalizedX = x.coerceIn(0.02f, 0.8f),
            normalizedY = y.coerceIn(0.02f, 0.9f),
            normalizedWidth = width,
            normalizedHeight = height,
            colorArgb = 0xFFFFFFFFL
        )
        _uiState.update { state ->
            state.copy(
                whiteouts = state.whiteouts + whiteout,
                selectedElement = SelectedElement.Whiteout(whiteout)
            )
        }
    }

    fun updateWhiteoutPosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val list = state.whiteouts.map {
                if (it.id == id) it.copy(normalizedX = newX.coerceIn(0f, 0.95f), normalizedY = newY.coerceIn(0f, 0.95f))
                else it
            }
            state.copy(
                whiteouts = list,
                selectedElement = (state.selectedElement as? SelectedElement.Whiteout)?.let {
                    SelectedElement.Whiteout(list.first { item -> item.id == id })
                } ?: state.selectedElement
            )
        }
    }

    fun updateWhiteoutSize(id: String, newWidth: Float, newHeight: Float) {
        _uiState.update { state ->
            val list = state.whiteouts.map {
                if (it.id == id) it.copy(
                    normalizedWidth = newWidth.coerceIn(0.05f, 0.95f),
                    normalizedHeight = newHeight.coerceIn(0.02f, 0.95f)
                ) else it
            }
            state.copy(whiteouts = list)
        }
    }

    fun deleteWhiteout(id: String) {
        pushUndo()
        _uiState.update { state ->
            state.copy(
                whiteouts = state.whiteouts.filterNot { it.id == id },
                selectedElement = null
            )
        }
    }

    // --- IMAGE OPERATIONS ---
    fun openImagePicker() {
        _uiState.update { it.copy(isImagePickerSheetOpen = true) }
    }

    fun dismissImagePicker() {
        _uiState.update { it.copy(isImagePickerSheetOpen = false) }
    }

    fun addImageBitmap(bitmap: Bitmap, filter: ImageFilterType = ImageFilterType.NONE) {
        pushUndo()
        val img = ImageElement(
            pageIndex = _uiState.value.currentPageIndex,
            bitmap = bitmap,
            normalizedX = 0.25f,
            normalizedY = 0.35f,
            normalizedWidth = 0.45f,
            normalizedHeight = 0.30f,
            filter = filter
        )
        _uiState.update { state ->
            state.copy(
                images = state.images + img,
                isImagePickerSheetOpen = false,
                selectedElement = SelectedElement.Image(img)
            )
        }
    }

    fun replaceSelectedImage(bitmap: Bitmap) {
        val selected = _uiState.value.selectedElement as? SelectedElement.Image ?: return
        pushUndo()
        val updated = selected.element.copy(bitmap = bitmap)
        _uiState.update { state ->
            state.copy(
                images = state.images.map { if (it.id == updated.id) updated else it },
                isImagePickerSheetOpen = false,
                selectedElement = SelectedElement.Image(updated)
            )
        }
    }

    fun updateImagePosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val list = state.images.map {
                if (it.id == id) it.copy(normalizedX = newX.coerceIn(0f, 0.95f), normalizedY = newY.coerceIn(0f, 0.95f))
                else it
            }
            state.copy(
                images = list,
                selectedElement = (state.selectedElement as? SelectedElement.Image)?.let {
                    SelectedElement.Image(list.first { item -> item.id == id })
                } ?: state.selectedElement
            )
        }
    }

    fun updateImageSize(id: String, newWidth: Float, newHeight: Float) {
        _uiState.update { state ->
            val list = state.images.map {
                if (it.id == id) it.copy(
                    normalizedWidth = newWidth.coerceIn(0.1f, 0.95f),
                    normalizedHeight = newHeight.coerceIn(0.08f, 0.95f)
                ) else it
            }
            state.copy(images = list)
        }
    }

    fun updateImageFilter(id: String, filter: ImageFilterType) {
        pushUndo()
        _uiState.update { state ->
            val list = state.images.map {
                if (it.id == id) it.copy(filter = filter) else it
            }
            state.copy(
                images = list,
                selectedElement = (state.selectedElement as? SelectedElement.Image)?.let {
                    SelectedElement.Image(list.first { item -> item.id == id })
                } ?: state.selectedElement
            )
        }
    }

    fun updateImageOpacity(id: String, opacity: Float) {
        _uiState.update { state ->
            val list = state.images.map {
                if (it.id == id) it.copy(opacity = opacity.coerceIn(0.1f, 1.0f)) else it
            }
            state.copy(images = list)
        }
    }

    fun deleteImage(id: String) {
        pushUndo()
        _uiState.update { state ->
            state.copy(
                images = state.images.filterNot { it.id == id },
                selectedElement = null
            )
        }
    }

    // --- DRAW & HIGHLIGHT OPERATIONS ---
    fun addDrawStroke(stroke: DrawStroke) {
        pushUndo()
        _uiState.update { state ->
            state.copy(drawStrokes = state.drawStrokes + stroke)
        }
    }

    fun clearDrawingsOnCurrentPage() {
        pushUndo()
        val currentIdx = _uiState.value.currentPageIndex
        _uiState.update { state ->
            state.copy(drawStrokes = state.drawStrokes.filterNot { it.pageIndex == currentIdx })
        }
    }

    // --- SIGNATURE OPERATIONS ---
    fun openSignaturePad() {
        _uiState.update { it.copy(isSignaturePadOpen = true) }
    }

    fun dismissSignaturePad() {
        _uiState.update { it.copy(isSignaturePadOpen = false) }
    }

    fun addSignature(bitmap: Bitmap) {
        pushUndo()
        val sig = SignatureElement(
            pageIndex = _uiState.value.currentPageIndex,
            bitmap = bitmap,
            normalizedX = 0.55f,
            normalizedY = 0.78f,
            normalizedWidth = 0.35f,
            normalizedHeight = 0.12f
        )
        _uiState.update { state ->
            state.copy(
                signatures = state.signatures + sig,
                isSignaturePadOpen = false,
                selectedElement = SelectedElement.Signature(sig)
            )
        }
    }

    fun updateSignaturePosition(id: String, newX: Float, newY: Float) {
        _uiState.update { state ->
            val list = state.signatures.map {
                if (it.id == id) it.copy(normalizedX = newX.coerceIn(0f, 0.95f), normalizedY = newY.coerceIn(0f, 0.95f))
                else it
            }
            state.copy(
                signatures = list,
                selectedElement = (state.selectedElement as? SelectedElement.Signature)?.let {
                    SelectedElement.Signature(list.first { item -> item.id == id })
                } ?: state.selectedElement
            )
        }
    }

    fun deleteSignature(id: String) {
        pushUndo()
        _uiState.update { state ->
            state.copy(
                signatures = state.signatures.filterNot { it.id == id },
                selectedElement = null
            )
        }
    }

    // --- PAGE MANAGEMENT ---
    fun openPageManager() {
        _uiState.update { it.copy(isPageManagerOpen = true) }
    }

    fun dismissPageManager() {
        _uiState.update { it.copy(isPageManagerOpen = false) }
    }

    fun addBlankPage() {
        val newPageIndex = _uiState.value.pageCount
        val blankBmp = Bitmap.createBitmap(1200, 1696, Bitmap.Config.ARGB_8888)
        blankBmp.eraseColor(android.graphics.Color.WHITE)

        val updatedMap = _uiState.value.pageBitmaps.toMutableMap()
        updatedMap[newPageIndex] = blankBmp

        _uiState.update {
            it.copy(
                pageCount = newPageIndex + 1,
                pageBitmaps = updatedMap,
                currentPageIndex = newPageIndex
            )
        }
    }

    fun duplicateCurrentPage() {
        val curr = _uiState.value.currentPageIndex
        val srcBmp = _uiState.value.pageBitmaps[curr] ?: return
        val dupBmp = srcBmp.copy(srcBmp.config ?: Bitmap.Config.ARGB_8888, true)

        val newPageIndex = _uiState.value.pageCount
        val updatedMap = _uiState.value.pageBitmaps.toMutableMap()
        updatedMap[newPageIndex] = dupBmp

        // Duplicate elements on this page
        val dupTexts = _uiState.value.texts.filter { it.pageIndex == curr }.map { it.copy(id = java.util.UUID.randomUUID().toString(), pageIndex = newPageIndex) }
        val dupImages = _uiState.value.images.filter { it.pageIndex == curr }.map { it.copy(id = java.util.UUID.randomUUID().toString(), pageIndex = newPageIndex) }

        _uiState.update {
            it.copy(
                pageCount = newPageIndex + 1,
                pageBitmaps = updatedMap,
                texts = it.texts + dupTexts,
                images = it.images + dupImages,
                currentPageIndex = newPageIndex
            )
        }
    }

    fun deleteCurrentPage() {
        if (_uiState.value.pageCount <= 1) return
        val currentIdx = _uiState.value.currentPageIndex

        val newCount = _uiState.value.pageCount - 1
        val updatedMap = mutableMapOf<Int, Bitmap>()

        var newKey = 0
        for (i in 0 until _uiState.value.pageCount) {
            if (i == currentIdx) continue
            _uiState.value.pageBitmaps[i]?.let {
                updatedMap[newKey] = it
                newKey++
            }
        }

        val updatedTexts = _uiState.value.texts.filterNot { it.pageIndex == currentIdx }.map {
            if (it.pageIndex > currentIdx) it.copy(pageIndex = it.pageIndex - 1) else it
        }
        val updatedImages = _uiState.value.images.filterNot { it.pageIndex == currentIdx }.map {
            if (it.pageIndex > currentIdx) it.copy(pageIndex = it.pageIndex - 1) else it
        }
        val updatedWhiteouts = _uiState.value.whiteouts.filterNot { it.pageIndex == currentIdx }.map {
            if (it.pageIndex > currentIdx) it.copy(pageIndex = it.pageIndex - 1) else it
        }
        val updatedStrokes = _uiState.value.drawStrokes.filterNot { it.pageIndex == currentIdx }.map {
            if (it.pageIndex > currentIdx) it.copy(pageIndex = it.pageIndex - 1) else it
        }

        _uiState.update {
            it.copy(
                pageCount = newCount,
                pageBitmaps = updatedMap,
                currentPageIndex = currentIdx.coerceAtMost(newCount - 1),
                texts = updatedTexts,
                images = updatedImages,
                whiteouts = updatedWhiteouts,
                drawStrokes = updatedStrokes,
                selectedElement = null
            )
        }
    }

    // --- EXPORT & SHARE ---
    fun exportDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Exporting high-fidelity PDF...") }

            val dimensions = (0 until _uiState.value.pageCount).map { i ->
                rendererHelper.getPageDimension(i)
            }

            val sanitizedTitle = _uiState.value.documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val exportDir = File(context.cacheDir, "exported_pdfs").apply { mkdirs() }
            val outputFile = File(exportDir, "${sanitizedTitle}_edited.pdf")

            val success = exporter.exportPdf(
                pageCount = _uiState.value.pageCount,
                pageDimensions = dimensions,
                baseBitmaps = _uiState.value.pageBitmaps,
                texts = _uiState.value.texts,
                images = _uiState.value.images,
                whiteouts = _uiState.value.whiteouts,
                drawStrokes = _uiState.value.drawStrokes,
                signatures = _uiState.value.signatures,
                outputFile = outputFile
            )

            if (success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        exportedFile = outputFile,
                        isExportSuccessDialogOpen = true,
                        statusMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Export failed. Please check document contents."
                    )
                }
            }
        }
    }

    fun dismissExportSuccessDialog() {
        _uiState.update { it.copy(isExportSuccessDialogOpen = false) }
    }

    fun shareExportedPdf() {
        val file = _uiState.value.exportedFile ?: return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, _uiState.value.documentTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Edited PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("PdfEditorViewModel", "Error sharing PDF", e)
        }
    }

    fun printExportedPdf() {
        val file = _uiState.value.exportedFile ?: return
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(file.name)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(_uiState.value.pageCount)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        file.inputStream().use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            printManager.print(_uiState.value.documentTitle, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Log.e("PdfEditorViewModel", "Error printing PDF", e)
        }
    }

    // --- MONETIZATION & AD CONTROLS ---
    fun openRemoveAdsDialog() {
        _uiState.update { it.copy(isRemoveAdsDialogOpen = true) }
    }

    fun dismissRemoveAdsDialog() {
        _uiState.update { it.copy(isRemoveAdsDialogOpen = false) }
    }

    fun openRewardedAdPrompt() {
        if (!_uiState.value.isAdsRemoved) {
            adMobManager.initializeIfNeeded()
        }
        _uiState.update { it.copy(isRewardedAdPromptOpen = true) }
    }

    fun dismissRewardedAdPrompt() {
        _uiState.update { it.copy(isRewardedAdPromptOpen = false) }
    }

    fun handleExportClick(activity: Activity) {
        if (_uiState.value.isAdsRemoved) {
            exportDocument()
        } else {
            openRewardedAdPrompt()
        }
    }

    fun watchRewardedAdAndExport(activity: Activity) {
        dismissRewardedAdPrompt()
        if (_uiState.value.isRewardedAdReady) {
            adMobManager.showRewardedAd(
                activity = activity,
                onUserEarnedReward = { exportDocument() },
                onAdDismissed = {},
                onAdFailedToShow = { exportDocument() }
            )
        } else {
            // Ad is preloading, grant export reward immediately!
            exportDocument()
        }
    }

    fun purchaseRemoveAds(activity: Activity) {
        billingManager.launchPurchaseFlow(activity) {
            // Simulated / local purchase fallback for test and emulator environments
            billingManager.setAdsRemoved(true)
            _uiState.update { it.copy(isAdsRemoved = true, isRemoveAdsDialogOpen = false) }
        }
    }

    fun testWatchRewardedAd(activity: Activity) {
        if (_uiState.value.isAdsRemoved) {
            _uiState.update { it.copy(statusMessage = "PRO Active: Ads are removed on your account!") }
            return
        }
        adMobManager.initializeIfNeeded {
            adMobManager.loadRewardedAd()
        }
        if (_uiState.value.isRewardedAdReady) {
            adMobManager.showRewardedAd(
                activity = activity,
                onUserEarnedReward = {
                    _uiState.update { it.copy(statusMessage = "Test Rewarded Ad completed! Reward earned successfully.") }
                },
                onAdDismissed = {},
                onAdFailedToShow = { msg ->
                    _uiState.update { it.copy(statusMessage = "Ad status: $msg") }
                }
            )
        } else {
            adMobManager.loadRewardedAd()
            _uiState.update { it.copy(statusMessage = "Ad is loading (${AdMobManager.TEST_REWARDED_AD_UNIT_ID}). Please try again in a moment.") }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun restorePurchases() {
        billingManager.queryExistingPurchases()
    }

    override fun onCleared() {
        super.onCleared()
        rendererHelper.close()
    }
}

