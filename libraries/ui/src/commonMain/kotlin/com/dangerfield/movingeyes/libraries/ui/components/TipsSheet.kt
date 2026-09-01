package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SheetCornerRadius = 16.dp
private val BodyMinHeight = 88.dp

/** One page of the tips sheet. */
data class Tip(val title: String, val body: String)

/**
 * What replaced onboarding.
 *
 * The app opens on a live canvas with two eyes already blinking, and this sits
 * at the bottom until it's swiped away. Nobody reads three full-screen cards
 * before they've seen the thing work; they will read one sentence sitting under
 * something already moving.
 *
 * So the rules are: it never covers the canvas, it never blocks a gesture, and
 * skip is always available on every page. If a tip can't earn its place beside
 * a working canvas, cut the tip.
 *
 * Copy is passed in rather than owned here — this is a shell, and the words
 * belong to the feature that shows it.
 */
@Composable
fun TipsSheet(
    tips: List<Tip>,
    pageIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    eyebrow: String,
    skipLabel: String,
    nextLabel: String,
    doneLabel: String,
    counter: (page: Int, total: Int) -> String,
    modifier: Modifier = Modifier,
) {
    if (tips.isEmpty()) return
    val page = pageIndex.coerceIn(0, tips.lastIndex)
    val tip = tips[page]
    val isLast = page == tips.lastIndex
    val offsetPx = with(LocalDensity.current) { Motion.Tips.PageOffsetDp.roundToPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SheetCornerRadius))
            .background(AppTheme.colors.surfacePrimary.color)
            .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(SheetCornerRadius))
            .padding(Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = eyebrow,
                    typography = AppTheme.typography.Readout.R300,
                    color = AppTheme.colors.accentPrimary,
                )
                Text(
                    text = counter(page + 1, tips.size),
                    typography = AppTheme.typography.Readout.R300,
                    color = AppTheme.colors.textTertiary,
                )
            }
            Text(
                text = skipLabel,
                typography = AppTheme.typography.Label.L400.SemiBold,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier
                    .clickable(onClick = onSkip)
                    .padding(Dimension.D300),
            )
        }

        AnimatedContent(
            targetState = tip,
            transitionSpec = {
                (fadeIn(tween(Motion.Tips.PageMillis)) +
                    slideInHorizontally(tween(Motion.Tips.PageMillis)) { offsetPx })
                    .togetherWith(
                        fadeOut(tween(Motion.Tips.PageMillis)) +
                            slideOutHorizontally(tween(Motion.Tips.PageMillis)) { -offsetPx }
                    )
            },
            label = "tipPage",
        ) { current ->
            Column(
                modifier = Modifier.heightIn(min = BodyMinHeight),
                verticalArrangement = Arrangement.spacedBy(Dimension.D400),
            ) {
                Text(text = current.title, typography = AppTheme.typography.Heading.H700)
                Text(
                    text = current.body,
                    typography = AppTheme.typography.Body.B500,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Medium,
            style = ButtonStyle.Filled,
        ) {
            Text(if (isLast) doneLabel else nextLabel)
        }
    }
}

@Preview
@Composable
private fun PreviewTipsSheet() {
    PreviewContent(modifier = Modifier.padding(Dimension.D800)) {
        TipsSheet(
            tips = listOf(
                Tip(
                    title = "Drag an eye. Pinch to size it.",
                    body = "The canvas is the screen at actual size, so what you see here is what " +
                        "gets taped to the cardboard. Eyes snap to each other and to the middle " +
                        "of the screen as you go.",
                ),
            ),
            pageIndex = 0,
            onNext = {},
            onSkip = {},
            eyebrow = "TIPS",
            skipLabel = "Skip",
            nextLabel = "Next",
            doneLabel = "Done",
            counter = { page, total -> "$page of $total" },
        )
    }
}
