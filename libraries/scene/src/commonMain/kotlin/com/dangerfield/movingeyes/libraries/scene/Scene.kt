package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeStyleId
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
import kotlinx.serialization.Serializable

/**
 * A saved composition: where the eyes are, what they look like, and how they
 * move.
 *
 * **Nothing in here is in pixels.** A scene is built on one device and opened
 * on another, or on the same device turned ninety degrees, and it has to
 * survive both — see [SceneEye.x] and [SceneEye.sizeFraction]. A scene that
 * stored absolute pixels would silently ruin an alignment the moment anything
 * about the display changed, which is the one failure this app cannot have.
 */
@Serializable
data class Scene(
    val id: String,
    val name: String,
    val eyes: List<SceneEye>,

    /**
     * Which way up the composition is drawn, independent of how the device is
     * held. A scene property rather than a device one because it's decided by
     * where the charge cable has to exit the cardboard.
     */
    val canvasRotation: CanvasRotation = CanvasRotation.None,

    /** Behind the eyes. Black on OLED means those pixels are genuinely off. */
    val canvasColor: Long = OpaqueBlack,

    /**
     * 0..1, applied on top of the OS brightness. Lets the scene go dimmer than
     * the system minimum, which is the difference between "a tablet in a hole"
     * and "eyes in the dark".
     */
    val brightness: Float = 1f,
) {
    companion object {
        const val OpaqueBlack: Long = 0xFF000000
    }
}

/**
 * One eye in a saved scene.
 *
 * The counterpart of `RenderedEye`, which is the same eye with its live state
 * machine attached and its measurements resolved to pixels for a particular
 * canvas.
 */
@Serializable
data class SceneEye(
    val styleId: EyeStyleId,

    /** 0..1 of canvas width. */
    val x: Float,

    /** 0..1 of canvas height. */
    val y: Float,

    /**
     * Diameter as a fraction of the canvas's **short edge**, not of either
     * axis.
     *
     * The short edge is the only measurement that doesn't change meaning when
     * a device is rotated, so this is what makes "keep sizes on rotate" fall
     * out for free rather than needing to be implemented. Fractions of width
     * would make every eye grow by the aspect ratio on turning to landscape.
     */
    val sizeFraction: Float,

    val rotationDegrees: Float = 0f,

    val scleraColor: Long,
    val irisColor: Long,
    val pupilColor: Long,

    /** Bloom radius as a fraction of the eye's own diameter, so glow tracks
     *  size instead of being swamped by a big eye or drowning a small one. */
    val glowFraction: Float = 0f,

    val veinIntensity: Float = 0.5f,

    val mood: Mood = Mood.IdleScan,

    /** Set only when [mood] is [Mood.Custom]; otherwise the mood names the
     *  config and storing it too would let the two disagree. */
    val customBehavior: BehaviorConfig? = null,
) {
    /** The motion this eye actually runs. */
    fun behavior(): BehaviorConfig =
        if (mood == Mood.Custom) customBehavior ?: Moods.FreeDefault else Moods.forMood(mood)
}

/**
 * Canvas turn, in ninety-degree steps.
 *
 * Only four positions exist on purpose. This is not a rotation control, it's a
 * mounting decision — the tablet goes into the cardboard whichever way the
 * charge port allows, and the scene turns to compensate. Anything between the
 * detents would be a mistake, not a choice.
 */
@Serializable
enum class CanvasRotation(val degrees: Int) {
    None(0),
    Quarter(90),
    Half(180),
    ThreeQuarter(270),
    ;

    fun next(): CanvasRotation = entries[(ordinal + 1) % entries.size]

    /** True when the turn swaps the canvas's width and height. */
    val swapsAxes: Boolean get() = this == Quarter || this == ThreeQuarter
}
