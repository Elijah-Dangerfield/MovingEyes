@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.eyes

import kotlinx.serialization.Serializable

/**
 * An eye style is a **parameter set**, never an image.
 *
 * Everything is drawn from these numbers, which is what makes any style
 * recolourable to anything, crisp at any size, and cheap to add: a thirteenth
 * style is a row in [EyeStyles], not an art pipeline. It also keeps the binary
 * small enough that the whole app fits well inside the 40MB budget.
 *
 * The geometry ratios are lifted from the reference implementation in
 * `Eye.dc.html` so the shipped renderer and the design prototype agree. Don't
 * tune them independently of that file.
 */
@Serializable
data class EyeStyle(
    val id: EyeStyleId,
    val displayName: String,

    /** Height as a fraction of width. 1.0 is a circle; 0.6 is a human almond. */
    val aspectRatio: Float,

    /** Iris diameter as a fraction of eye width. */
    val irisRatio: Float,

    /** Pupil diameter as a fraction of the iris. Ignored when [slitPupil]. */
    val pupilRatio: Float,

    /**
     * A vertical slit rather than a round pupil — feline, reptile. Drawn at
     * 20% of the iris wide and 96% tall, so it reads as a hard vertical line
     * that still dilates believably.
     */
    val slitPupil: Boolean = false,

    /**
     * A horizontal bar pupil, the goat-eyed look. Mutually exclusive with
     * [slitPupil].
     */
    val barPupil: Boolean = false,

    /**
     * Sclera opacity. 1.0 is a normal white; 0.6 makes a spider's body barely
     * distinguishable from its iris; 0.0 is Glow Orb, which has no sclera at
     * all and is therefore the cheapest style in the app to draw — worth
     * knowing when a low-end tablet is struggling.
     */
    val scleraOpacity: Float = 1f,

    /** Specular highlight diameter as a fraction of eye width. The single
     *  cheapest thing that makes an eye look wet and alive. */
    val glintRatio: Float,

    /** Draws the red vein layer. Only meaningful on styles that have one. */
    val hasVeins: Boolean = false,

    /**
     * A cataract wash over the iris. Ghoul is unsettling precisely because it
     * can't focus, so its iris edge is deliberately soft.
     */
    val isMilky: Boolean = false,

    /** Free tier, or behind the unlock. */
    val tier: EyeTier = EyeTier.Paid,

    /** Sensible starting colours. Every one is user-overridable. */
    val defaultSclera: Long = 0xFFF1EBE0,
    val defaultIris: Long = 0xFFD8963A,
    val defaultPupil: Long = 0xFF000000,

    /** Default bloom radius in px at a 100px eye. Scales with size. */
    val defaultGlow: Float = 0f,
)

@Serializable
enum class EyeTier { Free, Paid }

@Serializable
enum class EyeStyleId {
    CartoonRound,
    HumanBasic,
    GlowOrb,
    HumanRealistic,
    Bloodshot,
    Bat,
    Feline,
    Demon,
    Spider,
    Reptile,
    Ghoul,
    Doll,
}

/**
 * The twelve launch styles.
 *
 * The free three are chosen to be genuinely sufficient, not a teaser: someone
 * must be able to complete the taped-behind-a-painting trick for free and love
 * it, because that is the review that sells the next hundred copies. Glow Orb
 * in particular is free *because* it's the best-performing style for the
 * bushes and window cases, which are the ones that photograph well.
 */
object EyeStyles {

    val CartoonRound = EyeStyle(
        id = EyeStyleId.CartoonRound,
        displayName = "Cartoon",
        aspectRatio = 1.00f,
        irisRatio = 0.58f,
        pupilRatio = 0.50f,
        glintRatio = 0.26f,
        tier = EyeTier.Free,
        defaultSclera = 0xFFF6F2EA,
        defaultIris = 0xFF4FA3C7,
    )

    val HumanBasic = EyeStyle(
        id = EyeStyleId.HumanBasic,
        displayName = "Human",
        aspectRatio = 0.60f,
        irisRatio = 0.44f,
        pupilRatio = 0.44f,
        glintRatio = 0.20f,
        tier = EyeTier.Free,
        defaultIris = 0xFF7A5A34,
    )

    /**
     * No sclera at all, just a glowing iris. Costs almost nothing to render
     * and is the style that actually works from the street.
     */
    val GlowOrb = EyeStyle(
        id = EyeStyleId.GlowOrb,
        displayName = "Glow Orb",
        aspectRatio = 1.00f,
        irisRatio = 1.00f,
        pupilRatio = 0.34f,
        scleraOpacity = 0f,
        glintRatio = 0.18f,
        tier = EyeTier.Free,
        defaultIris = 0xFFE0533A,
        defaultGlow = 22f,
    )

    val HumanRealistic = EyeStyle(
        id = EyeStyleId.HumanRealistic,
        displayName = "Realistic",
        aspectRatio = 0.58f,
        irisRatio = 0.42f,
        pupilRatio = 0.42f,
        glintRatio = 0.16f,
        hasVeins = true,
        defaultIris = 0xFF6B5136,
    )

    val Bloodshot = EyeStyle(
        id = EyeStyleId.Bloodshot,
        displayName = "Bloodshot",
        aspectRatio = 0.62f,
        irisRatio = 0.44f,
        pupilRatio = 0.46f,
        glintRatio = 0.18f,
        hasVeins = true,
        defaultSclera = 0xFFEFD9D2,
        defaultIris = 0xFF8A5A3A,
    )

    val Bat = EyeStyle(
        id = EyeStyleId.Bat,
        displayName = "Bat",
        aspectRatio = 0.70f,
        irisRatio = 0.54f,
        pupilRatio = 0.40f,
        scleraOpacity = 0.9f,
        glintRatio = 0.16f,
        defaultSclera = 0xFF2A1A16,
        defaultIris = 0xFFE0533A,
        defaultGlow = 10f,
    )

    val Feline = EyeStyle(
        id = EyeStyleId.Feline,
        displayName = "Feline",
        aspectRatio = 0.88f,
        irisRatio = 0.76f,
        pupilRatio = 0.30f,
        slitPupil = true,
        glintRatio = 0.18f,
        defaultSclera = 0xFFF0E9D8,
        defaultIris = 0xFFC8D24A,
        defaultGlow = 6f,
    )

    val Demon = EyeStyle(
        id = EyeStyleId.Demon,
        displayName = "Demon",
        aspectRatio = 0.72f,
        irisRatio = 0.82f,
        pupilRatio = 0.30f,
        barPupil = true,
        scleraOpacity = 0.45f,
        glintRatio = 0.12f,
        defaultSclera = 0xFF3A1A0E,
        defaultIris = 0xFFF07A1E,
        defaultGlow = 18f,
    )

    val Spider = EyeStyle(
        id = EyeStyleId.Spider,
        displayName = "Spider",
        aspectRatio = 0.96f,
        irisRatio = 0.94f,
        pupilRatio = 0.52f,
        scleraOpacity = 0.6f,
        glintRatio = 0.30f,
        defaultSclera = 0xFF0E0C0E,
        defaultIris = 0xFF141014,
        defaultGlow = 4f,
    )

    val Reptile = EyeStyle(
        id = EyeStyleId.Reptile,
        displayName = "Reptile",
        aspectRatio = 0.66f,
        irisRatio = 0.70f,
        pupilRatio = 0.28f,
        slitPupil = true,
        glintRatio = 0.14f,
        defaultSclera = 0xFFD8C89A,
        defaultIris = 0xFFB8A63C,
    )

    val Ghoul = EyeStyle(
        id = EyeStyleId.Ghoul,
        displayName = "Ghoul",
        aspectRatio = 0.78f,
        irisRatio = 0.40f,
        pupilRatio = 0.30f,
        glintRatio = 0.12f,
        isMilky = true,
        defaultSclera = 0xFFCFC9BE,
        defaultIris = 0xFF9FB0A8,
    )

    val Doll = EyeStyle(
        id = EyeStyleId.Doll,
        displayName = "Doll",
        aspectRatio = 0.82f,
        irisRatio = 0.62f,
        pupilRatio = 0.48f,
        glintRatio = 0.34f,
        defaultSclera = 0xFFFBF6EE,
        defaultIris = 0xFF4A6FA5,
    )

    val All: List<EyeStyle> = listOf(
        CartoonRound,
        HumanBasic,
        GlowOrb,
        HumanRealistic,
        Bloodshot,
        Bat,
        Feline,
        Demon,
        Spider,
        Reptile,
        Ghoul,
        Doll,
    )

    val Free: List<EyeStyle> = All.filter { it.tier == EyeTier.Free }

    fun byId(id: EyeStyleId): EyeStyle = All.first { it.id == id }
}
