package com.gorilla.gallery.data.model

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import androidx.media3.effect.SingleColorLut

object VideoEffects {
    fun createFilterLut(preset: FilterPreset): SingleColorLut? {
        if (preset == FilterPreset.ORIGINAL) return null
        
        val matrix = EditMatrix.preset(preset)
        val m = matrix.array
        
        val n = 32
        val lutCube = Array(n) { rIndex ->
            Array(n) { gIndex ->
                IntArray(n) { bIndex ->
                    val r = (rIndex * 255f) / (n - 1)
                    val g = (gIndex * 255f) / (n - 1)
                    val b = (bIndex * 255f) / (n - 1)

                    val newR = (m[0] * r + m[1] * g + m[2] * b + m[3] * 255f + m[4]).coerceIn(0f, 255f).toInt()
                    val newG = (m[5] * r + m[6] * g + m[7] * b + m[8] * 255f + m[9]).coerceIn(0f, 255f).toInt()
                    val newB = (m[10] * r + m[11] * g + m[12] * b + m[13] * 255f + m[14]).coerceIn(0f, 255f).toInt()
                    val newA = (m[15] * r + m[16] * g + m[17] * b + m[18] * 255f + m[19]).coerceIn(0f, 255f).toInt()

                    Color.argb(newA, newR, newG, newB)
                }
            }
        }
        return SingleColorLut.createFromCube(lutCube)
    }
}
