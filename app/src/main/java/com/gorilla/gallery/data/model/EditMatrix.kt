package com.gorilla.gallery.data.model

import android.graphics.ColorMatrix

/**
 * Builds a single [ColorMatrix] from an [EditState]: a named preset base with the user's
 * brightness/contrast/saturation/warmth post-concatenated on top. Pure + side-effect-free
 * so the Compose preview (GPU colour filter) and the on-save bake produce identical pixels.
 */
object EditMatrix {

    fun preset(p: FilterPreset): ColorMatrix = when (p) {
        FilterPreset.ORIGINAL -> ColorMatrix()
        FilterPreset.VIVID -> ColorMatrix().apply {
            postConcat(saturation(1.4f)); postConcat(contrast(0.15f))
        }
        FilterPreset.COOL -> warmth(-0.35f)
        FilterPreset.WARM -> warmth(0.35f)
        FilterPreset.FADED -> ColorMatrix().apply {
            postConcat(contrast(-0.2f)); postConcat(brightness(0.08f)); postConcat(saturation(0.85f))
        }
        FilterPreset.MONO -> saturation(0f)
    }

    fun combined(edit: EditState): ColorMatrix = ColorMatrix().apply {
        postConcat(preset(edit.preset))
        postConcat(exposure(edit.exposure))
        postConcat(brilliance(edit.brilliance))
        postConcat(brightness(edit.brightness))
        postConcat(highlights(edit.highlights))
        postConcat(shadows(edit.shadows))
        postConcat(contrast(edit.contrast))
        postConcat(saturation(edit.saturation))
        postConcat(vibrancy(edit.vibrancy))
        postConcat(warmth(edit.warmth))
    }

    /** e in [-1,1] -> channel scale around black, similar to exposure compensation. */
    fun exposure(e: Float): ColorMatrix {
        val s = 1f + e * 0.65f
        return ColorMatrix(
            floatArrayOf(
                s, 0f, 0f, 0f, 0f,
                0f, s, 0f, 0f, 0f,
                0f, 0f, s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    /** b in [-1,1] -> combined lift and midtone contrast. */
    fun brilliance(b: Float): ColorMatrix = ColorMatrix().apply {
        postConcat(contrast(b * 0.24f))
        postConcat(brightness(b * 0.16f))
        postConcat(saturation(1f + b * 0.12f))
    }

    /** b in [-1,1] → luminance offset on the 0..255 scale. */
    fun brightness(b: Float): ColorMatrix {
        val o = b * 100f
        return ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, o,
                0f, 1f, 0f, 0f, o,
                0f, 0f, 1f, 0f, o,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    /** c in [-1,1] → scale factor 0..2 around mid-grey. */
    fun contrast(c: Float): ColorMatrix {
        val s = 1f + c
        val t = (1f - s) * 127.5f
        return ColorMatrix(
            floatArrayOf(
                s, 0f, 0f, 0f, t,
                0f, s, 0f, 0f, t,
                0f, 0f, s, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    /** s in [0,2]; 1 = unchanged. */
    fun saturation(s: Float): ColorMatrix = ColorMatrix().apply { setSaturation(s) }

    /** h in [-1,1] -> approximate highlight recovery/boost using upper-mid pivot. */
    fun highlights(h: Float): ColorMatrix {
        val s = 1f + h * 0.34f
        val t = (1f - s) * 192f
        return ColorMatrix(
            floatArrayOf(
                s, 0f, 0f, 0f, t,
                0f, s, 0f, 0f, t,
                0f, 0f, s, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    /** s in [-1,1] -> approximate shadow lift/crush using lower-mid pivot. */
    fun shadows(s: Float): ColorMatrix {
        val scale = 1f - s * 0.34f
        val offset = (1f - scale) * 64f
        return ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    /** v in [-1,1] -> safe saturation-style vibrancy approximation. */
    fun vibrancy(v: Float): ColorMatrix = saturation(1f + v * 0.55f)

    /** w in [-1,1]; positive warms (more red, less blue). */
    fun warmth(w: Float): ColorMatrix {
        val r = 1f + w * 0.25f
        val b = 1f - w * 0.25f
        return ColorMatrix(
            floatArrayOf(
                r, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}
