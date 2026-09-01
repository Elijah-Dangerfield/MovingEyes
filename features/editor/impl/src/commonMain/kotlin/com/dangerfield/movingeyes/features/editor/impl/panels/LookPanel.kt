@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.features.editor.impl.EditorState
import com.dangerfield.movingeyes.features.editor.impl.label
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.EyeTier
import com.dangerfield.movingeyes.libraries.render.EyePreview
import com.dangerfield.movingeyes.libraries.ui.components.ColorField
import com.dangerfield.movingeyes.libraries.ui.components.StyleCard
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.look_glow
import movingeyes.libraries.resources.generated.resources.look_iris
import movingeyes.libraries.resources.generated.resources.look_pupil
import movingeyes.libraries.resources.generated.resources.look_sclera
import movingeyes.libraries.resources.generated.resources.look_size
import movingeyes.libraries.resources.generated.resources.look_style
import movingeyes.libraries.resources.generated.resources.look_veins
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Style and colour. Locked styles animate in the picker rather than showing a
 * greyed thumbnail — you can't tell what Demon looks like from a still image.
 */
@Composable
fun LookPanel(
    editor: EditorState,
    isUnlocked: Boolean,
    onLockedStyleTapped: (EyeStyle) -> Unit,
    shortEdgePx: Float,
    modifier: Modifier = Modifier,
) {
    val activeStyle = editor.activeStyle()
    val first = editor.activeEye()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        PanelRow(label = stringResource(Res.string.look_style)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                items(EyeStyles.All.size) { index ->
                    val style = EyeStyles.All[index]
                    val locked = style.tier == EyeTier.Paid && !isUnlocked
                    StyleCard(
                        label = stringResource(style.id.label),
                        isSelected = style.id == activeStyle?.id,
                        isLocked = locked,
                        onClick = {
                            if (locked) onLockedStyleTapped(style) else editor.setStyle(style)
                        },
                        preview = {
                            // The style's own palette, not the scene's: twelve
                            // recoloured to match would be indistinguishable.
                            EyePreview(
                                style = style,
                                sizeDp = StylePreviewSize,
                                behavior = editor.activeBehavior(),
                            )
                        },
                    )
                }
            }
        }

        if (first != null) {
            ColorField(
                label = stringResource(Res.string.look_sclera),
                color = first.scleraColor,
                onColorChange = { editor.setScleraColor(it) },
            )
            ColorField(
                label = stringResource(Res.string.look_iris),
                color = first.irisColor,
                onColorChange = { editor.setIrisColor(it) },
            )
            ColorField(
                label = stringResource(Res.string.look_pupil),
                color = first.pupilColor,
                onColorChange = { editor.setPupilColor(it) },
            )

            PanelSlider(
                label = stringResource(Res.string.look_size),
                value = first.sizePx / shortEdgePx,
                valueLabel = "${first.sizePx.roundToInt()} px",
                onValueChange = { editor.setSizePx(it * shortEdgePx) },
                valueRange = MinSizeFraction..MaxSizeFraction,
            )

            PanelSlider(
                label = stringResource(Res.string.look_glow),
                value = first.glowFraction,
                valueLabel = "${(first.glowFraction * 100).roundToInt()}%",
                onValueChange = { editor.setGlowFraction(it) },
                valueRange = 0f..MaxGlowFraction,
            )

            if (activeStyle?.hasVeins == true) {
                PanelSlider(
                    label = stringResource(Res.string.look_veins),
                    value = first.veinIntensity,
                    valueLabel = "${(first.veinIntensity * 100).roundToInt()}%",
                    onValueChange = { editor.setVeinIntensity(it) },
                )
            }
        }
    }
}

private val StylePreviewSize = 52.dp

/** Fractions of the canvas's short edge. Below the minimum an eye stops
 *  reading from across a room; above the maximum a pair won't fit. */
private const val MinSizeFraction = 0.03f
private const val MaxSizeFraction = 0.45f

/** Matches EditorState's cap. */
private const val MaxGlowFraction = 0.35f
