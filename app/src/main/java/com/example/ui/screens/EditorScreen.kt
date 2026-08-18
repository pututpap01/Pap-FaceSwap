package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EditorAdjustments
import com.example.domain.model.FilterType
import com.example.domain.model.StickerType
import com.example.engine.SocialShareManager
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceSwapUiState

enum class EditorSubTab(val label: String, val icon: String) {
    ALIGNMENT("Posisi & Skala", "🎯"),
    COLOR_SKIN("Warna & Kulit", "☀️"),
    FILTERS("Filter Estetik", "🔮"),
    STICKERS("Aksesoris", "🕶️"),
    TEXT("Teks", "✍️")
}

@Composable
fun EditorScreen(
    state: FaceSwapUiState,
    onUpdateAdjustments: (EditorAdjustments) -> Unit,
    onAddSticker: (StickerType) -> Unit,
    onRemoveSticker: (String) -> Unit,
    onAddText: (String, String) -> Unit,
    onRemoveText: (String) -> Unit,
    onReset: () -> Unit,
    onShowShareSheet: () -> Unit,
    onSaveToVault: () -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(EditorSubTab.COLOR_SKIN) }
    var textInput by remember { mutableStateOf("") }
    var textColorHex by remember { mutableStateOf("#FFFFFF") }
    var showTextDialog by remember { mutableStateOf(false) }

    val adjustments = state.adjustments
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar Controls (Frosted Glass Header)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Editor Canggih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Penyetelan Wajah Presisi & Estetika",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGlow
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Reset Button (Frosted Circle)
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, BorderGlass, CircleShape)
                        .testTag("editor_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Pengaturan",
                        tint = TextSecondary
                    )
                }

                // Share / Export Button (Gradient Pill)
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassGradientPrimary)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { onShowShareSheet() }
                        .padding(horizontal = 14.dp)
                        .testTag("editor_export_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Ekspor HD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Before/After Interactive Comparison Canvas
        BeforeAfterSlider(
            beforeBitmap = state.targetBitmap,
            afterBitmap = state.resultBitmap ?: state.targetBitmap,
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        )

        // Sub-Tab Selector (Frosted Container)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x14FFFFFF),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeSubTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = PrimaryGlow,
                edgePadding = 8.dp,
                divider = {}
            ) {
                EditorSubTab.values().forEach { tab ->
                    val isSelected = activeSubTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { activeSubTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0x2E6366F1) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = tab.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        // Sub-Tab Content Trays (Frosted Glass Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x17FFFFFF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeSubTab) {
                    EditorSubTab.ALIGNMENT -> {
                        // Face Scale Slider
                        SliderControl(
                            label = "Ukuran Wajah (Skala)",
                            value = adjustments.scale,
                            range = 0.5f..1.8f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(scale = it)) }
                        )

                        // Offset X (Horizontal)
                        SliderControl(
                            label = "Geser Horizontal (X)",
                            value = adjustments.offsetX,
                            range = -0.3f..0.3f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(offsetX = it)) }
                        )

                        // Offset Y (Vertical)
                        SliderControl(
                            label = "Geser Vertikal (Y)",
                            value = adjustments.offsetY,
                            range = -0.3f..0.3f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(offsetY = it)) }
                        )

                        // Face Rotation
                        SliderControl(
                            label = "Kemiringan / Rotasi",
                            value = adjustments.rotation,
                            range = -30f..30f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(rotation = it)) }
                        )
                    }

                    EditorSubTab.COLOR_SKIN -> {
                        // Skin Warmth
                        SliderControl(
                            label = "Rona Kehangatan Kulit",
                            value = adjustments.skinWarmth,
                            range = -0.8f..0.8f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(skinWarmth = it)) }
                        )

                        // Feathering / Edge Softness
                        SliderControl(
                            label = "Kelembutan Tepi (Feathering)",
                            value = adjustments.feathering,
                            range = 0.1f..0.95f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(feathering = it)) }
                        )

                        // Blend Intensity
                        SliderControl(
                            label = "Intensitas Penyatuan (Blend)",
                            value = adjustments.blendIntensity,
                            range = 0.3f..1.0f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(blendIntensity = it)) }
                        )

                        // Brightness & Contrast
                        SliderControl(
                            label = "Kecerahan (Brightness)",
                            value = adjustments.brightness,
                            range = -0.4f..0.4f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(brightness = it)) }
                        )

                        SliderControl(
                            label = "Kontras",
                            value = adjustments.contrast,
                            range = 0.5f..1.6f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(contrast = it)) }
                        )

                        SliderControl(
                            label = "Saturasi Warna",
                            value = adjustments.saturation,
                            range = 0.0f..2.0f,
                            onValueChange = { onUpdateAdjustments(adjustments.copy(saturation = it)) }
                        )
                    }

                    EditorSubTab.FILTERS -> {
                        Text(
                            text = "Pilih Filter Warna Sinematik:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(FilterType.values()) { filter ->
                                val isSelected = adjustments.filter == filter
                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onUpdateAdjustments(adjustments.copy(filter = filter)) }
                                        .testTag("filter_option_${filter.name}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0x336366F1) else Color(0x14FFFFFF)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryNeon else BorderGlass
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = getFilterEmoji(filter),
                                            fontSize = 24.sp
                                        )
                                        Text(
                                            text = filter.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = filter.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    EditorSubTab.STICKERS -> {
                        Text(
                            text = "Tambahkan Aksesoris Menarik:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(StickerType.values()) { sticker ->
                                Button(
                                    onClick = { onAddSticker(sticker) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1CFFFFFF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
                                ) {
                                    Text(text = "${sticker.emoji} ${sticker.displayName}", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        // Active Stickers list
                        if (state.placedStickers.isNotEmpty()) {
                            Divider(color = BorderGlass)
                            Text(
                                text = "Aksesoris Terpasang (${state.placedStickers.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.placedStickers.forEach { placed ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveSticker(placed.id) },
                                        label = { Text("${placed.type.emoji} ${placed.type.displayName}", color = Color.White) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus Aksesoris",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    EditorSubTab.TEXT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overlay Teks Kustom:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )

                            Button(
                                onClick = { showTextDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Teks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (state.textOverlays.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.textOverlays.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x1CFFFFFF))
                                            .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "\"${item.text}\"",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        IconButton(
                                            onClick = { onRemoveText(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = AccentPink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Text Dialog (Frosted Glass Dialog)
        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                containerColor = Color(0xFF131520),
                shape = RoundedCornerShape(24.dp),
                title = { Text("Tambah Teks Kustom", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Contoh: Face Swap Keren 🎭", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Pilih Warna Teks:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("#FFFFFF", "#00E5FF", "#FF4081", "#FFD700", "#10B981").forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (textColorHex == hex) 2.5.dp else 1.dp,
                                            color = if (textColorHex == hex) Color.White else BorderGlass,
                                            shape = CircleShape
                                        )
                                        .clickable { textColorHex = hex }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onAddText(textInput, textColorHex)
                                textInput = ""
                                showTextDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Tambahkan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false }) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = CyanGlow,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = PrimaryNeon,
                activeTrackColor = PrimaryNeon,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

private fun getFilterEmoji(filter: FilterType): String {
    return when (filter) {
        FilterType.NONE -> "🖼️"
        FilterType.CYBERPUNK -> "🤖"
        FilterType.VINTAGE_90S -> "📼"
        FilterType.GOLDEN_HOUR -> "🌅"
        FilterType.FILM_NOIR -> "🎬"
        FilterType.STUDIO_GLOW -> "✨"
        FilterType.COMIC_POP -> "💥"
        FilterType.WARM_SUNSET -> "🌇"
        FilterType.DRAMATIC_CONTRAST -> "⚡"
    }
}
