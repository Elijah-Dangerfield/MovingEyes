package com.dangerfield.movingeyes.features.editor.impl

import com.dangerfield.movingeyes.libraries.billing.DemoControl
import com.dangerfield.movingeyes.libraries.eyes.EyeStyleId
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.scene.ScenePresetId
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.look_style
import movingeyes.libraries.resources.generated.resources.mood_custom
import movingeyes.libraries.resources.generated.resources.mood_dormant
import movingeyes.libraries.resources.generated.resources.mood_frantic
import movingeyes.libraries.resources.generated.resources.mood_idle_scan
import movingeyes.libraries.resources.generated.resources.mood_possessed
import movingeyes.libraries.resources.generated.resources.mood_sleepy
import movingeyes.libraries.resources.generated.resources.mood_suspicious
import movingeyes.libraries.resources.generated.resources.motion_blink_rate
import movingeyes.libraries.resources.generated.resources.motion_gaze_center
import movingeyes.libraries.resources.generated.resources.motion_mood
import movingeyes.libraries.resources.generated.resources.motion_reactivity
import movingeyes.libraries.resources.generated.resources.motion_restlessness
import movingeyes.libraries.resources.generated.resources.motion_wander
import movingeyes.libraries.resources.generated.resources.preset_attic_bats
import movingeyes.libraries.resources.generated.resources.preset_cat_in_the_bushes
import movingeyes.libraries.resources.generated.resources.preset_demon_awakens
import movingeyes.libraries.resources.generated.resources.preset_dolls_room
import movingeyes.libraries.resources.generated.resources.preset_portrait_haunt
import movingeyes.libraries.resources.generated.resources.preset_pumpkin_pals
import movingeyes.libraries.resources.generated.resources.preset_spider_nest
import movingeyes.libraries.resources.generated.resources.preset_window_watchers
import movingeyes.libraries.resources.generated.resources.style_bat
import movingeyes.libraries.resources.generated.resources.style_bloodshot
import movingeyes.libraries.resources.generated.resources.style_cartoon
import movingeyes.libraries.resources.generated.resources.style_demon
import movingeyes.libraries.resources.generated.resources.style_doll
import movingeyes.libraries.resources.generated.resources.style_feline
import movingeyes.libraries.resources.generated.resources.style_ghoul
import movingeyes.libraries.resources.generated.resources.style_glow_orb
import movingeyes.libraries.resources.generated.resources.style_human
import movingeyes.libraries.resources.generated.resources.style_realistic
import movingeyes.libraries.resources.generated.resources.style_reptile
import movingeyes.libraries.resources.generated.resources.style_spider
import org.jetbrains.compose.resources.StringResource

/**
 * Names for things identified by an id in a pure-Kotlin module.
 *
 * `:libraries:eyes` and `:libraries:scene` have no Compose dependency and
 * therefore no access to string resources, so they carry ids and this file
 * turns each into copy. `EyeStyle.displayName` still exists but is a
 * developer-facing fallback for logs — it is deliberately never rendered.
 *
 * The `when`s are exhaustive on purpose: adding a style or a mood should fail
 * to compile here until it has a name, rather than shipping an enum constant to
 * a user.
 */
val EyeStyleId.label: StringResource
    get() = when (this) {
        EyeStyleId.CartoonRound -> Res.string.style_cartoon
        EyeStyleId.HumanBasic -> Res.string.style_human
        EyeStyleId.GlowOrb -> Res.string.style_glow_orb
        EyeStyleId.HumanRealistic -> Res.string.style_realistic
        EyeStyleId.Bloodshot -> Res.string.style_bloodshot
        EyeStyleId.Bat -> Res.string.style_bat
        EyeStyleId.Feline -> Res.string.style_feline
        EyeStyleId.Demon -> Res.string.style_demon
        EyeStyleId.Spider -> Res.string.style_spider
        EyeStyleId.Reptile -> Res.string.style_reptile
        EyeStyleId.Ghoul -> Res.string.style_ghoul
        EyeStyleId.Doll -> Res.string.style_doll
    }

val Mood.label: StringResource
    get() = when (this) {
        Mood.IdleScan -> Res.string.mood_idle_scan
        Mood.Suspicious -> Res.string.mood_suspicious
        Mood.Frantic -> Res.string.mood_frantic
        Mood.Sleepy -> Res.string.mood_sleepy
        Mood.Dormant -> Res.string.mood_dormant
        Mood.Possessed -> Res.string.mood_possessed
        Mood.Custom -> Res.string.mood_custom
    }

val ScenePresetId.label: StringResource
    get() = when (this) {
        ScenePresetId.PortraitHaunt -> Res.string.preset_portrait_haunt
        ScenePresetId.PumpkinPals -> Res.string.preset_pumpkin_pals
        ScenePresetId.SpiderNest -> Res.string.preset_spider_nest
        ScenePresetId.AtticBats -> Res.string.preset_attic_bats
        ScenePresetId.CatInTheBushes -> Res.string.preset_cat_in_the_bushes
        ScenePresetId.DemonAwakens -> Res.string.preset_demon_awakens
        ScenePresetId.WindowWatchers -> Res.string.preset_window_watchers
        ScenePresetId.DollsRoom -> Res.string.preset_dolls_room
    }

/** Used in the "Keep Frantic" bar, so it has to name the thing that just left. */
val DemoControl.label: StringResource
    get() = when (this) {
        DemoControl.Mood -> Res.string.motion_mood
        DemoControl.BlinkRate -> Res.string.motion_blink_rate
        DemoControl.WanderRadius -> Res.string.motion_wander
        DemoControl.Restlessness -> Res.string.motion_restlessness
        DemoControl.GazeCenter -> Res.string.motion_gaze_center
        DemoControl.EyeStyle -> Res.string.look_style
        DemoControl.Reactivity -> Res.string.motion_reactivity
    }
