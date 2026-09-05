package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.features.paywall.PaywallTrigger
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyePreview
import org.jetbrains.compose.resources.StringResource

/**
 * A paid thing the user just reached for, with everything the sheet needs to
 * show it running before asking anyone to pay for it.
 *
 * The preview *is* the trial. A style or a mood is a thing you look at, so a
 * live one at [PreviewSize] answers "what am I buying" better than thirty
 * seconds of owning it and then having it taken back.
 */
sealed interface LockedItem {

    val label: StringResource
    val trigger: PaywallTrigger

    @Composable
    fun Preview()

    data class Style(private val style: EyeStyle) : LockedItem {
        override val label get() = style.id.label
        override val trigger get() = PaywallTrigger.EyeStyle

        @Composable
        override fun Preview() {
            EyePreview(style = style, sizeDp = PreviewSize, behavior = Moods.IdleScan)
        }
    }

    data class Mood(private val mood: com.dangerfield.movingeyes.libraries.eyes.Mood) : LockedItem {
        override val label get() = mood.label
        override val trigger get() = PaywallTrigger.Motion

        /** The style doesn't matter here and the motion does, so this is a
         *  plain eye running the mood you tapped. */
        @Composable
        override fun Preview() {
            EyePreview(
                style = EyeStyles.HumanRealistic,
                sizeDp = PreviewSize,
                behavior = Moods.forMood(mood),
            )
        }
    }
}

/** Big enough to actually judge. This is the only look at a paid style anyone
 *  gets before buying, so it should not be a thumbnail. */
private val PreviewSize = 200.dp
