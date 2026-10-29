package com.dangerfield.movingeyes.libraries.device

/**
 * The window while display mode runs. One interface because these are always
 * flipped together on entering and leaving, and leaving one stuck on is the
 * failure that matters.
 *
 * Nothing here touches screen brightness. It used to, and it was wrong twice
 * over: the scene's own brightness slider defaulted to 100%, so hitting play
 * drove the backlight to full on a device the user had deliberately turned
 * down, and on iOS `UIScreen.brightness` is a global the change outlives the
 * app. Dimming is a black overlay now — see `dimOverlayAlpha` — which the user
 * can see while editing and which never reaches outside this app.
 *
 * Every call is best-effort; a platform that can't honour one ignores it.
 */
interface DisplayController {

    fun setKeepAwake(keepAwake: Boolean)

    fun setImmersive(immersive: Boolean)

    /**
     * Freezes the orientation currently on screen. Locked when display mode
     * starts and not before: while editing, rotation should follow the device.
     */
    fun setOrientationLocked(locked: Boolean)
}
