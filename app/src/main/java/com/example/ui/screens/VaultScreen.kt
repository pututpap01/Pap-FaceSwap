package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FaceSwapEntity
import com.example.engine.PrivacySecurityManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceSwapUiState
import java.io.File

@Composable
fun VaultScreen(
    state: FaceSwapUiState,
    vaultProjects: List<FaceSwapEntity>,
    onSetPin: (String) -> Unit,
    onUnlock: (String) -> Boolean,
    onLockVault: () -> Unit,
    onClearVault: () -> Unit,
    onSelectVaultProject: (FaceSwapEntity) -> Unit,
    onMoveOutFromVault: (Long) -> Unit,
    onDeleteVaultProject: (Long) -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var isSettingPinMode by remember { mutableStateOf(!state.isVaultPinSet) }
    var pinError by remember { mutableStateOf<String?>(null) }

    var isExifEnabled by remember {
        mutableStateOf(PrivacySecurityManager.isExifScrubbingEnabled(context))
    }
    var isLocalOnly by remember {
        mutableStateOf(PrivacySecurityManager.isLocalOnlyMode(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vault Header (Frosted Glass Style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Brankas Privasi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "Enkripsi End-to-End AES-256 GCM",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGlow
                )
            }

            if (state.isVaultUnlocked) {
                IconButton(
                    onClick = onLockVault,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, BorderGlass, CircleShape)
                        .testTag("lock_vault_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Kunci Brankas",
                        tint = AccentAmber
                    )
                }
            }
        }

        // Check if vault is locked or unlocked
        if (!state.isVaultUnlocked) {
            // LOCKED STATE OR SET PIN STATE (Frosted Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0x2E6366F1))
                            .border(1.5.dp, Color(0x606366F1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PrimaryGlow,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!state.isVaultPinSet || isSettingPinMode) {
                        // Set New PIN Flow
                        Text(
                            text = "Buat Kode PIN Brankas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "PIN digunakan untuk membuka kunci foto berenkripsi AES-256.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 6) newPinInput = it },
                            label = { Text("Masukkan PIN Baru (4-6 Angka)", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = BorderGlass,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x1AFFFFFF),
                                unfocusedContainerColor = Color(0x0DFFFFFF)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_new_pin_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 6) confirmPinInput = it },
                            label = { Text("Konfirmasi PIN", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = BorderGlass,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x1AFFFFFF),
                                unfocusedContainerColor = Color(0x0DFFFFFF)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_confirm_pin_input")
                        )

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pinError!!,
                                color = AccentPink,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassGradientPrimary)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (newPinInput.length < 4) {
                                        pinError = "PIN minimal 4 angka."
                                    } else if (newPinInput != confirmPinInput) {
                                        pinError = "Konfirmasi PIN tidak cocok!"
                                    } else {
                                        pinError = null
                                        onSetPin(newPinInput)
                                        isSettingPinMode = false
                                    }
                                }
                                .testTag("save_vault_pin_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Simpan PIN & Buka Brankas", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        // Enter PIN Flow
                        Text(
                            text = "Brankas Terkunci",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Masukkan PIN untuk mendekripsi foto pribadi Anda",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // PIN indicator dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            repeat(4) { idx ->
                                val isFilled = pinInput.length > idx
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isFilled) CyanGlow else Color(0x1AFFFFFF))
                                        .border(1.dp, if (isFilled) CyanGlow else BorderGlass, CircleShape)
                                )
                            }
                        }

                        // PIN Keypad Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.width(260.dp)
                        ) {
                            val rows = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "OK")
                            )

                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    row.forEach { key ->
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(if (key == "OK") GlassGradientPrimary else Brush.horizontalGradient(listOf(Color(0x24FFFFFF), Color(0x18FFFFFF))))
                                                .border(1.dp, if (key == "OK") Color.White.copy(alpha = 0.3f) else BorderGlass, CircleShape)
                                                .clickable {
                                                    when (key) {
                                                        "C" -> pinInput = ""
                                                        "OK" -> {
                                                            val success = onUnlock(pinInput)
                                                            if (!success) pinInput = ""
                                                        }
                                                        else -> {
                                                            if (pinInput.length < 6) {
                                                                pinInput += key
                                                                if (pinInput.length == 4) {
                                                                    val ok = onUnlock(pinInput)
                                                                    if (ok) pinInput = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("keypad_$key"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.vaultErrorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.vaultErrorMessage,
                                color = AccentPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        } else {
            // UNLOCKED STATE: ENCRYPTED GALLERY (Frosted Container)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ Status Privasi & Audit Keamanan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        TextButton(onClick = onClearVault) {
                            Text("Kosongkan Brankas", color = AccentPink, fontSize = 11.sp)
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pembersih Metadata EXIF Otomatis", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Hapus lokasi GPS & identitas kamera pada ekspor", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = isExifEnabled,
                            onCheckedChange = {
                                isExifEnabled = it
                                PrivacySecurityManager.setExifScrubbingEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessGreen,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }

                    Divider(color = BorderGlass)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mode Hanya AI Lokal (Offline)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Jaminan 0 byte data wajah keluar perangkat", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = isLocalOnly,
                            onCheckedChange = {
                                isLocalOnly = it
                                PrivacySecurityManager.setLocalOnlyMode(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuccessGreen,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            }

            // Grid of Encrypted Photos
            if (vaultProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Brankas Masih Kosong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Simpan hasil face swap ke brankas untuk mengenkripsinya dengan AES-256.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(vaultProjects) { item ->
                        val decryptedBmp = remember(item.resultImagePath) {
                            val file = File(item.resultImagePath)
                            PrivacySecurityManager.decryptBitmapFromFile(context, file)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onSelectVaultProject(item) }
                                .testTag("vault_item_${item.id}"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (decryptedBmp != null) {
                                    Image(
                                        bitmap = decryptedBmp.asImageBitmap(),
                                        contentDescription = item.title,
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
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber)
                                    }
                                }

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
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onMoveOutFromVault(item.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = "Keluarkan ke Galeri Publik",
                                                tint = CyanGlow,
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
    }
}
