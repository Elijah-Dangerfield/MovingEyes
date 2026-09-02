package com.dangerfield.movingeyes.libraries.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Below this width the panel is a bottom sheet; at or above it, a right rail.
 *
 * 720dp is the conventional tablet break, and it's the right one here for a
 * physical reason: in landscape on a tablet the canvas's *height* is the
 * dimension that matters, because you're matching eye-holes cut at eye level.
 * A bottom sheet eats height. A rail eats width, of which there is plenty.
 */
private val RailBreakpoint = 720.dp

/**
 * How much of the screen a bottom sheet may take. The canvas scales into what's
 * left, and past this there isn't enough left to judge a composition by.
 */
private const val SheetMaxHeightFraction = 0.5f

private val PanelCornerRadius = 16.dp
private val GrabHandleWidth = 44.dp
private val GrabHandleHeight = 4.dp

/** Which shape the panel took. Callers lay their contents out differently. */
enum class PanelLayout { Sheet, Rail }

/**
 * The editor's controls, in the shape the device calls for.
 *
 * Phone portrait: a bottom sheet, so everything is reachable with one thumb
 * while the other hand holds cardboard against a wall. Tablet landscape: a
 * [Motion.Panel.RailWidthDp] rail on the right, so the canvas keeps its full
 * height and the whole transform row fits on one line.
 *
 * Drag the grab edge to slide it away; a flick decides on its own. [state]
 * carries the live position, so a caller can scale the canvas against
 * `occupiedPx` and the two move together rather than one chasing the other.
 */
@Composable
fun AdaptivePanel(
    state: PanelState,
    modifier: Modifier = Modifier,
    content: @Composable (PanelLayout) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = if (maxWidth >= RailBreakpoint) PanelLayout.Rail else PanelLayout.Sheet
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

        state.grabEdgePx = with(density) { Motion.Panel.GrabEdgeDp.roundToPx() }

        val travel = state.travel.value
        val contentAlpha = ((Motion.Panel.ContentFadeAtTravel - travel) /
            Motion.Panel.ContentFadeAtTravel).coerceIn(0f, 1f)

        val drag = Modifier.draggable(
            state = rememberDraggableState { delta ->
                scope.launch { state.dragBy(delta) }
            },
            orientation = when (layout) {
                PanelLayout.Sheet -> Orientation.Vertical
                PanelLayout.Rail -> Orientation.Horizontal
            },
            onDragStopped = { velocity -> state.settle(velocity, Motion.Panel.slide()) },
        )

        Panel(
            layout = layout,
            offsetPx = state.offsetPx(),
            onExtentMeasured = { state.extentPx = it },
            expanded = state.isExpanded,
            onToggle = { scope.launch { state.toggle() } },
            contentAlpha = contentAlpha,
            dragModifier = drag,
            maxSheetHeight = maxHeight * SheetMaxHeightFraction,
        ) {
            content(layout)
        }
    }
}

@Composable
private fun BoxScope.Panel(
    layout: PanelLayout,
    offsetPx: Int,
    onExtentMeasured: (Int) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    contentAlpha: Float,
    dragModifier: Modifier,
    maxSheetHeight: Dp,
    content: @Composable () -> Unit,
) {
    val shape = when (layout) {
        PanelLayout.Sheet -> RoundedCornerShape(
            topStart = PanelCornerRadius,
            topEnd = PanelCornerRadius,
        )

        PanelLayout.Rail -> RoundedCornerShape(
            topStart = PanelCornerRadius,
            bottomStart = PanelCornerRadius,
        )
    }

    val placement = when (layout) {
        PanelLayout.Sheet -> Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .offset { IntOffset(x = 0, y = offsetPx) }
            .onSizeChanged { onExtentMeasured(it.height) }

        PanelLayout.Rail -> Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(Motion.Panel.RailWidthDp)
            .offset { IntOffset(x = offsetPx, y = 0) }
            .onSizeChanged { onExtentMeasured(it.width) }
    }

    Column(
        modifier = placement
            .clip(shape)
            .background(AppTheme.colors.surfacePrimary.color),
    ) {
        GrabEdge(expanded = expanded, onToggle = onToggle, dragModifier = dragModifier)
        Box(modifier = Modifier.alpha(contentAlpha)) { content() }
    }
}

/**
 * Draggable along the panel's axis and still tappable, because a tap is what
 * you reach for when the panel is a sliver at the bottom of the screen. Amber
 * when collapsed, when it's the only way back in.
 */
@Composable
private fun GrabEdge(expanded: Boolean, onToggle: () -> Unit, dragModifier: Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Motion.Panel.GrabEdgeDp)
            .then(dragModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = GrabHandleWidth, height = GrabHandleHeight)
                .clip(RoundedCornerShape(GrabHandleHeight / 2))
                .background(
                    if (expanded) {
                        AppTheme.colors.borderSecondary.color
                    } else {
                        AppTheme.colors.accentPrimary.color
                    }
                ),
        )
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun PreviewSheetPanel() {
    PreviewContent {
        AdaptivePanel(state = rememberPanelState()) { layout -> PanelSample(layout) }
    }
}

@Preview(widthDp = 1000, heightDp = 700)
@Composable
private fun PreviewRailPanel() {
    PreviewContent {
        AdaptivePanel(state = rememberPanelState()) { layout -> PanelSample(layout) }
    }
}

@Composable
private fun PanelSample(layout: PanelLayout) {
    Column(
        modifier = Modifier.padding(Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        Text(text = layout.name, typography = AppTheme.typography.Heading.H600)
        SegmentedControl(
            options = listOf("Place", "Look", "Motion", "Scene"),
            selected = "Place",
            onSelect = {},
            label = { it },
        )
        ReadoutPill(text = "2 eyes · 148 px · 31.3 mm · 0°")
    }
}
