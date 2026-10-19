package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PdfEditorViewModel

enum class AppScreen {
    SPLASH,
    EDITOR
}

class MainActivity : ComponentActivity() {

    private val viewModel: PdfEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pre-create WebView cache directory structure to prevent Chromium simple_file_enumerator opendir errors
        try {
            val dirs = listOf(
                java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js"),
                java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm"),
                java.io.File(cacheDir, "WebView/Default/HTTP Cache/Service Worker/Code Cache/js"),
                java.io.File(cacheDir, "WebView/Default/HTTP Cache/Service Worker/Code Cache/wasm")
            )
            for (d in dirs) {
                if (!d.exists()) {
                    d.mkdirs()
                }
            }
        } catch (_: Exception) {}

        // Handle opening a PDF passed via intent
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

                    // Predictive back handling
                    BackHandler(enabled = currentScreen == AppScreen.EDITOR) {
                        currentScreen = AppScreen.SPLASH
                    }

                    val state by viewModel.uiState.collectAsState()
                    val activity = this@MainActivity

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == AppScreen.EDITOR) {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                slideInVertically { height -> -height } + fadeIn() togetherWith
                                        slideOutVertically { height -> height } + fadeOut()
                            }
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SPLASH -> {
                                SplashScreen(
                                    currentDocumentTitle = state.documentTitle,
                                    isAdsRemoved = state.isAdsRemoved,
                                    isRewardedAdReady = state.isRewardedAdReady,
                                    statusMessage = state.statusMessage,
                                    onClearStatusMessage = { viewModel.clearStatusMessage() },
                                    onOpenRemoveAds = { viewModel.openRemoveAdsDialog() },
                                    onWatchTestRewardedAd = { viewModel.testWatchRewardedAd(activity) },
                                    onOpenSampleProposal = {
                                        viewModel.loadSampleProposal()
                                    },
                                    onOpenSampleInvoice = {
                                        viewModel.loadSampleInvoice()
                                    },
                                    onOpenSampleAgreement = {
                                        viewModel.loadSampleAgreement()
                                    },
                                    onOpenSampleResume = {
                                        viewModel.loadSampleResume()
                                    },
                                    onCreateBlank = {
                                        viewModel.createBlankDocument()
                                    },
                                    onOpenUri = { uri ->
                                        viewModel.openPdfFromUri(uri)
                                    },
                                    onEnterEditor = {
                                        currentScreen = AppScreen.EDITOR
                                    }
                                )
                            }
                            AppScreen.EDITOR -> {
                                EditorScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        currentScreen = AppScreen.SPLASH
                                    }
                                )
                            }
                        }
                    }

                    // Monetization & Ad dialogs accessible across the app
                    if (state.isRemoveAdsDialogOpen) {
                        com.example.ui.dialogs.RemoveAdsDialog(
                            isAdsRemoved = state.isAdsRemoved,
                            onDismiss = { viewModel.dismissRemoveAdsDialog() },
                            onPurchaseRemoveAds = { viewModel.purchaseRemoveAds(activity) },
                            onRestorePurchases = { viewModel.restorePurchases() }
                        )
                    }

                    if (state.isRewardedAdPromptOpen) {
                        com.example.ui.dialogs.RewardedAdPromptDialog(
                            isAdReady = state.isRewardedAdReady,
                            onWatchAd = { viewModel.watchRewardedAdAndExport(activity) },
                            onUpgradeNoAds = {
                                viewModel.dismissRewardedAdPrompt()
                                viewModel.openRemoveAdsDialog()
                            },
                            onDismiss = { viewModel.dismissRewardedAdPrompt() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null && (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT)) {
            viewModel.openPdfFromUri(uri)
        }
    }
}
