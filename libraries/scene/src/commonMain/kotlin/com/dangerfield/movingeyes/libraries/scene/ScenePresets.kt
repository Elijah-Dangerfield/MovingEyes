@file:Suppress("MagicNumber", "LongMethod")

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
    BloodshotVigil,
    GhoulsStare,
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
 * **Depth is colour, not just size.** In the multi-pair scenes the far eyes are
 * smaller *and* darker *and* dimmer, because that's what distance does. Scaling
 * alone reads as small eyes, not distant ones.
 *
 * The exceptions prove it: Pumpkin Pals and The Doll's Room are deliberately
 * symmetric and level. A cartoon is a graphic and should look like one, and a
 * doll is unsettling *because* it's too perfect.
 */
object ScenePresets {

    /** The flagship, and the one that has to sell the product in a screenshot. */
    val PortraitHaunt = ScenePreset(
        id = ScenePresetId.PortraitHaunt,
        eyes = listOf(
            eye(
                style = EyeStyles.HumanRealistic,
                x = 0.33f, y = 0.455f, size = 0.255f, rotation = -2.5f,
                sclera = AgedSclera, iris = DeepBrown, veins = 0.35f,
                mood = Mood.Suspicious,
            ),
            // Fractionally smaller and a touch lower: the head is turned a few
            // degrees, which is the difference between a face and a symbol.
            eye(
                style = EyeStyles.HumanRealistic,
                x = 0.67f, y = 0.465f, size = 0.248f, rotation = -2.5f,
                sclera = AgedSclera, iris = DeepBrown, veins = 0.30f,
                mood = Mood.Suspicious,
            ),
        ),
    )

    /** Free, friendly, and deliberately graphic — a cartoon should look drawn. */
    val PumpkinPals = ScenePreset(
        id = ScenePresetId.PumpkinPals,
        eyes = symmetricPair(
            style = EyeStyles.CartoonRound,
            y = 0.5f, separation = 0.40f, size = 0.30f,
            sclera = 0xFFFFFBF2, iris = PumpkinOrange, glow = 0.05f,
            mood = Mood.IdleScan,
        ),
    )

    /**
     * The real jumping-spider arrangement: two huge forward-facing eyes, a
     * smaller lateral pair, and four little ones set back. A ring of eight
     * evenly spaced dots is what people draw from memory and it looks like a
     * pattern; this looks like an animal.
     */
    val SpiderNest = ScenePreset(
        id = ScenePresetId.SpiderNest,
        eyes = symmetricPair(
            style = EyeStyles.Spider, y = 0.545f, separation = 0.165f, size = 0.150f,
            sclera = SpiderShell, iris = SpiderIris, glow = 0.04f, mood = Mood.Frantic,
        ) + symmetricPair(
            style = EyeStyles.Spider, y = 0.515f, separation = 0.365f, size = 0.098f,
            sclera = SpiderShell, iris = SpiderIris, glow = 0.03f, mood = Mood.Frantic,
        ) + symmetricPair(
            style = EyeStyles.Spider, y = 0.435f, separation = 0.300f, size = 0.052f,
            sclera = 0xFF0A090B, iris = 0xFF121016, mood = Mood.Frantic,
        ) + symmetricPair(
            style = EyeStyles.Spider, y = 0.425f, separation = 0.455f, size = 0.044f,
            sclera = 0xFF0A090B, iris = 0xFF121016, mood = Mood.Frantic,
        ),
    )

    /** Three pairs receding into the dark. Each one further back is smaller,
     *  darker and dimmer, which is what distance actually does. */
    val AtticBats = ScenePreset(
        id = ScenePresetId.AtticBats,
        eyes = tiltedPair(
            style = EyeStyles.Bat, centerX = 0.30f, y = 0.30f, separation = 0.20f,
            size = 0.150f, tilt = -4f, sclera = 0xFF2A1A14, iris = 0xFFD8452C,
            glow = 0.15f, mood = Mood.Suspicious,
        ) + tiltedPair(
            style = EyeStyles.Bat, centerX = 0.68f, y = 0.50f, separation = 0.145f,
            size = 0.108f, tilt = 3f, sclera = 0xFF1F130F, iris = 0xFFB33C26,
            glow = 0.11f, mood = Mood.Suspicious,
        ) + tiltedPair(
            style = EyeStyles.Bat, centerX = 0.42f, y = 0.74f, separation = 0.100f,
            size = 0.074f, tilt = -2f, sclera = 0xFF160D0A, iris = 0xFF8A2F1E,
            glow = 0.07f, mood = Mood.Suspicious,
        ),
    )

    /** Low and wide: it sits in a planter and looks up at a path. */
    val CatInTheBushes = ScenePreset(
        id = ScenePresetId.CatInTheBushes,
        eyes = tiltedPair(
            style = EyeStyles.Feline, centerX = 0.5f, y = 0.665f, separation = 0.42f,
            size = 0.235f, tilt = 3.5f, sclera = 0xFFE6DCBE, iris = CatYellow,
            glow = 0.06f, mood = Mood.Suspicious,
        ),
    )

    /** Dormant on purpose: the effect is the contrast when it opens. The glow
     *  is heavy so it reads as embers even while the lids are down. */
    val DemonAwakens = ScenePreset(
        id = ScenePresetId.DemonAwakens,
        eyes = tiltedPair(
            style = EyeStyles.Demon, centerX = 0.5f, y = 0.495f, separation = 0.365f,
            size = 0.275f, tilt = -1.5f, sclera = 0xFF35120A, iris = 0xFFFF5A12,
            glow = 0.26f, mood = Mood.Dormant,
        ),
    )

    /** Five pairs at five depths. Glow Orb because this is the one meant to be
     *  seen from the street, and it's the style that survives a window. */
    val WindowWatchers = ScenePreset(
        id = ScenePresetId.WindowWatchers,
        eyes = depthPair(0.22f, 0.26f, separation = 0.115f, depth = 0f) +
            depthPair(0.74f, 0.38f, separation = 0.098f, depth = 0.25f) +
            depthPair(0.40f, 0.55f, separation = 0.128f, depth = 0.1f) +
            depthPair(0.79f, 0.71f, separation = 0.082f, depth = 0.6f) +
            depthPair(0.24f, 0.80f, separation = 0.070f, depth = 0.85f),
    )

    /** Symmetric and level on purpose. A doll is unsettling *because* it is
     *  too perfect, so every trick used to make the others feel alive is
     *  deliberately withheld here. */
    val DollsRoom = ScenePreset(
        id = ScenePresetId.DollsRoom,
        eyes = symmetricPair(
            style = EyeStyles.Doll, y = 0.48f, separation = 0.345f, size = 0.265f,
            sclera = Porcelain, iris = DollBlue, mood = Mood.Dormant,
        ),
    )

    /** Someone who has not slept. The veins carry this one, so they run near
     *  full and the lids sit heavy. */
    val BloodshotVigil = ScenePreset(
        id = ScenePresetId.BloodshotVigil,
        eyes = listOf(
            eye(
                style = EyeStyles.Bloodshot,
                x = 0.325f, y = 0.505f, size = 0.268f, rotation = 1.5f,
                sclera = 0xFFEFDBD3, iris = 0xFF7A5030, veins = 0.95f,
                mood = Mood.Sleepy,
            ),
            eye(
                style = EyeStyles.Bloodshot,
                x = 0.675f, y = 0.495f, size = 0.262f, rotation = 1.5f,
                sclera = 0xFFEDD8CF, iris = 0xFF6E4729, veins = 0.85f,
                mood = Mood.Sleepy,
            ),
        ),
    )

    /** Cataracts. The style already flattens the iris gradient so it can't
     *  appear to focus; the colours here keep it just this side of dead. */
    val GhoulsStare = ScenePreset(
        id = ScenePresetId.GhoulsStare,
        eyes = tiltedPair(
            style = EyeStyles.Ghoul, centerX = 0.5f, y = 0.47f, separation = 0.33f,
            size = 0.275f, tilt = -3f, sclera = 0xFFC9C0AE, iris = 0xFFA9B3A4,
            veins = 0.6f, mood = Mood.Suspicious,
        ),
    )

    val All: List<ScenePreset> = listOf(
        PortraitHaunt,
        PumpkinPals,
        DemonAwakens,
        CatInTheBushes,
        AtticBats,
        SpiderNest,
        WindowWatchers,
        DollsRoom,
        BloodshotVigil,
        GhoulsStare,
    )

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
private const val AgedSclera = 0xFFEBE3D5
private const val Porcelain = 0xFFFDF8F0

private const val WarmBrown = 0xFF6B4A2B
private const val DeepBrown = 0xFF553C24
private const val DollBlue = 0xFF5C7EA8
private const val CatYellow = 0xFFC2B23A
private const val PumpkinOrange = 0xFFE8721C

private const val SpiderShell = 0xFF0E0C10
private const val SpiderIris = 0xFF17131C

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
