@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.features.editor.impl.EditorState
import com.dangerfield.movingeyes.features.editor.impl.RotationMode
import com.dangerfield.movingeyes.libraries.scene.CanvasRotation
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.Stepper
import com.dangerfield.movingeyes.libraries.ui.components.Switch
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonType
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.place_add_eye
import movingeyes.libraries.resources.generated.resources.place_align_evenly
import movingeyes.libraries.resources.generated.resources.place_canvas_turn
import movingeyes.libraries.resources.generated.resources.place_delete
import movingeyes.libraries.resources.generated.resources.place_horizontal
import movingeyes.libraries.resources.generated.resources.place_mirror
import movingeyes.libraries.resources.generated.resources.place_needs_two
import movingeyes.libraries.resources.generated.resources.place_rotate
import movingeyes.libraries.resources.generated.resources.place_rotate_each
import movingeyes.libraries.resources.generated.resources.place_rotate_group
import movingeyes.libraries.resources.generated.resources.place_select_all
import movingeyes.libraries.resources.generated.resources.place_snapping
import movingeyes.libraries.resources.generated.resources.place_vertical
import org.jetbrains.compose.resources.stringResource

/**
 * Alignment. Fingers are too fat for matching eyes to holes cut in cardboard at
 * arm's length, so snapping does most of the work and the stepper the last two
 * pixels. All of it is free — the paywall runs along motion.
 */
@Composable
fun PlacePanel(
    editor: EditorState,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    snappingEnabled: Boolean,
    onSnappingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rotationMode by remember { mutableStateOf(RotationMode.Group) }
    val hasMultiple = editor.activeIndices().size > 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D700),
        ) {
            PanelRow(
                label = stringResource(Res.string.place_horizontal),
                modifier = Modifier.weight(1f),
            ) {
                Stepper(
                    onStep = { steps ->
                        editor.nudge(steps.toFloat(), 0f, canvasWidthPx, canvasHeightPx)
                    },
                )
            }
            PanelRow(
                label = stringResource(Res.string.place_vertical),
                modifier = Modifier.weight(1f),
            ) {
                Stepper(
                    onStep = { steps ->
                        editor.nudge(0f, steps.toFloat(), canvasWidthPx, canvasHeightPx)
                    },
                )
            }
        }

        PanelRow(
            label = stringResource(Res.string.place_rotate),
            trailing = {
                // Identical with a single selection, so the choice would be noise.
                if (hasMultiple) {
                    // SegmentedControl's label is a plain function, not composable.
                    val groupLabel = stringResource(Res.string.place_rotate_group)
                    val eachLabel = stringResource(Res.string.place_rotate_each)
                    SegmentedControl(
                        options = RotationMode.entries,
                        selected = rotationMode,
                        onSelect = { rotationMode = it },
                        label = { mode ->
                            when (mode) {
                                RotationMode.Group -> groupLabel
                                RotationMode.Each -> eachLabel
                            }
                        },
                    )
                }
            },
        ) {
            Stepper(
                onStep = { steps ->
                    editor.rotateSelection(
                        degrees = steps.toFloat(),
                        mode = rotationMode,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                    )
                },
                unitLabel = RotationUnitLabel,
                holdLabel = RotationHoldLabel,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
        ) {
            Button(
                onClick = { editor.alignEvenly(canvasWidthPx, canvasHeightPx) },
                enabled = hasMultiple,
                size = ButtonSize.Small,
                style = ButtonStyle.Outlined,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.place_align_evenly))
            }
            Button(
                onClick = { editor.mirror(canvasWidthPx, canvasHeightPx) },
                enabled = hasMultiple,
                size = ButtonSize.Small,
                style = ButtonStyle.Outlined,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.place_mirror))
            }
        }

        if (!hasMultiple) {
            Text(
                text = stringResource(Res.string.place_needs_two),
                typography = AppTheme.typography.Caption.C300,
                color = AppTheme.colors.textTertiary,
            )
        }

        PanelRow(
            label = stringResource(Res.string.place_snapping),
            trailing = {
                Switch(checked = snappingEnabled, onCheckedChange = onSnappingChange)
            },
        ) {}

        PanelRow(label = stringResource(Res.string.place_canvas_turn)) {
            SegmentedControl(
                options = CanvasRotation.entries,
                selected = editor.canvas.rotation,
                onSelect = { editor.setCanvasRotation(it) },
                label = { "${it.degrees}°" },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
        ) {
            Button(
                onClick = { editor.addEye() },
                size = ButtonSize.Small,
                style = ButtonStyle.Outlined,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.place_add_eye))
            }
            Button(
                onClick = { editor.selectAll() },
                size = ButtonSize.Small,
                style = ButtonStyle.Outlined,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.place_select_all))
            }
            Button(
                onClick = { editor.deleteSelection() },
                enabled = editor.selection.isNotEmpty() &&
                    editor.selection.size < editor.eyes.size,
                type = ButtonType.Danger,
                size = ButtonSize.Small,
                style = ButtonStyle.Outlined,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.place_delete))
            }
        }
    }
}

/** Symbols rather than copy, so not in the string catalogue. */
private const val RotationUnitLabel = "1°"
private const val RotationHoldLabel = "hold 10°"
