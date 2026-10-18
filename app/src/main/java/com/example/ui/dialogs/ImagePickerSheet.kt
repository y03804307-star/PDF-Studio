package com.example.ui.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ImageFilterType
import com.example.model.ImageElement

data class StockPhotoItem(
    val title: String,
    val resId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSheet(
    selectedImageElement: ImageElement?,
    onDismiss: () -> Unit,
    onImageSelected: (Bitmap) -> Unit,
    onFilterChanged: (ImageFilterType) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onDeleteImage: () -> Unit
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bmp != null) {
                    onImageSelected(bmp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val stockPhotos = listOf(
        StockPhotoItem("Modern Center", R.drawable.img_sample_photo_1792325021983),
        StockPhotoItem("Design Studio", R.drawable.img_splash_hero_1792325006879)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedImageElement != null) "Edit Image" else "Insert Image",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedImageElement != null) "Replace, filter or adjust transparency" else "Choose from device or curated photography",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Choose from Device Button
            FilledTonalButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_device_photo_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose from Device Gallery")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stock / Curated Photography
            Text(
                text = "Curated Photography & Assets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(stockPhotos) { stock ->
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .height(110.dp)
                            .clickable {
                                try {
                                    val bmp = BitmapFactory.decodeResource(context.resources, stock.resId)
                                    if (bmp != null) {
                                        onImageSelected(bmp)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = stock.resId),
                                contentDescription = stock.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stock.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Controls for currently selected image
            if (selectedImageElement != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Image Filters",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedImageElement.filter == ImageFilterType.NONE,
                        onClick = { onFilterChanged(ImageFilterType.NONE) },
                        label = { Text("Original") }
                    )
                    FilterChip(
                        selected = selectedImageElement.filter == ImageFilterType.GRAYSCALE,
                        onClick = { onFilterChanged(ImageFilterType.GRAYSCALE) },
                        label = { Text("B&W") }
                    )
                    FilterChip(
                        selected = selectedImageElement.filter == ImageFilterType.SEPIA,
                        onClick = { onFilterChanged(ImageFilterType.SEPIA) },
                        label = { Text("Sepia") }
                    )
                    FilterChip(
                        selected = selectedImageElement.filter == ImageFilterType.HIGH_CONTRAST,
                        onClick = { onFilterChanged(ImageFilterType.HIGH_CONTRAST) },
                        label = { Text("High Contrast") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Opacity: ${(selectedImageElement.opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = selectedImageElement.opacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDeleteImage,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Image")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
