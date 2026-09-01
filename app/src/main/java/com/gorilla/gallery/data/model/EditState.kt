package com.gorilla.gallery.data.model

/** Filter presets for the editor; each maps to a base ColorMatrix in editor/Adjust.kt. */
enum class FilterPreset(val label: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    COOL("Cool"),
    WARM("Warm"),
    FADED("Faded"),
    MONO("Mono"),
}

/** Aspect-ratio presets for the crop tool. null = free. */
enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT("4:5", 4f / 5f),
    STORY("9:16", 9f / 16f),
    STANDARD("4:3", 4f / 3f),
    WIDE("16:9", 16f / 9f),
    ORIGINAL("Original", null),
}

/**
 * Non-destructive edit state. Sliders are normalised:
 * exposure/brilliance/brightness/highlights/shadows/contrast/warmth/vibrancy in [-1, 1]
 * (0 = none), saturation in [0, 2] (1 = none), vignette/sharpness in [0, 1] (0 = none).
 * [rotationDeg] is the total rotation; [cropRectN] is a normalised crop in [0,1].
 */
data class EditState(
    val preset: FilterPreset = FilterPreset.ORIGINAL,
    val exposure: Float = 0f,
    val brilliance: Float = 0f,
    val brightness: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 1f,
    val vibrancy: Float = 0f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val sharpness: Float = 0f,
    val rotationDeg: Float = 0f,
    val cropAspect: CropAspect = CropAspect.FREE,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
) {
    val hasEdits: Boolean
        get() = preset != FilterPreset.ORIGINAL ||
            exposure != 0f || brilliance != 0f || brightness != 0f ||
            highlights != 0f || shadows != 0f || contrast != 0f ||
            saturation != 1f || vibrancy != 0f || warmth != 0f ||
            vignette > 0f || sharpness > 0f || rotationDeg != 0f ||
            cropLeft != 0f || cropTop != 0f || cropRight != 1f || cropBottom != 1f
}
