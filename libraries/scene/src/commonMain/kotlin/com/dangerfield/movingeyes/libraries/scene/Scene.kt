package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeStyleId
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
import kotlinx.serialization.Serializable

/**
 * A saved composition. Nothing here is in pixels: a scene is built on one
 * device and opened on another, or on the same device turned ninety degrees,
 * and it has to survive both.
 */
@Serializable
data class Scene(
    val id: String,
    val name: String,
    val eyes: List<SceneEye>,

    /** Decided by where the charge cable has to leave the cardboard. */
    val canvasRotation: CanvasRotation = CanvasRotation.None,

    val canvasColor: Long = OpaqueBlack,

    /** 0..1, applied on top of the OS brightness so the scene can go dimmer
     *  than the system minimum. */
    val brightness: Float = 1f,

    /** Minutes before the scene fades to black, or null to run all night. */
    val sleepTimerMinutes: Int? = null,
) {
    companion object {
        const val OpaqueBlack: Long = 0xFF000000
    }
}

@Serializable
data class SceneEye(
    val styleId: EyeStyleId,

    /** 0..1 of canvas width. */
    val x: Float,

    /** 0..1 of canvas height. */
    val y: Float,

    /**
     * Diameter as a fraction of the canvas's **short edge**, the one
     * measurement that doesn't change meaning when a device is rotated. This
     * is what makes "keep sizes on rotate" fall out rather than need building.
     */
    val sizeFraction: Float,

    val rotationDegrees: Float = 0f,

    val scleraColor: Long,
    val irisColor: Long,
    val pupilColor: Long,

    /** Fraction of the eye's own diameter, so glow tracks size. */
    val glowFraction: Float = 0f,

    val veinIntensity: Float = 0.5f,

    val mood: Mood = Mood.IdleScan,

    /** Set only for [Mood.Custom]; otherwise the mood names the config and
     *  storing both would let them disagree. */
    val customBehavior: BehaviorConfig? = null,
) {
    fun behavior(): BehaviorConfig =
        if (mood == Mood.Custom) customBehavior ?: Moods.FreeDefault else Moods.forMood(mood)
}

/** A mounting decision, not a rotation control, which is why it has four values. */
@Serializable
enum class CanvasRotation(val degrees: Int) {
    None(0),
    Quarter(90),
    Half(180),
    ThreeQuarter(270),
    ;

    fun next(): CanvasRotation = entries[(ordinal + 1) % entries.size]

    val swapsAxes: Boolean get() = this == Quarter || this == ThreeQuarter
}
