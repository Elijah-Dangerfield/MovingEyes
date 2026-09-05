@file:Suppress("MagicNumber", "LongMethod")

package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.EyeTier
import com.dangerfield.movingeyes.libraries.eyes.Mood

/** Names live in the string catalogue; this module is pure Kotlin. */
enum class ScenePresetId {
    WallOfEyes,
    Trapped,
    PumpkinPals,
}

/**
 * A composed scene, not a default arrangement.
 *
 * Positions still get dragged — nobody's cardboard has holes where a preset
 * guessed — but the colour, the tilt and the depth are the part that takes
 * taste, and those survive being moved.
 */
data class ScenePreset(
    val id: ScenePresetId,
    val eyes: List<SceneEye>,
    val canvasColor: Long = Scene.OpaqueBlack,
) {
    /** Shown anyway, animating: a live locked preset is an advert, a greyed
     *  one is a nag. */
    val isPaid: Boolean = eyes.any { eye ->
        EyeStyles.byId(eye.styleId).tier == EyeTier.Paid || eye.mood != Mood.IdleScan
    }

    fun toScene(id: String, name: String) = Scene(
        id = id,
        name = name,
        eyes = eyes,
        canvasColor = canvasColor,
    )
}

/**
 * Three, and every one of them free.
 *
 * A preset is a demonstration, not a catalogue. Ten of them made the drawer a
 * list to get through, and a locked one is a scene you can look at and not
 * use — which is the worst version of a paywall. These three each show a
 * different thing the app can do (a crowd, a face, a graphic), all with free
 * styles and the free motion default, so the trick works before anyone pays.
 * The unlock is styles, moods and sound, which is where v2 puts it.
 *
 * ## What makes these read as real rather than as graphics
 *
 * **Nothing is pure white.** A real sclera is warm off-white and picks up the
 * light around it; `FFFFFF` is the single loudest tell that an eye was drawn
 * rather than photographed.
 *
 * **Nothing living is perfectly level.** A pair sitting at identical heights,
 * identical sizes, zero degrees reads as a logo. A degree or two of tilt, a
 * percent of size difference and a hair of vertical offset is the whole
 * difference between "two ovals" and "someone is behind that painting".
 *
 * **Depth is colour, not just size.** In [WallOfEyes] the far eyes are smaller
 * *and* darker *and* dimmer, because that's what distance does. Scaling alone
 * reads as small eyes, not distant ones.
 *
 * Pumpkin Pals is the deliberate exception: a cartoon is a graphic and should
 * look like one, so it stays exactly symmetric and level.
 */
object ScenePresets {

    /**
     * A crowd in the dark, and the one that has to sell the app in a
     * screenshot. Seven pairs at seven distances: the near ones are large,
     * warm and low in frame; the far ones are small, dim, cooler and high,
     * because that is what a room full of things looking at you does.
     */
    val WallOfEyes = ScenePreset(
        id = ScenePresetId.WallOfEyes,
        eyes = listOf(
            depthPair(centerX = 0.24f, y = 0.74f, separation = 0.150f, depth = 0.04f),
            depthPair(centerX = 0.71f, y = 0.80f, separation = 0.134f, depth = 0.19f),
            depthPair(centerX = 0.46f, y = 0.59f, separation = 0.112f, depth = 0.38f),
            depthPair(centerX = 0.15f, y = 0.45f, separation = 0.097f, depth = 0.53f),
            depthPair(centerX = 0.83f, y = 0.50f, separation = 0.087f, depth = 0.62f),
            depthPair(centerX = 0.35f, y = 0.29f, separation = 0.071f, depth = 0.79f),
            depthPair(centerX = 0.64f, y = 0.21f, separation = 0.059f, depth = 0.91f),
        ).flatten(),
    )

    /**
     * The opposite composition: one face, close, low, and too near the glass.
     *
     * The pair sits tighter than anatomy would put it and low in the frame, so
     * it reads as something looking *up* out of a gap rather than a portrait
     * looking straight ahead.
     */
    val Trapped = ScenePreset(
        id = ScenePresetId.Trapped,
        eyes = tiltedPair(
            style = EyeStyles.HumanBasic,
            centerX = 0.5f, y = 0.63f, separation = 0.285f, size = 0.255f, tilt = -2f,
            sclera = LivingSclera, iris = WarmBrown, veins = 0.55f,
        ),
    )

    /** Friendly, and deliberately graphic — a cartoon should look drawn. */
    val PumpkinPals = ScenePreset(
        id = ScenePresetId.PumpkinPals,
        eyes = symmetricPair(
            style = EyeStyles.CartoonRound,
            y = 0.5f, separation = 0.40f, size = 0.30f,
            sclera = 0xFFFFFBF2, iris = PumpkinOrange, glow = 0.05f,
            mood = Mood.IdleScan,
        ),
    )

    val All: List<ScenePreset> = listOf(WallOfEyes, Trapped, PumpkinPals)

    fun byId(id: ScenePresetId): ScenePreset = All.first { it.id == id }

    /**
     * What the app opens on before anything is saved. The plainest thing it can
     * show: a real pair of eyes, not a demo of the spookiest style available.
     */
    fun blank(): List<SceneEye> = listOf(
        eye(
            style = EyeStyles.HumanBasic,
            // 0.405 apart is 1.35 eye widths. A face is anatomically nearer
            // 2.6, but at a size visible across a room a tighter pair reads as
            // a face and an anatomical one reads as two separate things.
            x = 0.5f - 0.2025f, y = 0.4975f, size = 0.30f, rotation = -1.5f,
            sclera = LivingSclera, iris = WarmBrown,
        ),
        eye(
            style = EyeStyles.HumanBasic,
            x = 0.5f + 0.2025f, y = 0.5025f, size = 0.294f, rotation = -1.5f,
            sclera = LivingSclera, iris = WarmBrown,
        ),
    )
}

// ---- Palette ------------------------------------------------------------
// Real irises, and sclerae that are never pure white.

private const val LivingSclera = 0xFFF1EBE0

private const val WarmBrown = 0xFF6B4A2B
private const val PumpkinOrange = 0xFFE8721C


// ---- Builders -----------------------------------------------------------

private fun eye(
    style: EyeStyle,
    x: Float,
    y: Float,
    size: Float,
    rotation: Float = 0f,
    sclera: Long = style.defaultSclera,
    iris: Long = style.defaultIris,
    glow: Float = style.defaultGlow / 100f,
    veins: Float = 0.5f,
    mood: Mood = Mood.IdleScan,
): SceneEye = SceneEye(
    styleId = style.id,
    x = x,
    y = y,
    sizeFraction = size,
    rotationDegrees = rotation,
    scleraColor = sclera,
    irisColor = iris,
    pupilColor = style.defaultPupil,
    glowFraction = glow,
    veinIntensity = veins,
    mood = mood,
)

/** Level, identical, mirrored. For the two scenes where being graphic is the
 *  point; everything else should use [tiltedPair]. */
private fun symmetricPair(
    style: EyeStyle,
    y: Float,
    separation: Float,
    size: Float,
    centerX: Float = 0.5f,
    sclera: Long = style.defaultSclera,
    iris: Long = style.defaultIris,
    glow: Float = style.defaultGlow / 100f,
    mood: Mood = Mood.IdleScan,
): List<SceneEye> = listOf(-1, 1).map { side ->
    eye(style, centerX + side * separation / 2f, y, size, 0f, sclera, iris, glow, 0.5f, mood)
}

/**
 * A pair on a tilted head: both eyes turn together, the far one sits slightly
 * lower and smaller. The numbers are small on purpose — enough to stop it
 * reading as a symbol, not enough to look like a mistake.
 */
private fun tiltedPair(
    style: EyeStyle,
    centerX: Float,
    y: Float,
    separation: Float,
    size: Float,
    tilt: Float,
    sclera: Long = style.defaultSclera,
    iris: Long = style.defaultIris,
    glow: Float = style.defaultGlow / 100f,
    veins: Float = 0.5f,
    mood: Mood = Mood.IdleScan,
): List<SceneEye> {
    val drop = separation * kotlin.math.sin(tilt * kotlin.math.PI.toFloat() / 180f) / 2f
    return listOf(
        eye(style, centerX - separation / 2f, y - drop, size, tilt, sclera, iris, glow, veins, mood),
        eye(style, centerX + separation / 2f, y + drop, size * 0.972f, tilt, sclera, iris, glow, veins * 0.9f, mood),
    )
}

/**
 * A pair at [depth] 0 (near) to 1 (far). Distance takes size, brightness and
 * glow together — shrinking alone reads as small eyes rather than distant ones.
 */
private fun depthPair(
    centerX: Float,
    y: Float,
    separation: Float,
    depth: Float,
): List<SceneEye> {
    val near = 1f - depth
    val size = 0.048f + 0.062f * near
    val iris = lerpColor(0xFF9A4433, 0xFFE85A33, near)
    return symmetricPair(
        style = EyeStyles.GlowOrb,
        y = y,
        separation = separation,
        size = size,
        centerX = centerX,
        sclera = 0xFF000000,
        iris = iris,
        glow = 0.14f + 0.16f * near,
        mood = Mood.IdleScan,
    )
}

private fun lerpColor(from: Long, to: Long, fraction: Float): Long {
    fun channel(shift: Int): Long {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * fraction).toLong().coerceIn(0, 255) shl shift
    }
    return 0xFF000000L or channel(16) or channel(8) or channel(0)
}
