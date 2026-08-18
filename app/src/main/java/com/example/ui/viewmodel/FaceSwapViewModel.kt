package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FaceSwapEntity
import com.example.data.local.FaceSwapRepository
import com.example.domain.model.*
import com.example.engine.FaceSwapEngine
import com.example.engine.PrivacySecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FaceSwapUiState(
    val currentTab: AppTab = AppTab.SWAP,
    val targetBitmap: Bitmap? = null,
    val targetUri: Uri? = null,
    val targetTemplateId: String? = null,
    val sourceBitmap: Bitmap? = null,
    val sourceUri: Uri? = null,
    val resultBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val processingProgress: Float = 0f,
    val swapMode: SwapMode = SwapMode.LOCAL_AI_FAST,
    val adjustments: EditorAdjustments = EditorAdjustments(),
    val placedStickers: List<PlacedSticker> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val selectedStickerId: String? = null,
    val isVaultUnlocked: Boolean = false,
    val isVaultPinSet: Boolean = false,
    val vaultErrorMessage: String? = null,
    val showPrivacyDialog: Boolean = false,
    val showShareSheet: Boolean = false,
    val activeCompareMode: Boolean = true
)

class FaceSwapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FaceSwapRepository
    private val _uiState = MutableStateFlow(FaceSwapUiState())
    val uiState: StateFlow<FaceSwapUiState> = _uiState.asStateFlow()

    val publicProjects: StateFlow<List<FaceSwapEntity>>
    val vaultProjects: StateFlow<List<FaceSwapEntity>>
    val favoriteProjects: StateFlow<List<FaceSwapEntity>>

    private var liveUpdateJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FaceSwapRepository(application, db.faceSwapDao())

        publicProjects = repository.publicProjects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        vaultProjects = repository.vaultProjects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        favoriteProjects = repository.favoriteProjects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val isPinSet = PrivacySecurityManager.isVaultPinSet(application)
        _uiState.update { it.copy(isVaultPinSet = isPinSet) }

        // Load default template into Target slot to make first-time user experience instant & delightful
        loadDefaultTemplate()
    }

    private fun loadDefaultTemplate() {
        viewModelScope.launch {
            val defaultTpl = PresetTemplates.list.firstOrNull() ?: return@launch
            val bmp = repository.loadBitmapFromResource(defaultTpl.drawableRes)
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        targetBitmap = bmp,
                        targetTemplateId = defaultTpl.id
                    )
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setTargetImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val bmp = repository.loadBitmapFromUri(uri)
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        targetBitmap = bmp,
                        targetUri = uri,
                        targetTemplateId = null
                    )
                }
                autoTriggerSwapIfReady()
            }
        }
    }

    fun setSourceImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val bmp = repository.loadBitmapFromUri(uri)
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        sourceBitmap = bmp,
                        sourceUri = uri
                    )
                }
                autoTriggerSwapIfReady()
            }
        }
    }

    fun selectTemplate(template: FaceTemplate) {
        viewModelScope.launch {
            val bmp = repository.loadBitmapFromResource(template.drawableRes)
            if (bmp != null) {
                _uiState.update {
                    it.copy(
                        targetBitmap = bmp,
                        targetTemplateId = template.id,
                        currentTab = AppTab.SWAP
                    )
                }
                autoTriggerSwapIfReady()
            }
        }
    }

    fun swapTargetAndSource() {
        val currentTarget = _uiState.value.targetBitmap
        val currentSource = _uiState.value.sourceBitmap
        if (currentTarget != null && currentSource != null) {
            _uiState.update {
                it.copy(
                    targetBitmap = currentSource,
                    sourceBitmap = currentTarget,
                    targetUri = it.sourceUri,
                    sourceUri = it.targetUri,
                    targetTemplateId = null
                )
            }
            performFaceSwap()
        }
    }

    private fun autoTriggerSwapIfReady() {
        if (_uiState.value.targetBitmap != null && _uiState.value.sourceBitmap != null) {
            performFaceSwap()
        }
    }

    fun performFaceSwap() {
        val target = _uiState.value.targetBitmap ?: return
        val source = _uiState.value.sourceBitmap ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingMessage = "Memulai Pemrosesan AI Lokal...",
                    processingProgress = 0.15f
                )
            }

            delay(150)
            _uiState.update {
                it.copy(
                    processingMessage = "Mendeteksi landmark wajah & posisi mata...",
                    processingProgress = 0.45f
                )
            }

            delay(200)
            _uiState.update {
                it.copy(
                    processingMessage = "Harmonisasi rona kulit & pencahayaan...",
                    processingProgress = 0.75f
                )
            }

            val result = FaceSwapEngine.performFaceSwap(
                targetBitmap = target,
                sourceBitmap = source,
                adjustments = _uiState.value.adjustments,
                stickers = _uiState.value.placedStickers,
                textOverlays = _uiState.value.textOverlays
            )

            _uiState.update {
                it.copy(
                    resultBitmap = result,
                    isProcessing = false,
                    processingMessage = "Selesai!",
                    processingProgress = 1.0f
                )
            }

            // Save auto history
            repository.saveProject(
                title = "Swap AI ${_uiState.value.targetTemplateId ?: "Kustom"}",
                targetBmp = target,
                sourceBmp = source,
                resultBmp = result,
                templateId = _uiState.value.targetTemplateId,
                filterName = _uiState.value.adjustments.filter.name
            )
        }
    }

    fun updateAdjustments(newAdjustments: EditorAdjustments) {
        _uiState.update { it.copy(adjustments = newAdjustments) }
        scheduleLiveRecompute()
    }

    private fun scheduleLiveRecompute() {
        liveUpdateJob?.cancel()
        liveUpdateJob = viewModelScope.launch {
            delay(100) // Debounce slider events
            val target = _uiState.value.targetBitmap ?: return@launch
            val source = _uiState.value.sourceBitmap ?: return@launch

            val result = FaceSwapEngine.performFaceSwap(
                targetBitmap = target,
                sourceBitmap = source,
                adjustments = _uiState.value.adjustments,
                stickers = _uiState.value.placedStickers,
                textOverlays = _uiState.value.textOverlays
            )
            _uiState.update { it.copy(resultBitmap = result) }
        }
    }

    fun addSticker(type: StickerType) {
        val newSticker = PlacedSticker(type = type)
        _uiState.update {
            it.copy(placedStickers = it.placedStickers + newSticker)
        }
        scheduleLiveRecompute()
    }

    fun removeSticker(id: String) {
        _uiState.update {
            it.copy(placedStickers = it.placedStickers.filter { s -> s.id != id })
        }
        scheduleLiveRecompute()
    }

    fun addTextOverlay(text: String, colorHex: String = "#FFFFFF") {
        val overlay = TextOverlay(text = text, colorHex = colorHex)
        _uiState.update {
            it.copy(textOverlays = it.textOverlays + overlay)
        }
        scheduleLiveRecompute()
    }

    fun removeTextOverlay(id: String) {
        _uiState.update {
            it.copy(textOverlays = it.textOverlays.filter { t -> t.id != id })
        }
        scheduleLiveRecompute()
    }

    fun resetAdjustments() {
        _uiState.update {
            it.copy(
                adjustments = EditorAdjustments(),
                placedStickers = emptyList(),
                textOverlays = emptyList()
            )
        }
        scheduleLiveRecompute()
    }

    fun saveCurrentToVault() {
        val target = _uiState.value.targetBitmap ?: return
        val source = _uiState.value.sourceBitmap ?: return
        val result = _uiState.value.resultBitmap ?: return

        viewModelScope.launch {
            repository.saveProject(
                title = "Vault_${System.currentTimeMillis() % 10000}",
                targetBmp = target,
                sourceBmp = source,
                resultBmp = result,
                isVault = true,
                filterName = _uiState.value.adjustments.filter.name
            )
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, current)
        }
    }

    fun moveProjectToVault(id: Long, toVault: Boolean) {
        viewModelScope.launch {
            repository.moveToVault(id, toVault)
        }
    }

    fun setVaultPin(pin: String) {
        PrivacySecurityManager.setVaultPin(getApplication(), pin)
        _uiState.update {
            it.copy(
                isVaultPinSet = true,
                isVaultUnlocked = true,
                vaultErrorMessage = null
            )
        }
    }

    fun unlockVault(pin: String): Boolean {
        val isValid = PrivacySecurityManager.verifyVaultPin(getApplication(), pin)
        if (isValid) {
            _uiState.update {
                it.copy(
                    isVaultUnlocked = true,
                    vaultErrorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(vaultErrorMessage = "PIN salah. Silakan coba lagi.")
            }
        }
        return isValid
    }

    fun lockVault() {
        _uiState.update {
            it.copy(isVaultUnlocked = false)
        }
    }

    fun clearVault() {
        viewModelScope.launch {
            repository.clearVault()
        }
    }

    fun showPrivacyDialog(show: Boolean) {
        _uiState.update { it.copy(showPrivacyDialog = show) }
    }

    fun showShareSheet(show: Boolean) {
        _uiState.update { it.copy(showShareSheet = show) }
    }

    fun loadProjectIntoWorkspace(project: FaceSwapEntity) {
        viewModelScope.launch {
            val targetBmp = repository.loadBitmapFromPath(project.targetImagePath)
            val sourceBmp = repository.loadBitmapFromPath(project.sourceImagePath)
            val resultBmp = repository.loadBitmapFromPath(project.resultImagePath, project.isEncryptedVault)

            val filter = try {
                FilterType.valueOf(project.filterName)
            } catch (e: Exception) {
                FilterType.NONE
            }

            _uiState.update {
                it.copy(
                    targetBitmap = targetBmp,
                    sourceBitmap = sourceBmp,
                    resultBitmap = resultBmp,
                    adjustments = EditorAdjustments(
                        scale = project.scale,
                        offsetX = project.offsetX,
                        offsetY = project.offsetY,
                        rotation = project.rotation,
                        skinWarmth = project.skinWarmth,
                        brightness = project.brightness,
                        contrast = project.contrast,
                        saturation = project.saturation,
                        filter = filter
                    ),
                    currentTab = AppTab.EDITOR
                )
            }
        }
    }
}
