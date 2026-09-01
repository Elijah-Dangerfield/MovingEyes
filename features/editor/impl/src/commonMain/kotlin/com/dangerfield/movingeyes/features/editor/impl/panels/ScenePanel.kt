@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dangerfield.movingeyes.features.editor.impl.EditorState
import com.dangerfield.movingeyes.features.editor.impl.SleepTimerOptions
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.ColorField
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.scene_brightness
import movingeyes.libraries.resources.generated.resources.scene_canvas_color
import movingeyes.libraries.resources.generated.resources.scene_save
import movingeyes.libraries.resources.generated.resources.scene_sleep_hours
import movingeyes.libraries.resources.generated.resources.scene_sleep_minutes
import movingeyes.libraries.resources.generated.resources.scene_sleep_never
import movingeyes.libraries.resources.generated.resources.scene_sleep_timer
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * Scene-wide settings and the way out to saving.
 *
 * Brightness lives here rather than in the OS because the app needs to go
 * *below* the system minimum — a tablet at its dimmest is still far too bright
 * behind a painting in a dark hallway. It's applied as a black overlay on top
 * of the canvas, which is also why it can never quite reach zero.
 */
@Composable
fun ScenePanel(
    editor: EditorState,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        ColorField(
            label = stringResource(Res.string.scene_canvas_color),
            color = Color(editor.canvas.color),
            onColorChange = { editor.setCanvasColor(it.toArgb().toLong() and 0xFFFFFFFFL) },
        )

        PanelSlider(
            label = stringResource(Res.string.scene_brightness),
            value = editor.canvas.brightness,
            valueLabel = "${(editor.canvas.brightness * 100).roundToInt()}%",
            onValueChange = { editor.setBrightness(it) },
            valueRange = MinBrightness..1f,
        )

        // Options rather than a slider: nobody wants "forty-seven minutes", and
        // a scene that runs all night is a legitimate choice rather than a
        // missing value.
        PanelRow(label = stringResource(Res.string.scene_sleep_timer)) {
            val labels = SleepTimerOptions.associateWith { sleepTimerLabel(it) }
            SegmentedControl(
                options = SleepTimerOptions,
                selected = editor.canvas.sleepTimer,
                onSelect = { editor.setSleepTimer(it) },
                label = { labels.getValue(it) },
            )
        }

        Button(
            onClick = onSave,
            size = ButtonSize.Medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.scene_save))
        }
    }
}

@Composable
private fun sleepTimerLabel(timer: Duration?): String = when {
    timer == null -> stringResource(Res.string.scene_sleep_never)
    timer.inWholeMinutes < MinutesPerHour -> stringResource(
        Res.string.scene_sleep_minutes,
        timer.inWholeMinutes.toInt(),
    )
    else -> stringResource(Res.string.scene_sleep_hours, timer.inWholeHours.toInt())
}

/** Matches EditorState's floor. A brightness slider that reaches zero looks
 *  exactly like a crash, and the way back is invisible. */
private const val MinBrightness = 0.05f

private const val MinutesPerHour = 60
