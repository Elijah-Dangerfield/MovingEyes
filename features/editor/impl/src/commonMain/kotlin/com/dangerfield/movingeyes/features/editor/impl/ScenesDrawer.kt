@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyePreview
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.ScenePreset
import com.dangerfield.movingeyes.libraries.scene.ScenePresets
import com.dangerfield.movingeyes.libraries.ui.components.LockBadge
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonType
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.scenes_delete
import movingeyes.libraries.resources.generated.resources.scenes_mine
import movingeyes.libraries.resources.generated.resources.scenes_new_blank
import movingeyes.libraries.resources.generated.resources.scenes_none_saved
import movingeyes.libraries.resources.generated.resources.scenes_presets
import movingeyes.libraries.resources.generated.resources.scenes_title
import org.jetbrains.compose.resources.stringResource

/**
 * Saved scenes and the eight launch presets, behind the top-left button.
 *
 * v2 demoted the gallery from being the home screen to being a drawer, and that
 * is the right call: the canvas is the product, and a grid of thumbnails in
 * front of it makes the app look like a content library rather than a tool.
 *
 * Every row renders a **live miniature**, never a static thumbnail. A still
 * image of two eyes is indistinguishable from a still image of two other eyes;
 * the motion is the only thing that tells them apart, and it's also the thing
 * being sold.
 */
@Composable
fun ScenesDrawer(
    saved: List<Scene>,
    onOpenScene: (Scene) -> Unit,
    onOpenPreset: (ScenePreset) -> Unit,
    onDeleteScene: (Scene) -> Unit,
    onNewBlank: () -> Unit,
    onDismiss: () -> Unit,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // A scrim that closes on tap. The drawer covers the canvas, and the
        // canvas is what the user actually wants to look at, so getting back to
        // it must not require finding a button.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .clickable(onClick = onDismiss),
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                // A fraction as well as a cap, so a phone always leaves a strip
                // of scrim to tap. At a flat 360dp the drawer covers the whole
                // width of a phone and the only way out is the back gesture.
                .fillMaxWidth(DrawerWidthFraction)
                .widthIn(max = DrawerMaxWidth)
                .background(AppTheme.colors.surfacePrimary.color)
                .safeDrawingPadding()
                .padding(Dimension.D700),
            verticalArrangement = Arrangement.spacedBy(Dimension.D500),
        ) {
            Text(
                text = stringResource(Res.string.scenes_title),
                typography = AppTheme.typography.Heading.H600,
            )

            Button(
                onClick = onNewBlank,
                size = ButtonSize.Small,
                style = ButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.scenes_new_blank))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                item {
                    SectionLabel(stringResource(Res.string.scenes_mine))
                }

                if (saved.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.scenes_none_saved),
                            typography = AppTheme.typography.Body.B600,
                            color = AppTheme.colors.textTertiary,
                        )
                    }
                }

                items(saved.size) { index ->
                    val scene = saved[index]
                    SceneRow(
                        name = scene.name,
                        isLocked = false,
                        onClick = { onOpenScene(scene) },
                        onDelete = { onDeleteScene(scene) },
                        preview = {
                            val first = scene.eyes.firstOrNull()
                            EyePreview(
                                style = EyeStyles.byId(
                                    first?.styleId ?: EyeStyles.HumanBasic.id,
                                ),
                                sizeDp = MiniatureEyeSize,
                                irisColor = first?.let { Color(it.irisColor) },
                                scleraColor = first?.let { Color(it.scleraColor) },
                                behavior = first?.behavior() ?: Moods.FreeDefault,
                                seed = index,
                            )
                        },
                    )
                }

                item {
                    SectionLabel(stringResource(Res.string.scenes_presets))
                }

                items(ScenePresets.All.size) { index ->
                    val preset = ScenePresets.All[index]
                    SceneRow(
                        name = stringResource(preset.id.label),
                        isLocked = preset.isPaid && !isUnlocked,
                        onClick = { onOpenPreset(preset) },
                        onDelete = null,
                        preview = {
                            val first = preset.eyes.first()
                            EyePreview(
                                style = EyeStyles.byId(first.styleId),
                                sizeDp = MiniatureEyeSize,
                                irisColor = Color(first.irisColor),
                                scleraColor = Color(first.scleraColor),
                                behavior = first.behavior(),
                                seed = index,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        typography = AppTheme.typography.Caption.C300,
        color = AppTheme.colors.textTertiary,
        allCaps = true,
        modifier = Modifier.padding(top = Dimension.D400),
    )
}

@Composable
private fun SceneRow(
    name: String,
    isLocked: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    preview: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(RowCornerRadius))
            .background(AppTheme.colors.surfaceSecondary.color)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimension.D500),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimension.D500),
    ) {
        Box(
            modifier = Modifier.size(MiniatureSize),
            contentAlignment = Alignment.Center,
        ) {
            preview()
        }

        Text(
            text = name,
            typography = AppTheme.typography.Body.B600,
            modifier = Modifier.weight(1f),
        )

        if (isLocked) LockBadge()

        if (onDelete != null) {
            Button(
                onClick = onDelete,
                size = ButtonSize.Small,
                style = ButtonStyle.Text,
                type = ButtonType.Danger,
            ) {
                Text(stringResource(Res.string.scenes_delete))
            }
        }
    }
}

private val DrawerMaxWidth = 360.dp
private const val DrawerWidthFraction = 0.86f
private val RowHeight = 72.dp
private val RowCornerRadius = 14.dp
private val MiniatureSize = 48.dp
private val MiniatureEyeSize = 34.dp

/** Dark enough to push the canvas back, light enough that the scene stays
 *  visible behind — you're picking a replacement for what you can still see. */
private val ScrimColor = Color.Black.copy(alpha = 0.6f)
