package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SocialShareManager
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareSheet(
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
    onSaveToVault: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF5131520),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x66FFFFFF)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header with badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Bagikan & Ekspor Foto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "✨ Kualitas HD • Bebas Watermark • Privasi Bersih",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGlow
                )
            }

            // Quick Gallery Save Button (Frosted Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassGradientPrimary)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable {
                        if (bitmap != null) {
                            SocialShareManager.saveToGallery(context, bitmap)
                            onDismiss()
                        }
                    }
                    .testTag("save_to_gallery_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan ke Galeri (Tanpa Watermark)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            // Social Media Direct App Buttons Grid
            Text(
                text = "Bagikan Langsung ke Aplikasi:",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SocialAppItem(
                    name = "Instagram",
                    iconColor = Color(0xFFE1306C),
                    emoji = "📸",
                    onClick = {
                        if (bitmap != null) {
                            SocialShareManager.shareToSocialApp(context, bitmap, "com.instagram.android")
                            onDismiss()
                        }
                    }
                )

                SocialAppItem(
                    name = "WhatsApp",
                    iconColor = Color(0xFF25D366),
                    emoji = "💬",
                    onClick = {
                        if (bitmap != null) {
                            SocialShareManager.shareToSocialApp(context, bitmap, "com.whatsapp")
                            onDismiss()
                        }
                    }
                )

                SocialAppItem(
                    name = "TikTok",
                    iconColor = Color(0xFF00F2FE),
                    emoji = "🎵",
                    onClick = {
                        if (bitmap != null) {
                            SocialShareManager.shareToSocialApp(context, bitmap, "com.zhiliaoapp.musically")
                            onDismiss()
                        }
                    }
                )

                SocialAppItem(
                    name = "Twitter / X",
                    iconColor = Color(0xFF1DA1F2),
                    emoji = "✖️",
                    onClick = {
                        if (bitmap != null) {
                            SocialShareManager.shareToSocialApp(context, bitmap, "com.twitter.android")
                            onDismiss()
                        }
                    }
                )

                SocialAppItem(
                    name = "Lainnya",
                    iconColor = PrimaryNeon,
                    emoji = "🔗",
                    onClick = {
                        if (bitmap != null) {
                            SocialShareManager.shareToSocialApp(context, bitmap, null)
                            onDismiss()
                        }
                    }
                )
            }

            Divider(color = BorderGlass, thickness = 1.dp)

            // Secondary Actions: Save to Encrypted Vault (Frosted Card Button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                    .clickable {
                        onSaveToVault()
                        onDismiss()
                    }
                    .testTag("save_to_vault_action_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan ke Brankas Privasi (Terenkripsi AES-256)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialAppItem(
    name: String,
    iconColor: Color,
    emoji: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.18f))
                .border(1.dp, iconColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
