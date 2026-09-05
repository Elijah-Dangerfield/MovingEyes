package com.dangerfield.movingeyes.system.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangerfield.movingeyes.libraries.ui.system.LocalContentColor
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorCard
import com.dangerfield.movingeyes.libraries.ui.system.color.ColorResource
import com.dangerfield.movingeyes.libraries.ui.system.color.toHexString
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Radii
import org.jetbrains.compose.ui.tooling.preview.Preview

@Immutable
@Suppress("LongParameterList")
interface Colors {

    val accentPrimary: ColorResource
    val onAccentPrimary: ColorResource
    val accentSecondary: ColorResource
    val onAccentSecondary: ColorResource

    /* Backgrounds */
    val shadow: ColorResource
    val background: ColorResource
    val backgroundOverlay: ColorResource
    val onBackground: ColorResource
    val border: ColorResource

    val borderSecondary: ColorResource
    val borderDisabled: ColorResource

    /* Texts */
    val text: ColorResource
    val textSecondary: ColorResource

    /**
     * The quietest readable text: field labels above a number, section
     * eyebrows, unit suffixes. Distinct from [textDisabled] — a tertiary label
     * is live and its control works, it's just not the thing being read.
     * Conflating the two makes every axis label look switched off.
     */
    val textTertiary: ColorResource
    val textDisabled: ColorResource
    val danger: ColorResource

    val status: StatusColor

    /* Surfaces */
    val surfacePrimary: ColorResource
    val onSurfacePrimary: ColorResource
    val surfaceSecondary: ColorResource
    val onSurfaceSecondary: ColorResource
    val surfaceTertiary: ColorResource
    val onSurfaceTertiary: ColorResource

    val surfaceDisabled: ColorResource
    val onSurfaceDisabled: ColorResource

}

interface StatusColor {
    val okay: ColorResource
    val warning: ColorResource
    val bad: ColorResource
}

/**
 * Safelight. One theme, dark only — this app is used in a dark room, at arm's
 * length, and a light mode would be actively hostile to the thing it's for.
 *
 * The rules the design leans on, which the semantic names above don't make
 * obvious on their own:
 *
 *  - [background] is true black and belongs to the **canvas only**. Chrome
 *    sits on [surfacePrimary] or above so the canvas edge always reads as an
 *    edge, and so OLED pixels under the eyes stay genuinely off.
 *  - [accentPrimary] is the single amber. It marks live, selected and
 *    unlocked, and nothing else. If a second accent starts to feel necessary,
 *    the hierarchy is wrong somewhere else.
 *  - Nothing in chrome drops below 60% contrast against its plate.
 */
val safelightColors = object : Colors {
    override val accentPrimary = ColorResource.Accent
    override val onAccentPrimary = ColorResource.Surface1
    override val accentSecondary = ColorResource.AccentDeep
    override val onAccentSecondary = ColorResource.TextHi

    override val shadow = ColorResource.Black_A70
    override val danger = ColorResource.Danger

    override val background = ColorResource.Canvas
    override val onBackground = ColorResource.TextHi
    override val backgroundOverlay = ColorResource.Black_A70

    override val surfacePrimary = ColorResource.Surface1
    override val onSurfacePrimary = ColorResource.TextHi
    override val surfaceSecondary = ColorResource.Surface2
    override val onSurfaceSecondary = ColorResource.TextHi
    override val surfaceTertiary = ColorResource.Surface3
    override val onSurfaceTertiary = ColorResource.TextHi
    override val surfaceDisabled = ColorResource.Surface2
    override val onSurfaceDisabled = ColorResource.TextLow

    override val border = ColorResource.Line
    override val borderSecondary = ColorResource.LineStrong
    override val borderDisabled = ColorResource.Surface2

    override val text = ColorResource.TextHi
    override val textSecondary = ColorResource.TextMid
    override val textTertiary = ColorResource.TextLow
    override val textDisabled = ColorResource.TextLow

    override val status = object : StatusColor {
        override val okay = ColorResource.Accent
        override val warning = ColorResource.Accent
        override val bad = ColorResource.Danger
    }
}

@Composable
private fun SectionTitle(text: String, colors: Colors) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary.color,
        modifier = Modifier.padding(bottom = Dimension.D400)
    )
}

@Composable
private fun HeroPanel(colors: Colors) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.Card.shape)
            .background(colors.surfacePrimary.color)
            .border(1.dp, colors.border.color, Radii.Card.shape)
            .padding(Dimension.D700)
    ) {
        Text(
            text = "Color palette",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurfacePrimary.color
        )
        Text(
            text = "Modern light theme",
            fontSize = 14.sp,
            color = colors.textSecondary.color,
            modifier = Modifier.padding(top = Dimension.D200)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimension.D600),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.Card.shape)
                    .background(colors.surfaceSecondary.color)
                    .padding(Dimension.D500)
            ) {
                Text(
                    text = "Active session",
                    fontSize = 14.sp,
                    color = colors.onSurfaceSecondary.color
                )
                Text(
                    text = "42m remaining",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceSecondary.color,
                    modifier = Modifier.padding(top = Dimension.D200)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.Card.shape)
                    .background(colors.backgroundOverlay.color)
                    .padding(Dimension.D500)
            ) {
                Text(
                    text = "Status",
                    fontSize = 14.sp,
                    color = colors.onBackground.color
                )
                Text(
                    text = "All good",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentSecondary.color,
                    modifier = Modifier.padding(top = Dimension.D200)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimension.D600),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent activity",
                fontSize = 14.sp,
                color = colors.textSecondary.color
            )
            Box(
                modifier = Modifier
                    .clip(Radii.Button.shape)
                    .background(colors.accentPrimary.color)
                    .padding(horizontal = Dimension.D800, vertical = Dimension.D400)
            ) {
                Text(
                    text = "View all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onAccentPrimary.color
                )
            }
        }
    }
}

@Composable
private fun AccentPalette(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Accent stack", colors)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            AccentChip(
                label = "Primary",
                background = colors.accentPrimary,
                foreground = colors.onAccentPrimary,
                supporting = colors.accentPrimary.toHexString()
            )
            AccentChip(
                label = "Secondary",
                background = colors.accentSecondary,
                foreground = colors.onAccentSecondary,
                supporting = colors.accentSecondary.toHexString()
            )
        }
    }
}

@Composable
private fun RowScope.AccentChip(
    label: String,
    background: ColorResource,
    foreground: ColorResource,
    supporting: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(Radii.Card.shape)
            .background(background.color.copy(alpha = 0.15f))
            .border(1.dp, background.color, Radii.Card.shape)
            .padding(Dimension.D500)
    ) {
        Box(
            modifier = Modifier
                .clip(Radii.Button.shape)
                .background(background.color)
                .padding(horizontal = Dimension.D800, vertical = Dimension.D400)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = foreground.color
            )
        }
        Text(
            text = background.designSystemName,
            fontSize = 12.sp,
            color = background.color,
            modifier = Modifier.padding(top = Dimension.D300)
        )
        Text(
            text = supporting,
            fontSize = 10.sp,
            color = foreground.color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SurfaceStack(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Surface ladder", colors)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            SurfaceCard(
                title = "Primary",
                background = colors.surfacePrimary,
                foreground = colors.onSurfacePrimary,
                border = colors.border,
                supporting = colors.surfacePrimary.toHexString()
            )
            SurfaceCard(
                title = "Secondary",
                background = colors.surfaceSecondary,
                foreground = colors.onSurfaceSecondary,
                border = colors.border,
                supporting = colors.surfaceSecondary.toHexString()
            )
            SurfaceCard(
                title = "Tertiary",
                background = colors.surfaceTertiary,
                foreground = colors.onSurfaceTertiary,
                border = colors.border,
                supporting = colors.surfaceTertiary.toHexString()
            )

            SurfaceCard(
                title = "Disabled",
                background = colors.surfaceDisabled,
                foreground = colors.onSurfaceDisabled,
                border = colors.border,
                supporting = colors.surfaceTertiary.toHexString()
            )
        }
    }
}

@Composable
private fun RowScope.SurfaceCard(
    title: String,
    background: ColorResource,
    foreground: ColorResource,
    border: ColorResource,
    supporting: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(Radii.Card.shape)
            .background(background.color)
            .border(1.dp, border.color, Radii.Card.shape)
            .padding(Dimension.D500)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = foreground.color.copy(alpha = 0.9f)
        )
        Text(
            text = "Card content",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground.color,
            modifier = Modifier.padding(top = Dimension.D200)
        )
        Text(
            text = supporting,
            fontSize = 10.sp,
            color = foreground.color.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = Dimension.D300)
        )
    }
}

@Composable
private fun TextHierarchy(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Typography contrast", colors)
        Column(
            modifier = Modifier
                .clip(Radii.Card.shape)
                .background(colors.background.color)
                .border(1.dp, colors.border.color, Radii.Card.shape)
                .padding(Dimension.D600),
            verticalArrangement = Arrangement.spacedBy(Dimension.D500)
        ) {
            TextSample("Primary", colors.text, colors.text)
            TextSample("Secondary", colors.textSecondary, colors.textSecondary)
            TextSample("Disabled", colors.textDisabled, colors.textDisabled)
            TextSample("Danger", colors.danger, colors.danger)
        }
    }
}

@Composable
private fun TextSample(label: String, swatch: ColorResource, hexColor: ColorResource) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = swatch.color
        )
        Text(
            text = hexColor.toHexString(),
            fontSize = 11.sp,
            color = swatch.color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SemanticStrip(colors: Colors) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("System states", colors)
        Row(
            modifier = Modifier
                .clip(Radii.Card.shape)
                .border(1.dp, colors.border.color, Radii.Card.shape)
                .background(colors.surfaceSecondary.color)
                .padding(Dimension.D400),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D400)
        ) {
            SemanticBadge("Background", colors.background, colors.onBackground)
            SemanticBadge("Overlay", colors.backgroundOverlay, colors.onBackground)
            SemanticBadge("Shadow", colors.shadow, colors.onBackground)
            SemanticBadge("Danger", colors.danger, colors.onAccentSecondary)
        }
    }
}

@Composable
private fun RowScope.SemanticBadge(
    label: String,
    background: ColorResource,
    content: ColorResource
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(Radii.Card.shape)
            .background(background.color)
            .padding(Dimension.D400)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = content.color.copy(alpha = 0.8f)
        )
        Text(
            text = background.designSystemName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = content.color,
            modifier = Modifier.padding(top = Dimension.D200)
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PaletteGridSection(colors: Colors) {
    val palette = listOf(
        colors.background,
        colors.backgroundOverlay,
        colors.onBackground,
        colors.surfacePrimary,
        colors.onSurfacePrimary,
        colors.surfaceSecondary,
        colors.onSurfaceSecondary,
        colors.surfaceTertiary,
        colors.onSurfaceTertiary,
        colors.surfaceDisabled,
        colors.onSurfaceDisabled,
        colors.accentPrimary,
        colors.onAccentPrimary,
        colors.accentSecondary,
        colors.onAccentSecondary,
        colors.text,
        colors.textSecondary,
        colors.textDisabled,
        colors.danger,
        colors.border,
        colors.borderDisabled,
        colors.shadow
    ).distinctBy { it.designSystemName }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle("Palette grid", colors)
        Box(
            modifier = Modifier
                .clip(Radii.Card.shape)
                .background(colors.surfaceSecondary.color)
                .border(1.dp, colors.border.color, Radii.Card.shape)
                .padding(Dimension.D300)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimension.D300),
                verticalArrangement = Arrangement.spacedBy(Dimension.D300)
            ) {
                palette.forEach { swatch ->
                    ColorCard(
                        colorResource = swatch,
                        title = swatch.designSystemName,
                        description = swatch.toHexString()
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewColorSwatch(colors: Colors) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.color)
            .padding(horizontal = Dimension.D800, vertical = Dimension.D600),
        verticalArrangement = Arrangement.spacedBy(Dimension.D700)
    ) {
        item { HeroPanel(colors) }
        item { AccentPalette(colors) }
        item { SurfaceStack(colors) }
        item { TextHierarchy(colors) }
        item { SemanticStrip(colors) }
        item { PaletteGridSection(colors) }
    }
}

@Preview(widthDp = 600, heightDp = 2000)
@Composable
private fun PreviewSafelightColors() {
    PreviewColorSwatch(safelightColors)
}

/**
 * The text colour that belongs on [surface].
 *
 * Every `on*` token in this file exists to pair with a specific surface, and
 * nothing in the type system made anyone use the right one — `Surface` takes
 * `color` and `contentColor` as two independent required arguments, so each
 * caller re-decides and any of them can drift. That is not hypothetical: the
 * dialog was moved from Surface1 to Surface3 and its content colour stayed
 * behind, which is a change that should not have been possible to make halfway.
 *
 * Falls back to [text] for anything unlisted, which is correct for the dark
 * surfaces this palette is built from and wrong loudly rather than quietly if a
 * light one is ever added.
 */
fun Colors.contentColorFor(surface: ColorResource): ColorResource = when (surface) {
    surfacePrimary -> onSurfacePrimary
    surfaceSecondary -> onSurfaceSecondary
    surfaceTertiary -> onSurfaceTertiary
    surfaceDisabled -> onSurfaceDisabled
    accentPrimary -> onAccentPrimary
    accentSecondary -> onAccentSecondary
    background -> onBackground
    else -> text
}

/** Paints [surface] and provides the content colour that goes with it, so the
 *  two cannot be changed apart. */
@Composable
fun ProvideSurfaceContentColor(surface: ColorResource, content: @Composable () -> Unit) {
    ProvideContentColor(AppTheme.colors.contentColorFor(surface), content)
}

@Composable
fun ProvideContentColor(color: ColorResource, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides color,
        androidx.compose.material3.LocalContentColor provides color.color,
        content = content
    )
}