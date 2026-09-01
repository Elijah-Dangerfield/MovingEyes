@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.EyeTier
import com.dangerfield.movingeyes.libraries.eyes.Mood

/**
 * Identifies a preset. The **name is not here** — it's copy, and this module is
 * pure Kotlin with no access to string resources, so the UI resolves the name
 * from this id. Same reason `EyeStyle.displayName` isn't what gets rendered.
 */
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
 * A starting arrangement, not a precision layout.
 *
 * Positions are hand-placed in normalised canvas coordinates and are meant to
 * be dragged: nobody's cardboard has holes where a preset guessed they'd be.
 * What a preset is actually for is picking the style, mood and colour
 * combination, which is the part that takes taste rather than a minute of
 * dragging.
 */
data class ScenePreset(
    val id: ScenePresetId,
    val eyes: List<SceneEye>,
    val canvasColor: Long = Scene.OpaqueBlack,
) {
    /**
     * True when the preset can't be used without the unlock, because it leans
     * on a paid style or a paid mood. Shown anyway, animating — a locked preset
     * that renders live is an advert; a greyed-out one is a nag.
     */
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

    /** The flagship. It's first because it's the one that explains the product. */
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

    /**
     * Eight eyes, and the reason the renderer was profiled at a count nobody
     * would place by hand. Deliberately uneven — a spider's eyes are not a grid.
     */
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

    /** Three pairs at different sizes, which reads as depth in a dark attic. */
    val AtticBats = ScenePreset(
        id = ScenePresetId.AtticBats,
        eyes = pair(EyeStyles.Bat, 0.28f, 0.30f, 0.20f, 0.15f, Mood.Suspicious) +
            pair(EyeStyles.Bat, 0.66f, 0.50f, 0.26f, 0.20f, Mood.Suspicious) +
            pair(EyeStyles.Bat, 0.40f, 0.76f, 0.15f, 0.11f, Mood.Suspicious),
    )

    /** Low and wide, because it's meant to sit in a planter and look up at a path. */
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

    /** Dormant on purpose. The whole effect is the contrast when it finally opens. */
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

    /**
     * Five pairs, staggered and varied. Glow Orb because this is the scene
     * meant to be seen from the street, and it's the style that survives the
     * trip through a window at night.
     */
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
     * What the app opens on before anything is saved: two Human eyes on the
     * free motion default.
     *
     * The plainest thing the app can show, deliberately. The first frame should
     * look like a real pair of eyes rather than a demo of the spookiest style
     * available — the product sells itself by working, not by showing off.
     */
    fun blank(): List<SceneEye> = pair(
        style = EyeStyles.HumanBasic,
        centerX = 0.5f,
        y = 0.5f,
        // 0.30 of the short edge, 1.35 eye widths apart. Anatomically a face is
        // nearer 2.6 eye widths, but that only reads right when the eyes are
        // small relative to the screen; at a size visible across a room a
        // tighter pair looks like a face and an anatomical one looks like two
        // separate things.
        separation = 0.405f,
        size = 0.30f,
        mood = Mood.IdleScan,
    )
}

/**
 * Two eyes either side of [centerX], [separation] apart in canvas widths.
 *
 * Pairs are their own helper because almost every scene is made of them, and
 * because a pair placed by hand tends to end up subtly asymmetric — which
 * reads, immediately and unmistakably, as wrong.
 */
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
    // The style's default glow is quoted at a 100px eye; as a fraction of the
    // eye's own diameter that's simply /100, and it then holds at any size.
    glowFraction = style.defaultGlow / 100f,
    mood = mood,
)
