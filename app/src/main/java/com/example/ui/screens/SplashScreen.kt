package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun SplashScreen(
    currentDocumentTitle: String? = null,
    isAdsRemoved: Boolean = false,
    isRewardedAdReady: Boolean = false,
    statusMessage: String? = null,
    onClearStatusMessage: () -> Unit = {},
    onOpenRemoveAds: () -> Unit = {},
    onWatchTestRewardedAd: () -> Unit = {},
    onOpenSampleProposal: () -> Unit,
    onOpenSampleInvoice: () -> Unit,
    onOpenSampleAgreement: () -> Unit,
    onOpenSampleResume: () -> Unit,
    onCreateBlank: () -> Unit,
    onOpenUri: (Uri) -> Unit,
    onEnterEditor: () -> Unit
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onOpenUri(uri)
            onEnterEditor()
        }
    }

    // Pure obsidian dark background - no background photo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C16))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional status message toast/banner
            if (statusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusMessage,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearStatusMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Header: Large Red PDF Document Icon + PDF Studio: Edit PDF + PRO pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large White squircle card showcasing the red folded PDF document icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x66000000))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pdf_icon_red_1792328684230),
                        contentDescription = "PDF Studio: Edit PDF",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PDF Studio: Edit PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // PRO Pill (Clickable)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isAdsRemoved) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF059669), Color(0xFF10B981))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFE11D48), Color(0xFFF59E0B))
                                        )
                                    }
                                )
                                .clickable { onOpenRemoveAds() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAdsRemoved) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "PRO ACTIVE",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "REMOVE ADS",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Clean Minimal Feature Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureBadge(dotColor = Color(0xFFEF4444), text = "Text, Erase & Replace")
                FeatureBadge(dotColor = Color(0xFF38BDF8), text = "Unsplash Stock Photos")
                FeatureBadge(dotColor = Color(0xFFA855F7), text = "Digital Signatures")
            }

            // Big Vibrant Hero Card: "Upload & Edit PDF" with longer capsule icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF8B5CF6))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2563EB),
                                Color(0xFF7C3AED),
                                Color(0xFFDB2777)
                            )
                        )
                    )
                    .clickable {
                        filePicker.launch(arrayOf("application/pdf"))
                    }
                    .testTag("upload_edit_pdf_card")
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Longer capsule container for the icon
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Upload PDF",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Text(
                        text = "Upload & Edit PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 19.sp
                    )
                }
            }

            // Section Label: START WITH TEMPLATE
            Text(
                text = "START WITH TEMPLATE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.3.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            // 2x2 Grid of Colorful Template Cards (Clean, Bold, Longer Capsule Icons)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: Invoice (Blue Gradient)
                    ColorfulTemplateCard(
                        title = "Invoice",
                        icon = Icons.Default.ReceiptLong,
                        gradient = listOf(Color(0xFF1D4ED8), Color(0xFF0284C7)),
                        testTag = "template_invoice_card",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenSampleInvoice()
                            onEnterEditor()
                        }
                    )

                    // Card 2: Agreement (Green Gradient)
                    ColorfulTemplateCard(
                        title = "Agreement",
                        icon = Icons.Default.Article,
                        gradient = listOf(Color(0xFF047857), Color(0xFF10B981)),
                        testTag = "template_agreement_card",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenSampleAgreement()
                            onEnterEditor()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 3: Resume (Purple Gradient)
                    ColorfulTemplateCard(
                        title = "Resume",
                        icon = Icons.Default.Badge,
                        gradient = listOf(Color(0xFF6D28D9), Color(0xFFA855F7)),
                        testTag = "template_resume_card",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenSampleResume()
                            onEnterEditor()
                        }
                    )

                    // Card 4: Proposal (Orange Gradient)
                    ColorfulTemplateCard(
                        title = "Proposal",
                        icon = Icons.Default.Assessment,
                        gradient = listOf(Color(0xFFC2410C), Color(0xFFF97316)),
                        testTag = "template_proposal_card",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenSampleProposal()
                            onEnterEditor()
                        }
                    )
                }
            }

            // "Create Blank Document" Button with Longer Capsule Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0E172A))
                    .border(1.5.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable {
                        onCreateBlank()
                        onEnterEditor()
                    }
                    .testTag("create_blank_doc_btn")
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Create Blank Document",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Section Label: RECENT PROJECTS
            Text(
                text = "RECENT PROJECTS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.3.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Recent Projects Card Container (Clean, Minimal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                if (currentDocumentTitle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = currentDocumentTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onEnterEditor,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("resume_document_btn")
                        ) {
                            Text("Open", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No documents yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Section Label: MONETIZATION & ADS
            Text(
                text = "MONETIZATION & ADS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.3.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Monetization & AdMob Test Hub Card
            MonetizationCard(
                isAdsRemoved = isAdsRemoved,
                isRewardedAdReady = isRewardedAdReady,
                onWatchTestRewardedAd = onWatchTestRewardedAd,
                onOpenRemoveAds = onOpenRemoveAds
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonetizationCard(
    isAdsRemoved: Boolean,
    isRewardedAdReady: Boolean,
    onWatchTestRewardedAd: () -> Unit,
    onOpenRemoveAds: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rewarded Ad Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Test Rewarded Ad",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isRewardedAdReady) "Ad ready to play" else "Preloading test ad...",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRewardedAdReady) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onWatchTestRewardedAd,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("watch_test_ad_home_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Watch Ad", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Remove Ads IAP Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isAdsRemoved) Color(0xFF065F46) else Color(0xFF4C0519)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdsRemoved) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint = if (isAdsRemoved) Color(0xFF34D399) else Color(0xFFFB7185),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAdsRemoved) "Ads Removed (PRO)" else "In-App Ad Removal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isAdsRemoved) "Lifetime License Active" else "One-time $2.99 IAP purchase",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAdsRemoved) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = onOpenRemoveAds,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdsRemoved) Color(0xFF1E293B) else Color(0xFFE11D48)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_remove_ads_home_btn")
                ) {
                    Icon(
                        imageVector = if (isAdsRemoved) Icons.Default.Star else Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAdsRemoved) "Licensed" else "Remove Ads",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(
    dotColor: Color,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF111827),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color(0xFFE2E8F0),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun ColorfulTemplateCard(
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(115.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = gradient.first())
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(gradient))
            .clickable { onClick() }
            .testTag(testTag)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Longer capsule badge for the icon
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 17.sp
            )
        }
    }
}
