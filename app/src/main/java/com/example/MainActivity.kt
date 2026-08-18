package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AppTab
import com.example.ui.components.PrivacyInfoDialog
import com.example.ui.components.SocialShareSheet
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FaceSwapViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                FaceMorphApp()
            }
        }
    }
}

@Composable
fun FaceMorphApp(
    viewModel: FaceSwapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val publicProjects by viewModel.publicProjects.collectAsStateWithLifecycle()
    val vaultProjects by viewModel.vaultProjects.collectAsStateWithLifecycle()
    val favoriteProjects by viewModel.favoriteProjects.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        BorderGlass,
                        androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    )
                    .testTag("main_navigation_bar"),
                color = Color(0xCC12131C),
                shadowElevation = 12.dp
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = PrimaryNeon,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.height(64.dp)
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = state.currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = getTabIcon(tab),
                                    contentDescription = tab.iconDescription,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryGlow,
                                indicatorColor = PrimaryNeon.copy(alpha = 0.85f),
                                unselectedIconColor = TextSecondary.copy(alpha = 0.6f),
                                unselectedTextColor = TextSecondary.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            // Ambient Atmospheric Glowing Orbs for Frosted Glass Depth
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-Left Indigo Glow Orb
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0x404F46E5),
                            Color(0x204F46E5),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = size.width * 0.75f
                    ),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = size.width * 0.75f
                )
                // Mid-Right Purple Glow Orb
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0x359333EA),
                            Color(0x189333EA),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.45f),
                        radius = size.width * 0.7f
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.45f),
                    radius = size.width * 0.7f
                )
                // Bottom-Left Cyan Ambient Accent
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color(0x2506B6D4),
                            Color(0x1006B6D4),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height),
                        radius = size.width * 0.6f
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height),
                    radius = size.width * 0.6f
                )
            }
            AnimatedContent(
                targetState = state.currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tabTransition"
            ) { targetTab ->
                when (targetTab) {
                    AppTab.SWAP -> {
                        SwapStudioScreen(
                            state = state,
                            onTargetSelected = { uri -> viewModel.setTargetImageFromUri(uri) },
                            onSourceSelected = { uri -> viewModel.setSourceImageFromUri(uri) },
                            onSwapSlots = { viewModel.swapTargetAndSource() },
                            onPerformSwap = { viewModel.performFaceSwap() },
                            onSelectTemplate = { template -> viewModel.selectTemplate(template) },
                            onOpenEditor = { viewModel.selectTab(AppTab.EDITOR) },
                            onShowPrivacyDialog = { viewModel.showPrivacyDialog(true) },
                            onShowShareSheet = { viewModel.showShareSheet(true) }
                        )
                    }

                    AppTab.EDITOR -> {
                        EditorScreen(
                            state = state,
                            onUpdateAdjustments = { adj -> viewModel.updateAdjustments(adj) },
                            onAddSticker = { sticker -> viewModel.addSticker(sticker) },
                            onRemoveSticker = { id -> viewModel.removeSticker(id) },
                            onAddText = { text, color -> viewModel.addTextOverlay(text, color) },
                            onRemoveText = { id -> viewModel.removeTextOverlay(id) },
                            onReset = { viewModel.resetAdjustments() },
                            onShowShareSheet = { viewModel.showShareSheet(true) },
                            onSaveToVault = { viewModel.saveCurrentToVault() }
                        )
                    }

                    AppTab.TEMPLATES -> {
                        TemplatesScreen(
                            onSelectTemplate = { template -> viewModel.selectTemplate(template) }
                        )
                    }

                    AppTab.GALLERY -> {
                        GalleryScreen(
                            projects = publicProjects,
                            favoriteProjects = favoriteProjects,
                            onSelectProject = { project -> viewModel.loadProjectIntoWorkspace(project) },
                            onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                            onMoveToVault = { id -> viewModel.moveProjectToVault(id, true) },
                            onDeleteProject = { id -> viewModel.deleteProject(id) }
                        )
                    }

                    AppTab.VAULT -> {
                        VaultScreen(
                            state = state,
                            vaultProjects = vaultProjects,
                            onSetPin = { pin -> viewModel.setVaultPin(pin) },
                            onUnlock = { pin -> viewModel.unlockVault(pin) },
                            onLockVault = { viewModel.lockVault() },
                            onClearVault = { viewModel.clearVault() },
                            onSelectVaultProject = { project -> viewModel.loadProjectIntoWorkspace(project) },
                            onMoveOutFromVault = { id -> viewModel.moveProjectToVault(id, false) },
                            onDeleteVaultProject = { id -> viewModel.deleteProject(id) }
                        )
                    }
                }
            }

            // Privacy Dialog
            if (state.showPrivacyDialog) {
                PrivacyInfoDialog(
                    onDismiss = { viewModel.showPrivacyDialog(false) }
                )
            }

            // Social Share Bottom Sheet
            if (state.showShareSheet) {
                SocialShareSheet(
                    bitmap = state.resultBitmap,
                    onDismiss = { viewModel.showShareSheet(false) },
                    onSaveToVault = {
                        viewModel.saveCurrentToVault()
                        viewModel.showShareSheet(false)
                    }
                )
            }
        }
    }
}

private fun getTabIcon(tab: AppTab): ImageVector {
    return when (tab) {
        AppTab.SWAP -> Icons.Default.AutoAwesome
        AppTab.EDITOR -> Icons.Default.Tune
        AppTab.TEMPLATES -> Icons.Default.Face
        AppTab.GALLERY -> Icons.Default.PhotoLibrary
        AppTab.VAULT -> Icons.Default.Lock
    }
}
