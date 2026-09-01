@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.device.ScreenMetrics
import com.dangerfield.movingeyes.libraries.device.millimeters
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyeCanvas
import com.dangerfield.movingeyes.libraries.render.EyeSceneState
import com.dangerfield.movingeyes.libraries.render.RenderedEye
import com.dangerfield.movingeyes.libraries.ui.components.ReadoutPill
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import com.dangerfield.movingeyes.system.Target
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The canvas is the screen at 1:1 — no zoom, no pan, no insets. Black bleeds
 * to every edge, including under the notch, so on OLED the lit pixels are the
 * eyes and nothing else.
 *
 * The app opens straight onto this with two eyes already blinking. That
 * replaced onboarding entirely: nobody reads three cards before they've seen
 * the thing work, and the product explains itself in a second if it's already
 * moving when you arrive.
 *
 * Chrome floats *over* the canvas rather than laying it out, so the canvas
 * bounds never change when the toolbar or readout appears. If chrome ever
 * starts resizing the canvas, every alignment the user has done is silently
 * wrong — that is the failure this screen exists to avoid.
 */
@Composable
fun EditorScreen(
    screenMetrics: ScreenMetrics,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val haptics = LocalHapticFeedback.current

        val canvasWidthPx = with(density) { maxWidth.toPx() }
        val canvasHeightPx = with(density) { maxHeight.toPx() }
        val snapThresholdPx = with(density) { Motion.Snap.ThresholdDp.toPx() }
        val minimumTouchPx = with(density) { Target.Minimum.toPx() }

        val scene = remember(canvasWidthPx, canvasHeightPx) {
            startingScene(canvasWidthPx, canvasHeightPx)
        }
        val editor = remember(scene) { EditorState(scene.eyes) }

        var guides by remember { mutableStateOf<List<SnapGuide>>(emptyList()) }
        var wasSnapped by remember { mutableStateOf(false) }
        var dragSession by remember { mutableStateOf<DragSession?>(null) }

        EyeCanvas(
            state = scene,
            modifier = Modifier
                .fillMaxSize()
                .editorGestures(
                    enabled = !editor.isLocked,
                    onGestureStart = { dragSession = editor.beginGesture() },
                    onTap = { position ->
                        val hit = scene.eyes.hitTest(
                            x = position.x,
                            y = position.y,
                            canvasWidth = canvasWidthPx,
                            canvasHeight = canvasHeightPx,
                            minimumTouchPx = minimumTouchPx,
                        )
                        if (hit != null) editor.select(hit) else editor.clearSelection()
                    },
                    onDoubleTap = { editor.selectAll() },
                    onDrag = { pan ->
                        val snappedNow = dragSelection(
                            editor = editor,
                            session = dragSession,
                            pan = pan,
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            snapThresholdPx = snapThresholdPx,
                            onGuides = { guides = it },
                        )
                        // One light tick on capture and nothing on release. A
                        // tick in both directions turns a careful nudge into a
                        // buzzing mess.
                        if (snappedNow && !wasSnapped) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        wasSnapped = snappedNow
                    },
                    onTransform = { zoom, rotation, _ ->
                        transformSelection(editor, zoom, rotation)
                    },
                    onGestureEnd = {
                        // Guides never persist past touch-up. A guide left on
                        // screen stops being a hint and starts being clutter.
                        guides = emptyList()
                        wasSnapped = false
                        dragSession = null
                    },
                    onUndo = { editor.undo() },
                    onRedo = { editor.redo() },
                ),
            canvasColor = Color.Black,
        )

        SelectionOverlay(
            eyes = scene.eyes,
            selection = editor.selection,
            guides = guides,
            accent = AppTheme.colors.accentPrimary.color,
            revision = editor.transformRevision,
            modifier = Modifier.fillMaxSize(),
        )

        EditorChrome(
            editor = editor,
            readout = readoutText(
                editor = editor,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                screenMetrics = screenMetrics,
            ),
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        )
    }
}

@Composable
private fun EditorChrome(
    editor: EditorState,
    readout: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        ReadoutPill(
            text = readout,
            emphasized = editor.selection.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimension.D700),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Dimension.D700),
            horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
        ) {
            // Undo and redo live in the floating toolbar and never inside a
            // collapsible panel. Fat-finger destruction of a careful alignment
            // is the top rage-quit risk in this app, and the way back has to
            // be visible at the moment it happens.
            ToolbarButton(
                label = "Undo",
                enabled = editor.canUndo,
                onClick = { editor.undo() },
            )
            ToolbarButton(
                label = "Redo",
                enabled = editor.canRedo,
                onClick = { editor.redo() },
            )
            ToolbarButton(
                label = if (editor.isLocked) "Unlock" else "Lock",
                enabled = true,
                isActive = editor.isLocked,
                onClick = { editor.toggleLock() },
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    com.dangerfield.movingeyes.libraries.ui.components.button.Button(
        onClick = onClick,
        enabled = enabled,
        size = com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize.Small,
        style = if (isActive) {
            com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle.Filled
        } else {
            com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle.Outlined
        },
    ) {
        com.dangerfield.movingeyes.libraries.ui.components.text.Text(label)
    }
}

/**
 * Moves the active eyes, snapping the *first* of them and carrying the rest
 * along rigidly.
 *
 * Snapping one member of a multi-selection rather than each independently is
 * what keeps a pair a pair: eyes that each snapped to their own nearest guide
 * would drift apart mid-drag, destroying a spacing the user had already set.
 */
private fun dragSelection(
    editor: EditorState,
    session: DragSession?,
    pan: Offset,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    snapThresholdPx: Float,
    onGuides: (List<SnapGuide>) -> Unit,
): Boolean {
    val active = editor.activeIndices()
    if (active.isEmpty() || session == null) return false

    session.accumulate(pan.x / canvasWidthPx, pan.y / canvasHeightPx)

    // Every eye is placed from its own start plus the raw finger travel, so a
    // snap applied last frame can't feed back into this one.
    active.forEach { index ->
        val raw = session.rawPosition(index) ?: return@forEach
        editor.eyes[index].apply {
            centerX = raw.first.coerceIn(0f, 1f)
            centerY = raw.second.coerceIn(0f, 1f)
        }
    }

    val leadIndex = active.first()
    val lead = editor.eyes[leadIndex]
    val result = resolveSnap(
        dragged = SnapCandidate(
            id = leadIndex,
            center = CanvasPoint(lead.centerX * canvasWidthPx, lead.centerY * canvasHeightPx),
        ),
        others = editor.eyes.indices
            .filter { it !in active }
            .map { index ->
                val eye = editor.eyes[index]
                SnapCandidate(
                    id = index,
                    center = CanvasPoint(eye.centerX * canvasWidthPx, eye.centerY * canvasHeightPx),
                )
            },
        canvasWidth = canvasWidthPx,
        canvasHeight = canvasHeightPx,
        thresholdPx = snapThresholdPx,
    )

    if (result.snapped) {
        val correctionX = result.position.x / canvasWidthPx - lead.centerX
        val correctionY = result.position.y / canvasHeightPx - lead.centerY
        active.forEach { index ->
            editor.eyes[index].apply {
                centerX += correctionX
                centerY += correctionY
            }
        }
    }

    onGuides(result.guides)
    editor.transformChanged()
    return result.snapped
}

/**
 * Pinch scales, twist rotates. Rotation snaps to 15° detents but holds any
 * angle if you keep turning past one, because a picture rail is sometimes at
 * 7°.
 */
private fun transformSelection(editor: EditorState, zoom: Float, rotation: Float) {
    val active = editor.activeIndices()
    if (active.isEmpty()) return

    active.forEach { index ->
        editor.eyes[index].apply {
            sizePx = (sizePx * zoom).coerceIn(MinEyeSizePx, MaxEyeSizePx)
            rotationDegrees = snapRotation(rotationDegrees + rotation)
        }
    }
    editor.transformChanged()
}

/**
 * The live measurement, in the mono readout.
 *
 * Millimetres appear only when the platform actually knows the screen's
 * physical size. Someone is going to hold a ruler against cardboard and cut
 * from this number, so a figure the app can't stand behind is worse than no
 * figure at all.
 */
@Composable
private fun readoutText(
    editor: EditorState,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    screenMetrics: ScreenMetrics,
): String {
    // Read so the readout recomposes as a drag moves eyes that aren't Compose
    // state.
    @Suppress("UNUSED_EXPRESSION")
    editor.transformRevision

    val selection = editor.selection
    if (selection.isEmpty()) {
        return "${editor.eyes.size} eyes · tap to select"
    }

    val parts = mutableListOf("${selection.size} ${if (selection.size == 1) "eye" else "eyes"}")

    if (selection.size == 1) {
        val eye = editor.eyes[selection.first()]
        parts += "X ${(eye.centerX * canvasWidthPx).roundToInt()}"
        parts += "Y ${(eye.centerY * canvasHeightPx).roundToInt()}"
        parts += "${eye.sizePx.roundToInt()} px"
    } else if (selection.size == 2) {
        val a = editor.eyes[selection[0]]
        val b = editor.eyes[selection[1]]
        val distance = hypot(
            (b.centerX - a.centerX) * canvasWidthPx,
            (b.centerY - a.centerY) * canvasHeightPx,
        )
        parts += "IPD ${distance.roundToInt()} px"
        screenMetrics.millimeters(distance)?.let { mm ->
            parts += "${((mm * 10).roundToInt() / 10f)} mm"
        }
    }

    val rotation = editor.eyes[selection.first()].rotationDegrees
    parts += "${rotation.roundToInt()}°"

    return parts.joinToString(" · ")
}

/**
 * A pair of Human Basic eyes on the free motion default. Deliberately the
 * plainest thing the app can show: the first frame should look like a real
 * pair of eyes, not a demo of the spookiest style available.
 */
private fun startingScene(canvasWidthPx: Float, canvasHeightPx: Float): EyeSceneState {
    // Sized off the canvas rather than a fixed dp, because "two eyes" has to
    // look like a pair on a 5" phone and on a 13" tablet.
    val eyeWidth = min(canvasWidthPx, canvasHeightPx) * StartingEyeWidthFraction
    val separation = eyeWidth * StartingSeparationInEyeWidths
    val offset = (separation / 2f) / canvasWidthPx

    return EyeSceneState(
        eyes = listOf(
            eye(x = 0.5f - offset, sizePx = eyeWidth, seed = 1),
            eye(x = 0.5f + offset, sizePx = eyeWidth, seed = 2),
        ),
    )
}

private fun eye(x: Float, sizePx: Float, seed: Int) = RenderedEye(
    style = EyeStyles.HumanBasic,
    centerX = x,
    centerY = 0.5f,
    sizePx = sizePx,
    behavior = Moods.FreeDefault,
    // A distinct seed per eye, so the pair never blinks in lockstep. Same rule
    // everywhere eyes are drawn, which is why it's a constructor argument
    // rather than a default.
    random = Random(seed),
)

private const val StartingEyeWidthFraction = 0.30f

/**
 * Distance between pupils, in eye widths. Anatomically a face is nearer 2.6,
 * but that only reads right when the eyes are small relative to the screen; at
 * a size visible across a room, a tighter pair looks like a face and an
 * anatomical one looks like two separate things.
 */
private const val StartingSeparationInEyeWidths = 1.35f

private val MinEyeSizePx = 24f
private val MaxEyeSizePx = 2000f
