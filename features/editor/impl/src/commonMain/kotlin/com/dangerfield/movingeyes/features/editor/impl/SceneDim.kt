package com.dangerfield.movingeyes.features.editor.impl

/**
 * How opaque the black dimming overlay is for a given scene brightness.
 *
 * A black overlay rather than the device's backlight. Brightness here is a
 * property of the scene, saved with it, and it has to behave like one: the
 * same scene has to look the same whatever the phone's own brightness is set
 * to, the editor has to show exactly what display mode will show, and leaving
 * display mode must leave the device as it was found. A backlight override
 * failed all three. It also could not reach the bottom of the range anyway —
 * the OS minimum is still too bright for an unlit hallway, which is why half
 * of this was an overlay to begin with.
 *
 * Linear, so perceived output stays proportional to the slider: an eye drawn
 * at full white under a 0.4 overlay reads as 60% as bright, which is what
 * "60%" on the slider should mean.
 */
fun dimOverlayAlpha(brightness: Float): Float = 1f - brightness.coerceIn(0f, 1f)
