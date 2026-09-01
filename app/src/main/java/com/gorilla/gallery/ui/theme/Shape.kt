package com.gorilla.gallery.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radius tokens used across the app. */
object Corners {
    val card = 20.dp
    val sheetTop = 28.dp
    val chip = 14.dp
    val thumb = 16.dp
    val miniPlayer = 22.dp
    /** Fully rounded capsule — large value clamped by RoundedCornerShape. */
    val capsule = 100.dp
}

val GorillaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(Corners.chip),
    medium = RoundedCornerShape(Corners.card),
    large = RoundedCornerShape(Corners.sheetTop),
    extraLarge = RoundedCornerShape(Corners.capsule),
)

val CardShape = RoundedCornerShape(Corners.card)
val ThumbShape = RoundedCornerShape(Corners.thumb)
val CapsuleShape = RoundedCornerShape(Corners.capsule)
val SheetShape = RoundedCornerShape(topStart = Corners.sheetTop, topEnd = Corners.sheetTop)
