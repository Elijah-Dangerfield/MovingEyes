@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.EyeTier
import com.dangerfield.movingeyes.libraries.eyes.Mood

/** Names live in the string catalogue; this module is pure Kotlin. */
enum class ScenePresetId {
    PortraitHaunt,
    PumpkinPals,
    SpiderNest,
    AtticBats,
    CatInTheBushes,
    DemonAwakens,
    WindowWatchers,
    DollsRoom,
}

/**
 * A starting arrangement, not a precision layout — nobody's cardboard has holes
 * where a preset guessed. What it's for is the style, mood and colour
 * combination, which is the part that takes taste.
 */
data class ScenePreset(
    val id: ScenePresetId,
    val eyes: List<SceneEye>,
    val canvasColor: Long = Scene.OpaqueBlack,
) {
    /** Shown anyway, animating: a live locked preset is an advert, a greyed
     *  one is a nag. */
    val isPaid: Boolean = eyes.any { eye ->
        EyeStyles.byId(eye.styleId).tier == EyeTier.Paid || eye.mood !in FreeMoods
    }

    fun toScene(id: String, name: String) = Scene(
        id = id,
        name = name,
        eyes = eyes,
        canvasColor = canvasColor,
    )

    private companion object {
        val FreeMoods = setOf(Mood.IdleScan)
    }
}

object ScenePresets {

    /** First because it's the one that explains the product. */
    val PortraitHaunt = ScenePreset(
        id = ScenePresetId.PortraitHaunt,
        eyes = pair(
            style = EyeStyles.HumanRealistic,
            centerX = 0.5f,
            y = 0.46f,
            separation = 0.34f,
            size = 0.26f,
            mood = Mood.Suspicious,
        ),
    )

    val PumpkinPals = ScenePreset(
        id = ScenePresetId.PumpkinPals,
        eyes = pair(
            style = EyeStyles.CartoonRound,
            centerX = 0.5f,
            y = 0.5f,
            separation = 0.40f,
            size = 0.30f,
            mood = Mood.IdleScan,
            iris = 0xFFF07A1E,
        ),
    )

    /** Deliberately uneven: a spider's eyes are not a grid. */
    val SpiderNest = ScenePreset(
        id = ScenePresetId.SpiderNest,
        eyes = listOf(
            eye(EyeStyles.Spider, 0.42f, 0.40f, 0.11f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.58f, 0.40f, 0.11f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.34f, 0.50f, 0.08f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.66f, 0.50f, 0.08f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.45f, 0.55f, 0.07f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.55f, 0.55f, 0.07f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.38f, 0.62f, 0.05f, Mood.Frantic),
            eye(EyeStyles.Spider, 0.62f, 0.62f, 0.05f, Mood.Frantic),
        ),
    )

    /** Three pairs at different sizes, which reads as depth. */
    val AtticBats = ScenePreset(
        id = ScenePresetId.AtticBats,
        eyes = pair(EyeStyles.Bat, 0.28f, 0.30f, 0.20f, 0.15f, Mood.Suspicious) +
            pair(EyeStyles.Bat, 0.66f, 0.50f, 0.26f, 0.20f, Mood.Suspicious) +
            pair(EyeStyles.Bat, 0.40f, 0.76f, 0.15f, 0.11f, Mood.Suspicious),
    )

    /** Low and wide: it sits in a planter and looks up at a path. */
    val CatInTheBushes = ScenePreset(
        id = ScenePresetId.CatInTheBushes,
        eyes = pair(
            style = EyeStyles.Feline,
            centerX = 0.5f,
            y = 0.68f,
            separation = 0.42f,
            size = 0.27f,
            mood = Mood.Suspicious,
        ),
    )

    /** Dormant on purpose: the effect is the contrast when it opens. */
    val DemonAwakens = ScenePreset(
        id = ScenePresetId.DemonAwakens,
        eyes = pair(
            style = EyeStyles.Demon,
            centerX = 0.5f,
            y = 0.5f,
            separation = 0.38f,
            size = 0.29f,
            mood = Mood.Dormant,
        ),
    )

    /** Glow Orb because this one is meant to be seen from the street. */
    val WindowWatchers = ScenePreset(
        id = ScenePresetId.WindowWatchers,
        eyes = pair(EyeStyles.GlowOrb, 0.20f, 0.24f, 0.10f, 0.07f, Mood.IdleScan) +
            pair(EyeStyles.GlowOrb, 0.72f, 0.34f, 0.13f, 0.09f, Mood.IdleScan) +
            pair(EyeStyles.GlowOrb, 0.38f, 0.52f, 0.16f, 0.11f, Mood.IdleScan) +
            pair(EyeStyles.GlowOrb, 0.78f, 0.68f, 0.11f, 0.08f, Mood.IdleScan) +
            pair(EyeStyles.GlowOrb, 0.26f, 0.80f, 0.14f, 0.10f, Mood.IdleScan),
    )

    val DollsRoom = ScenePreset(
        id = ScenePresetId.DollsRoom,
        eyes = pair(
            style = EyeStyles.Doll,
            centerX = 0.5f,
            y = 0.48f,
            separation = 0.34f,
            size = 0.27f,
            mood = Mood.Dormant,
        ),
    )

    val All: List<ScenePreset> = listOf(
        PortraitHaunt,
        PumpkinPals,
        SpiderNest,
        AtticBats,
        CatInTheBushes,
        DemonAwakens,
        WindowWatchers,
        DollsRoom,
    )

    fun byId(id: ScenePresetId): ScenePreset = All.first { it.id == id }

    /**
     * The plainest thing the app can show. The first frame should look like a
     * real pair of eyes, not a demo of the spookiest style available.
     */
    fun blank(): List<SceneEye> = pair(
        style = EyeStyles.HumanBasic,
        centerX = 0.5f,
        y = 0.5f,
        // 1.35 eye widths apart. A face is anatomically nearer 2.6, but at a
        // size visible across a room a tighter pair reads as a face and an
        // anatomical one reads as two separate things.
        separation = 0.405f,
        size = 0.30f,
        mood = Mood.IdleScan,
    )
}

/** [separation] is in canvas widths. A hand-placed pair ends up subtly
 *  asymmetric, which reads immediately as wrong. */
private fun pair(
    style: EyeStyle,
    centerX: Float,
    y: Float,
    separation: Float,
    size: Float,
    mood: Mood,
    iris: Long? = null,
): List<SceneEye> = listOf(
    eye(style, centerX - separation / 2f, y, size, mood, iris),
    eye(style, centerX + separation / 2f, y, size, mood, iris),
)

private fun eye(
    style: EyeStyle,
    x: Float,
    y: Float,
    size: Float,
    mood: Mood,
    iris: Long? = null,
): SceneEye = SceneEye(
    styleId = style.id,
    x = x,
    y = y,
    sizeFraction = size,
    scleraColor = style.defaultSclera,
    irisColor = iris ?: style.defaultIris,
    pupilColor = style.defaultPupil,
    // defaultGlow is quoted at a 100px eye.
    glowFraction = style.defaultGlow / 100f,
    mood = mood,
)
