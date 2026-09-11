package com.gorilla.gallery.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gorilla.gallery.ui.theme.LocalAppColors

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun GlassDateHeaderPill(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = LocalAppColors.current.textPrimary,
        modifier = modifier
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            .wrapContentWidth(Alignment.Start)
    )
}
