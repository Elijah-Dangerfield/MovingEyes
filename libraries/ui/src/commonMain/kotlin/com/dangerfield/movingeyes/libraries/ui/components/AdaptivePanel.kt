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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.PhoneAndTabletPreview
import com.dangerfield.movingeyes.libraries.ui.PreviewContent
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import kotlinx.coroutines.launch

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
    /** Stays on screen when the panel is slid away, and is the drag handle.
     *  Put the thing that says what the panel is for in here — a bare handle
     *  gives no reason to open it. */
    header: @Composable (PanelLayout) -> Unit,
    content: @Composable (PanelLayout) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = if (maxWidth >= RailBreakpoint) PanelLayout.Rail else PanelLayout.Sheet
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current

        // Fades out over the stretch between collapsed and expanded, so the
        // controls aren't legible while they're sliding past the header.
        val reveal = state.extentPx - state.headerPx
        val contentAlpha = if (reveal <= 0) {
            0f
        } else {
            ((state.occupiedPx - state.headerPx) / reveal / Motion.Panel.ContentFadeAtTravel)
                .coerceIn(0f, 1f)
        }

        LaunchedEffect(state.extentPx, state.headerPx) { state.settleIntoLayout() }

        val drag = Modifier.draggable(
            state = rememberDraggableState { delta ->
                scope.launch { state.dragBy(delta) }
            },
            orientation = when (layout) {
                PanelLayout.Sheet -> Orientation.Vertical
                PanelLayout.Rail -> Orientation.Horizontal
            },
            onDragStopped = { velocity -> state.settle(-velocity, Motion.Panel.slide()) },
        )

        Panel(
            layout = layout,
            occupiedPx = { state.occupiedPx },
            onExtentMeasured = { state.extentPx = it },
            onHeaderMeasured = { state.headerPx = it },
            expanded = state.isExpanded,
            onToggle = { scope.launch { if (state.isExpanded) state.show() else state.expand() } },
            contentAlpha = contentAlpha,
            dragModifier = drag,
            maxSheetHeight = maxHeight * SheetMaxHeightFraction,
            header = { header(layout) },
        ) {
            content(layout)
        }
    }
}

@Composable
private fun BoxScope.Panel(
    layout: PanelLayout,
    occupiedPx: () -> Float,
    onExtentMeasured: (Int) -> Unit,
    onHeaderMeasured: (Int) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    contentAlpha: Float,
    dragModifier: Modifier,
    maxSheetHeight: Dp,
    header: @Composable () -> Unit,
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
        // Slid by its *own* height, read inside graphicsLayer where the layout
        // size is already known. Offsetting by the measured extent instead
        // meant that on the frame before the first measurement the extent was
        // zero, so the panel drew fully in place and then jumped — it popped
        // into existence rather than arriving.
        PanelLayout.Sheet -> Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .graphicsLayer { translationY = size.height - occupiedPx() }
            .onSizeChanged { onExtentMeasured(it.height) }

        PanelLayout.Rail -> Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(Motion.Panel.RailWidthDp)
            .graphicsLayer { translationX = size.width - occupiedPx() }
            .onSizeChanged { onExtentMeasured(it.width) }
    }

    Column(
        modifier = placement
            .clip(shape)
            .background(AppTheme.colors.surfacePrimary.color),
    ) {
        Column(
            modifier = Modifier
                .onSizeChanged { onHeaderMeasured(it.height) }
                .then(dragModifier),
        ) {
            GrabEdge(expanded = expanded, onToggle = onToggle)
            header()
        }
        Box(modifier = Modifier.alpha(contentAlpha)) { content() }
    }
}

/** Still tappable, because a tap is what you reach for when the panel is
 *  mostly off screen. Amber when collapsed, when it's the way back in. */
@Composable
private fun GrabEdge(expanded: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Motion.Panel.GrabEdgeDp)
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

/**
 * Both shapes at once: the sheet a phone gets, and the rail a tablet does. The
 * whole point of this component is that those differ, so previewing one is
 * previewing half of it.
 */
@PhoneAndTabletPreview
@Composable
private fun PreviewAdaptivePanel() {
    PreviewContent {
        AdaptivePanel(
            state = rememberPanelState(PanelPosition.Expanded),
            header = { SegmentedControl(listOf("Place", "Look"), "Place", {}, label = { it }) },
        ) { layout -> PanelSample(layout) }
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
