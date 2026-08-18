package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PrivacyBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("privacy_guarantee_badge"),
        color = Color(0x1A22C55E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4022C55E))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen)
            )
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Privasi Terlindungi",
                tint = SuccessGreen,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "100% On-Device AI • Privasi Terenkripsi • Bebas Watermark",
                color = Color(0xFF86EFAC),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PrivacyInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131520),
        shape = RoundedCornerShape(28.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0x2022C55E))
                    .border(1.dp, Color(0x4022C55E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Jaminan Privasi & Keamanan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrivacyPoint(
                    icon = "🔒",
                    title = "Pemrosesan Lokal (On-Device)",
                    desc = "Foto Anda diproses langsung di memori perangkat tanpa dikirim ke server pihak ketiga."
                )
                PrivacyPoint(
                    icon = "🛡️",
                    title = "Enkripsi AES-256 GCM",
                    desc = "Foto di dalam 'Brankas Privasi' dienkripsi dengan standar militer dan dilindungi kode PIN."
                )
                PrivacyPoint(
                    icon = "🧹",
                    title = "Pembersih Metadata EXIF",
                    desc = "Informasi lokasi GPS, kamera, dan data pribadi dihapus otomatis saat mengekspor gambar."
                )
                PrivacyPoint(
                    icon = "✨",
                    title = "100% Gratis & Tanpa Watermark",
                    desc = "Hasil ekspor murni kualitas tinggi tanpa logo tempelan atau batasan penggunaan."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Saya Mengerti", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PrivacyPoint(
    icon: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
