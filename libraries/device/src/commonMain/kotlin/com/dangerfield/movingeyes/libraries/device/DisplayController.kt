package com.dangerfield.movingeyes.libraries.device

/**
 * The window, while the appliance runs.
 *
 * One interface rather than four, because these are never touched
 * independently: entering display mode flips all of them on and leaving flips
 * them all back. Splitting them would mean four bindings per platform and four
 * chances to leave one of them stuck on after the user has walked away.
 *
 * Everything here is **best-effort**. A platform that can't honour a request
 * ignores it rather than throwing: a tablet that refuses to lock its rotation
 * is a worse decoration, not a broken app, and it must not take the canvas
 * down with it.
 */
interface DisplayController {

    /**
     * Stop the screen sleeping.
     *
     * The single most important call in this interface. A decoration that
     * blanks after thirty seconds is not a decoration, and it is the first
     * thing a reviewer notices.
     */
    fun setKeepAwake(keepAwake: Boolean)

    /**
     * Hide the system bars.
     *
     * Not cosmetic: the device is behind cardboard with two holes cut in it,
     * and a status bar is a bright strip of pixels leaking out of a gap that
     * was supposed to be black.
     */
    fun setImmersive(immersive: Boolean)

    /**
     * Set the window's brightness, 0..1, or null to hand control back to the
     * system.
     *
     * Only ever the *upper* part of the range — see [dimLevelsFor]. The OS
     * floor is far too bright for a dark hallway, so the last stretch is done
     * with a Compose overlay instead.
     */
    fun setBrightness(brightness: Float?)

    /**
     * Freeze the current orientation.
     *
     * Locked the instant display mode starts and not before. While editing,
     * rotation should follow the device; once the tablet is taped to
     * something, an accelerometer reading is noise and a rotation is a ruined
     * alignment.
     */
    fun setOrientationLocked(locked: Boolean)
}

/**
 * How to split a requested brightness between the OS and a black overlay.
 *
 * @param osBrightness what to hand [DisplayController.setBrightness]
 * @param overlayAlpha how opaque the black overlay on top of the canvas is
 */
data class DimLevels(val osBrightness: Float, val overlayAlpha: Float)

/**
 * Below this the OS backlight stops being useful — the steps get coarse, the
 * panel bands, and on most hardware it simply won't go darker. It is still far
 * brighter than a decoration in an unlit hallway wants to be.
 */
const val SoftwareDimBelow = 0.25f

/**
 * Split [brightness] between the backlight and a software overlay so the scene
 * can go dimmer than the OS minimum.
 *
 * This is the single trick that makes the whole thing look right in a dark
 * room, and it needs no platform code at all: above [SoftwareDimBelow] the
 * backlight does the work; below it the backlight holds at the threshold and a
 * black overlay takes over. Perceived output stays proportional to
 * [brightness] across the join, so dragging the slider doesn't visibly change
 * gear at the handover point.
 */
fun dimLevelsFor(brightness: Float): DimLevels {
    val level = brightness.coerceIn(0f, 1f)
    if (level >= SoftwareDimBelow) return DimLevels(osBrightness = level, overlayAlpha = 0f)
    return DimLevels(
        osBrightness = SoftwareDimBelow,
        overlayAlpha = 1f - level / SoftwareDimBelow,
    )
}
