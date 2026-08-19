package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppTab
import com.example.domain.model.PresetTemplates
import com.example.domain.model.SwapMode
import com.example.engine.SocialShareManager
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.PrivacyBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceSwapUiState

@Composable
fun SwapStudioScreen(
    state: FaceSwapUiState,
    onTargetSelected: (Uri) -> Unit,
    onSourceSelected: (Uri) -> Unit,
    onSwapSlots: () -> Unit,
    onPerformSwap: () -> Unit,
    onSelectTemplate: (com.example.domain.model.FaceTemplate) -> Unit,
    onSelectSwapMode: (SwapMode) -> Unit = {},
    onOpenEditor: () -> Unit,
    onShowPrivacyDialog: () -> Unit,
    onShowShareSheet: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val targetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onTargetSelected(uri)
    }

    val sourceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onSourceSelected(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Frosted Glass Branding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "FaceMorph",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0x2E6366F1),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x606366F1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "AI PRO",
                            color = PrimaryGlow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Text(
                        text = "ON-DEVICE AI ACTIVE • TANPA WATERMARK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onShowPrivacyDialog,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, BorderGlass, CircleShape)
                        .testTag("privacy_shield_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Info Privasi & Keamanan",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Privacy Guarantee Badge
        PrivacyBadge(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowPrivacyDialog
        )

        // Hero Feature Banner (Frosted Glass Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x17FFFFFF))
                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_swap),
                contentDescription = "Banner Face Swap",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE60D0D0F),
                                Color(0x990D0D0F),
                                Color(0x404F46E5)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tukar Wajah Seketika ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih foto target & wajah sumber.\nAI Lokal menyatukan warna kulit alami!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33000000),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(SecondaryCyan)
                        )
                        Text(
                            text = "AUTO-DETECTION READY",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Quick Preset Templates Bar (Frosted Glass Card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET TEMPLATES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGlow,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Bebas Watermark",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(PresetTemplates.list) { template ->
                    val isSelected = state.targetTemplateId == template.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTemplate(template) }
                            .testTag("preset_template_${template.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryNeon else BorderGlass,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = template.drawableRes),
                                contentDescription = template.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryNeon),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) PrimaryGlow else TextSecondary,
                            maxLines = 1,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // AI Model Selection Mode (Generative Neural AI vs On-Device Fast ML Kit)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x18FFFFFF)),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PrimaryGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "PILIH MODEL FACE SWAP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGlow,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (state.swapMode == SwapMode.AI_ENHANCED) Color(0x30A855F7) else Color(0x203B82F6),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (state.swapMode == SwapMode.AI_ENHANCED) Color(0x60A855F7) else Color(0x503B82F6))
                    ) {
                        Text(
                            text = if (state.swapMode == SwapMode.AI_ENHANCED) "✨ Deep Neural AI" else "⚡ ML Kit Lokal",
                            color = if (state.swapMode == SwapMode.AI_ENHANCED) Color(0xFFE9D5FF) else Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Neural AI Generative Model
                    val isNeural = state.swapMode == SwapMode.AI_ENHANCED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isNeural) Modifier.background(GlassGradientPrimary)
                                else Modifier.background(Color(0x10FFFFFF))
                            )
                            .border(
                                1.dp,
                                if (isNeural) Color.White.copy(alpha = 0.4f) else BorderGlass,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectSwapMode(SwapMode.AI_ENHANCED) }
                            .padding(10.dp)
                            .testTag("mode_neural_ai")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("✨", fontSize = 12.sp)
                                Text(
                                    text = "Neural Deep AI",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Sintesis fotorealistik tekstur, cahaya & rambut (Gemini)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (isNeural) Color.White.copy(alpha = 0.9f) else TextSecondary,
                                maxLines = 2
                            )
                        }
                    }

                    // Option 2: ML Kit On-Device Landmark Model
                    val isLocal = state.swapMode == SwapMode.LOCAL_AI_FAST
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isLocal) Modifier.background(GlassGradientPrimary)
                                else Modifier.background(Color(0x10FFFFFF))
                            )
                            .border(
                                1.dp,
                                if (isLocal) Color.White.copy(alpha = 0.4f) else BorderGlass,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectSwapMode(SwapMode.LOCAL_AI_FAST) }
                            .padding(10.dp)
                            .testTag("mode_local_mlkit")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚡", fontSize = 12.sp)
                                Text(
                                    text = "ML Kit 3D Lokal",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Penyelarasan landmark 3D pupil & warna instan offline",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (isLocal) Color.White.copy(alpha = 0.9f) else TextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Dual Slot Photo Picker (Target Base vs Source Face)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot 1: Target Image (Latar / Karakter)
            PhotoSlotCard(
                title = "1. Foto Target",
                subtitle = "Latar / Karakter",
                bitmap = state.targetBitmap,
                onClick = { targetLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                testTag = "target_photo_slot"
            )

            // Flip / Swap Slots Button (Frosted Glass Circle)
            IconButton(
                onClick = onSwapSlots,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, BorderGlass, CircleShape)
                    .testTag("swap_slots_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Tukar Posisi Foto",
                    tint = PrimaryGlow
                )
            }

            // Slot 2: Source Face (Wajah Anda)
            PhotoSlotCard(
                title = "2. Wajah Anda",
                subtitle = "Sumber Wajah",
                bitmap = state.sourceBitmap,
                onClick = { sourceLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                testTag = "source_photo_slot"
            )
        }

        // Processing State Indicator
        AnimatedVisibility(visible = state.isProcessing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1CFFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { state.processingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryNeon,
                        trackColor = Color(0x20FFFFFF)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryNeon
                        )
                        Text(
                            text = state.processingMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGlow,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Primary Action: Swap Now Button (Frosted Gradient Glow Button)
        val canSwap = state.targetBitmap != null && state.sourceBitmap != null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (canSwap && !state.isProcessing) GlassGradientPrimary
                    else Brush.horizontalGradient(listOf(Color(0x24FFFFFF), Color(0x18FFFFFF)))
                )
                .border(
                    1.dp,
                    if (canSwap && !state.isProcessing) Color.White.copy(alpha = 0.3f) else BorderGlass,
                    RoundedCornerShape(24.dp)
                )
                .clickable(enabled = canSwap && !state.isProcessing) { onPerformSwap() }
                .testTag("perform_swap_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (canSwap && !state.isProcessing) Color.White else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isProcessing) "Sedang Memproses AI..." else "Tukar Wajah Instan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (canSwap && !state.isProcessing) Color.White else TextMuted
                )
            }
        }

        // Live Result & Before/After Comparison Area
        if (state.resultBitmap != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Hasil Face Swap AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x2022C55E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E))
                    ) {
                        Text(
                            text = "Bebas Watermark",
                            color = Color(0xFF86EFAC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Interactive Split Comparison
                BeforeAfterSlider(
                    beforeBitmap = state.targetBitmap,
                    afterBitmap = state.resultBitmap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )

                // Result Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open in Advanced Editor (Frosted Glass Button)
                    Button(
                        onClick = onOpenEditor,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_in_editor_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1CFFFFFF)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Lanjutan", color = Color.White, fontSize = 13.sp)
                    }

                    // Share & Export (Gradient Pill Button)
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GlassGradientPrimary)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .clickable { onShowShareSheet() }
                            .testTag("share_result_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Bagikan / Simpan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PhotoSlotCard(
    title: String,
    subtitle: String,
    bitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        modifier = modifier
            .height(154.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
        border = androidx.compose.foundation.BorderStroke(
            width = if (bitmap != null) 1.5.dp else 1.dp,
            color = if (bitmap != null) PrimaryNeon.copy(alpha = 0.7f) else BorderGlass
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Frosted Overlay label
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = Color(0xCC0D0D0F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ganti",
                            color = PrimaryGlow,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, BorderGlass, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = PrimaryGlow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
