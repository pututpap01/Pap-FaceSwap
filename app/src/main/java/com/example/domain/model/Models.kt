package com.example.domain.model

import androidx.annotation.DrawableRes
import com.example.R

enum class AppTab(val title: String, val iconDescription: String) {
    SWAP("Tukar Wajah", "Face Swap Studio"),
    EDITOR("Editor Pro", "Advanced Photo Editor"),
    TEMPLATES("Template", "Preset Face Library"),
    GALLERY("Koleksi", "Saved Swaps Gallery"),
    VAULT("Brankas", "Encrypted Privacy Vault")
}

enum class SwapMode {
    LOCAL_AI_FAST,
    AI_ENHANCED
}

enum class FilterType(val displayName: String, val category: String) {
    NONE("Normal", "Dasar"),
    CYBERPUNK("Cyber Neon", "Estetik"),
    VINTAGE_90S("Vintage 90s", "Retro"),
    GOLDEN_HOUR("Golden Hour", "Sinematik"),
    FILM_NOIR("Film Noir", "Monokrom"),
    STUDIO_GLOW("Studio Glow", "Glamour"),
    COMIC_POP("Komik Pop", "Artistik"),
    WARM_SUNSET("Sunset Hangat", "Sinematik"),
    DRAMATIC_CONTRAST("Kontras Pro", "Pro")
}

enum class StickerType(val id: String, val displayName: String, val emoji: String) {
    SUNGLASSES_CYBER("sg_cyber", "Kacamata Cyber", "🕶️"),
    SUNGLASSES_COOL("sg_cool", "Kacamata Hitam", "🕶️"),
    CROWN_GOLD("cr_gold", "Mahkota Emas", "👑"),
    PARTY_HAT("pt_hat", "Topi Pesta", "🎉"),
    NEON_HALO("nn_halo", "Halo Neon", "✨"),
    CYBER_VISOR("cb_visor", "Visor Mecha", "🤖"),
    MUSTACHE("mc_retro", "Kumis Retro", "🥸"),
    FIRE_GLOW("fr_glow", "Aura Api", "🔥"),
    SPARKLES("sp_magic", "Sparkle Magic", "⭐")
}

data class PlacedSticker(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: StickerType,
    var offsetX: Float = 0.5f,
    var offsetY: Float = 0.5f,
    var scale: Float = 1.0f,
    var rotation: Float = 0f
)

data class TextOverlay(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "FaceMorph AI",
    val colorHex: String = "#FFFFFF",
    val fontSize: Float = 24f,
    val offsetX: Float = 0.5f,
    val offsetY: Float = 0.85f,
    val hasBackground: Boolean = true
)

data class FaceTemplate(
    val id: String,
    val title: String,
    val category: String,
    @DrawableRes val drawableRes: Int,
    val description: String,
    val tags: List<String>
)

object PresetTemplates {
    val list = listOf(
        FaceTemplate(
            id = "tpl_cyberpunk",
            title = "Cyber Neon 2077",
            category = "Cyberpunk",
            drawableRes = R.drawable.img_tpl_cyberpunk,
            description = "Gaya karakter masa depan dengan kilauan lampu neon kota metropolis",
            tags = listOf("Futuristik", "Neon", "Sci-Fi")
        ),
        FaceTemplate(
            id = "tpl_royal",
            title = "Bangsawan Renaissance",
            category = "Kerajaan",
            drawableRes = R.drawable.img_tpl_royal,
            description = "Lukisan klasik elegan era abad pertengahan dengan pakaian beludru emas",
            tags = listOf("Elegan", "Klasik", "Lukisan")
        ),
        FaceTemplate(
            id = "tpl_cinema",
            title = "Pahlawan Sinematik",
            category = "Film & Laga",
            drawableRes = R.drawable.img_tpl_cinema,
            description = "Pose sinematik blockbuster dengan efek pencahayaan dramatis",
            tags = listOf("Action", "Sinematik", "Hero")
        ),
        FaceTemplate(
            id = "tpl_hero_swap",
            title = "Aura Bintang Studio",
            category = "Studio Pro",
            drawableRes = R.drawable.img_hero_swap,
            description = "Transformasi glamor modern dengan pencahayaan softbox studio profesional",
            tags = listOf("Studio", "Glow", "Model")
        )
    )
}

data class EditorAdjustments(
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val skinWarmth: Float = 0f,       // -1f to 1f
    val brightness: Float = 0f,       // -0.5f to 0.5f
    val contrast: Float = 1.0f,       // 0.5f to 2.0f
    val saturation: Float = 1.0f,     // 0f to 2.0f
    val feathering: Float = 0.5f,     // 0.1f to 1.0f
    val blendIntensity: Float = 0.95f, // 0.2f to 1.0f
    val filter: FilterType = FilterType.NONE,
    val noWatermark: Boolean = true
)

data class FaceDetectionResult(
    val hasFace: Boolean,
    val faceCount: Int = 1,
    val boundsLeft: Float = 0.2f,
    val boundsTop: Float = 0.15f,
    val boundsRight: Float = 0.8f,
    val boundsBottom: Float = 0.85f,
    val eyeDistance: Float = 0.3f,
    val angle: Float = 0f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val estimatedSkinToneR: Int = 220,
    val estimatedSkinToneG: Int = 180,
    val estimatedSkinToneB: Int = 150
)
