@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.ui.components.color

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @param hue 0..360, @param saturation 0..1, @param value 0..1
 *
 * Kept as its own type rather than round-tripping through [Color] on every
 * drag: converting back and forth loses the hue of a black or fully desaturated
 * colour, so a thumb dragged to the bottom of the panel would jump the hue
 * slider to red and never come back.
 */
data class Hsv(val hue: Float, val saturation: Float, val value: Float) {

    fun toColor(): Color = Color.hsv(
        hue = hue.mod(360f),
        saturation = saturation.coerceIn(0f, 1f),
        value = value.coerceIn(0f, 1f),
    )

    companion object {
        fun from(color: Color): Hsv {
            val r = color.red
            val g = color.green
            val b = color.blue

            val highest = max(r, max(g, b))
            val lowest = min(r, min(g, b))
            val range = highest - lowest

            val hue = when {
                range == 0f -> 0f
                highest == r -> 60f * (((g - b) / range).mod(6f))
                highest == g -> 60f * (((b - r) / range) + 2f)
                else -> 60f * (((r - g) / range) + 4f)
            }

            return Hsv(
                hue = if (hue < 0f) hue + 360f else hue,
                saturation = if (highest == 0f) 0f else range / highest,
                value = highest,
            )
        }
    }
}

/** Six-digit RRGGBB, no leading hash. Alpha is deliberately absent: an eye is
 *  opaque, and a translucent one reads as a rendering fault. */
fun Color.toHexDigits(): String {
    fun channel(component: Float): String {
        val byte = (component.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return byte.toString(16).uppercase().padStart(2, '0')
    }
    return channel(red) + channel(green) + channel(blue)
}

/** Null for anything that isn't six hex digits, so a half-typed value never
 *  repaints the canvas mid-keystroke. */
fun String.parseHexOrNull(): Color? {
    val digits = trim().removePrefix("#")
    if (digits.length != 6 || digits.any { !it.isHexDigit() }) return null
    return Color(
        red = digits.substring(0, 2).toInt(16) / 255f,
        green = digits.substring(2, 4).toInt(16) / 255f,
        blue = digits.substring(4, 6).toInt(16) / 255f,
    )
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun Float.mod(divisor: Float): Float {
    val remainder = this % divisor
    return if (remainder < 0f) remainder + divisor else remainder
}

internal fun Float.approximately(other: Float): Boolean = abs(this - other) < 0.002f
