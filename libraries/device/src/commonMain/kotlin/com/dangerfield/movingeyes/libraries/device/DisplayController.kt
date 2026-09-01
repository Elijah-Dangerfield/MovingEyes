package com.dangerfield.movingeyes.libraries.device

/**
 * The window while display mode runs. One interface because these are always
 * flipped together on entering and leaving, and leaving one stuck on is the
 * failure that matters.
 *
 * Every call is best-effort; a platform that can't honour one ignores it.
 */
interface DisplayController {

    fun setKeepAwake(keepAwake: Boolean)

    fun setImmersive(immersive: Boolean)

    /** 0..1, or null to hand control back to the system. Only the upper part
     *  of the range — see [dimLevelsFor]. */
    fun setBrightness(brightness: Float?)

    /**
     * Freezes the orientation currently on screen. Locked when display mode
     * starts and not before: while editing, rotation should follow the device.
     */
    fun setOrientationLocked(locked: Boolean)
}

data class DimLevels(val osBrightness: Float, val overlayAlpha: Float)

/**
 * Below this the backlight stops going darker and starts banding, and it is
 * still too bright for an unlit hallway.
 */
const val SoftwareDimBelow = 0.25f

/**
 * Splits [brightness] between the backlight and a black overlay so a scene can
 * go dimmer than the OS minimum. Perceived output stays proportional to
 * [brightness] across the join, so the slider doesn't change gear at the
 * handover.
 */
fun dimLevelsFor(brightness: Float): DimLevels {
    val level = brightness.coerceIn(0f, 1f)
    if (level >= SoftwareDimBelow) return DimLevels(osBrightness = level, overlayAlpha = 0f)
    return DimLevels(
        osBrightness = SoftwareDimBelow,
        overlayAlpha = 1f - level / SoftwareDimBelow,
    )
}
