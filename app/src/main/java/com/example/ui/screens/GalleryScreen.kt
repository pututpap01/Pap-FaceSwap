package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FaceSwapEntity
import com.example.engine.SocialShareManager
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    projects: List<FaceSwapEntity>,
    favoriteProjects: List<FaceSwapEntity>,
    onSelectProject: (FaceSwapEntity) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onMoveToVault: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    val context = LocalContext.current
    var showOnlyFavorites by remember { mutableStateOf(false) }
    val displayList = if (showOnlyFavorites) favoriteProjects else projects

    var selectedItemForAction by remember { mutableStateOf<FaceSwapEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header (Frosted Glass Style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Koleksi Karya Face Swap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${displayList.size} foto tersimpan • Kualitas Asli HD",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGlow
                )
            }

            // Filter Favorites Toggle Button (Frosted Pill)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (showOnlyFavorites) Color(0x33EC4899) else Color(0x14FFFFFF),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (showOnlyFavorites) AccentPink else BorderGlass
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showOnlyFavorites = !showOnlyFavorites }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (showOnlyFavorites) "❤️ Favorit" else "🤍 Favorit",
                        color = if (showOnlyFavorites) AccentPink else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (showOnlyFavorites) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Empty state (Frosted Card)
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, BorderGlass, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = PrimaryGlow,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (showOnlyFavorites) "Belum ada foto favorit" else "Belum ada riwayat tukar wajah",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mulai buat face swap pertama Anda di tab 'Tukar Wajah' secara instan dan tanpa watermark.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayList) { project ->
                    val file = remember(project.resultImagePath) { File(project.resultImagePath) }
                    val bitmap = remember(project.resultImagePath) {
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(236.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelectProject(project) }
                            .testTag("gallery_item_${project.id}"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = project.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x1AFFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }
                            }

                            // Favorite Icon Button (Frosted Circle)
                            IconButton(
                                onClick = { onToggleFavorite(project.id, project.isFavorite) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCC0D0D0F))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (project.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorit",
                                    tint = if (project.isFavorite) AccentPink else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Bottom Meta info (Frosted Bar)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                color = Color(0xCC0D0D0F),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = project.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        val dateStr = remember(project.timestamp) {
                                            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(project.timestamp))
                                        }
                                        Text(
                                            text = dateStr,
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }

                                    // More Menu Action
                                    IconButton(
                                        onClick = { selectedItemForAction = project },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opsi",
                                            tint = Color.White,
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

        // Action Sheet / Dialog for Selected Project (Frosted Glass Dialog)
        selectedItemForAction?.let { item ->
            val file = remember(item.resultImagePath) { File(item.resultImagePath) }
            val bmp = remember(item.resultImagePath) {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

            AlertDialog(
                onDismissRequest = { selectedItemForAction = null },
                containerColor = Color(0xFF131520),
                shape = RoundedCornerShape(24.dp),
                title = { Text(text = item.title, fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                onSelectProject(item)
                                selectedItemForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = CyanGlow)
                            Spacer(Modifier.width(8.dp))
                            Text("Buka di Editor Canggih", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                if (bmp != null) SocialShareManager.saveToGallery(context, bmp)
                                selectedItemForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = SuccessGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan ke Galeri HP", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                if (bmp != null) SocialShareManager.shareToSocialApp(context, bmp, null)
                                selectedItemForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryGlow)
                            Spacer(Modifier.width(8.dp))
                            Text("Bagikan ke Media Sosial", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                onMoveToVault(item.id)
                                selectedItemForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber)
                            Spacer(Modifier.width(8.dp))
                            Text("Pindahkan ke Brankas Terenkripsi", color = Color.White)
                        }

                        TextButton(
                            onClick = {
                                onDeleteProject(item.id)
                                selectedItemForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AccentPink)
                            Spacer(Modifier.width(8.dp))
                            Text("Hapus Foto", color = AccentPink)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedItemForAction = null }) {
                        Text("Tutup", color = TextSecondary)
                    }
                }
            )
        }
    }
}
