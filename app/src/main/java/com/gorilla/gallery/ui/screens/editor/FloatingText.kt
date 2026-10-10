package com.gorilla.gallery.ui.screens.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val CustomRobotoFamily = FontFamily(
    Font(com.gorilla.gallery.R.font.roboto_thin, FontWeight.Thin),
    Font(com.gorilla.gallery.R.font.roboto_regular, FontWeight.Normal),
    Font(com.gorilla.gallery.R.font.roboto_bold, FontWeight.Bold)
)

val CustomKalamFamily = FontFamily(
    Font(com.gorilla.gallery.R.font.kalam_light, FontWeight.Thin),
    Font(com.gorilla.gallery.R.font.kalam_regular, FontWeight.Normal),
    Font(com.gorilla.gallery.R.font.kalam_bold, FontWeight.Bold)
)

val CustomZillaSlabFamily = FontFamily(
    Font(com.gorilla.gallery.R.font.zillaslab_light, FontWeight.Thin),
    Font(com.gorilla.gallery.R.font.zillaslab_regular, FontWeight.Normal),
    Font(com.gorilla.gallery.R.font.zillaslab_bold, FontWeight.Bold)
)

data class FloatingText(
    val text: String,
    var offset: Offset,
    val color: Color,
    val scale: Float,
    val rotation: Float = 0f,
    val fontFamily: FontFamily = CustomRobotoFamily,
    val fontWeight: FontWeight = FontWeight.Normal
)
