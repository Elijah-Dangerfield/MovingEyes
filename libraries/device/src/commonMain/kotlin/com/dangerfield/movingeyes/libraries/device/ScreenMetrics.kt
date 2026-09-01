package com.dangerfield.movingeyes.libraries.device

/**
 * How many physical pixels make up a millimetre on this screen.
 *
 * Load-bearing for the whole product. The editor's readout says
 * `220 px / 46.6 mm`, and someone is going to hold a ruler against a piece of
 * cardboard and cut two holes based on that number. If it's wrong the eyes
 * don't line up, which is the one thing this app exists to get right.
 *
 * Android reads it from the display. **iOS has no public API for physical
 * screen size**, so [IosScreenMetrics] carries a model-identifier lookup
 * table; an unrecognised device falls back to a sane modern default rather
 * than refusing to show a measurement.
 */
interface ScreenMetrics {

    /**
     * Physical pixels per millimetre, or null when the platform genuinely
     * can't say.
     *
     * Null is not the same as a guess. The UI shows px only when this is null,
     * rather than printing a millimetre figure it can't stand behind — a
     * confidently wrong measurement is worse than an absent one when someone
     * is about to cut a hole in something.
     */
    val pixelsPerMillimeter: Float?

    /** True when this device's size was actually known, rather than assumed. */
    val isExact: Boolean
}

/** Millimetres for a pixel distance, or null when the screen size is unknown. */
fun ScreenMetrics.millimeters(pixels: Float): Float? =
    pixelsPerMillimeter?.let { pixels / it }
