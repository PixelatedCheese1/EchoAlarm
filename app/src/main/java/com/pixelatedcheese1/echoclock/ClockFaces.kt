package com.pixelatedcheese1.echoclock

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

enum class FaceKind { Digital, Stacked, Analog }

data class ClockFace(
    val id: String,
    val name: String,
    val kind: FaceKind,
    val font: FontFamily = FontFamily.Default,
    val weight: FontWeight = FontWeight.Normal
)

/**
 * ===== ADDING YOUR OWN FONTS =====
 * 1. Copy the .ttf/.otf into app/src/main/res/font/ (lowercase_with_underscores.ttf).
 * 2. Add a line below, e.g.:
 *
 *    ClockFace("neon", "Neon", FaceKind.Digital, FontFamily(Font(R.font.my_neon_font))),
 *    ClockFace("neon_stack", "Neon Stacked", FaceKind.Stacked, FontFamily(Font(R.font.my_neon_font))),
 *
 * (import androidx.compose.ui.text.font.Font). Every face shows up in the long-press picker.
 * Digital = one line, Stacked = hours over minutes, Analog = drawn dial (font unused).
 */
object ClockFaces {
    val all: List<ClockFace> = listOf(
        ClockFace("big", "Big", FaceKind.Digital, FontFamily.SansSerif, FontWeight.Normal),
        ClockFace("thin", "Thin", FaceKind.Digital, FontFamily.SansSerif, FontWeight.Thin),
        ClockFace("mono", "Mono", FaceKind.Digital, FontFamily.Monospace, FontWeight.Medium),
        ClockFace("serif", "Serif", FaceKind.Digital, FontFamily.Serif, FontWeight.Bold),
        ClockFace("stacked", "Stacked", FaceKind.Stacked, FontFamily.SansSerif, FontWeight.Black),
        ClockFace("analog", "Analog", FaceKind.Analog),
        // <- add your custom-font faces here
    )

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}
