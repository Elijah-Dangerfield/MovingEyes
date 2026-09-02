package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.billing.DemoControl
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyePreview
import org.jetbrains.compose.resources.StringResource

/**
 * A paid thing the user just reached for, with everything the preview sheet
 * needs to show it and then apply it.
 */
sealed interface LockedItem {

    val label: StringResource
    val control: DemoControl

    fun apply(editor: EditorState)

    @Composable
    fun Preview()

    data class Style(private val style: EyeStyle) : LockedItem {
        override val label get() = style.id.label
        override val control get() = DemoControl.EyeStyle

        override fun apply(editor: EditorState) = editor.setStyle(style)

        @Composable
        override fun Preview() {
            EyePreview(style = style, sizeDp = PreviewSize, behavior = Moods.IdleScan)
        }
    }

    data class Mood(private val mood: com.dangerfield.movingeyes.libraries.eyes.Mood) : LockedItem {
        override val label get() = mood.label
        override val control get() = DemoControl.Mood

        override fun apply(editor: EditorState) = editor.setMood(mood)

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

private val PreviewSize = 120.dp
