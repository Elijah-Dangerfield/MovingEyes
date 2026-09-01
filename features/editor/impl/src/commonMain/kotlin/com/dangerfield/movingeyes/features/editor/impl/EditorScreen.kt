@file:Suppress("MagicNumber", "LongMethod")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.movingeyes.features.editor.impl.panels.LookPanel
import com.dangerfield.movingeyes.features.editor.impl.panels.MotionPanel
import com.dangerfield.movingeyes.features.editor.impl.panels.PanelTab
import com.dangerfield.movingeyes.features.editor.impl.panels.PlacePanel
import com.dangerfield.movingeyes.features.editor.impl.panels.ScenePanel
import com.dangerfield.movingeyes.libraries.billing.DemoControl
import com.dangerfield.movingeyes.libraries.device.ScreenMetrics
import com.dangerfield.movingeyes.libraries.device.millimeters
import com.dangerfield.movingeyes.libraries.render.EyeCanvas
import com.dangerfield.movingeyes.libraries.render.EyeSceneState
import com.dangerfield.movingeyes.libraries.render.toRenderedEyes
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.ScenePreset
import com.dangerfield.movingeyes.libraries.scene.ScenePresets
import com.dangerfield.movingeyes.libraries.ui.components.AdaptivePanel
import com.dangerfield.movingeyes.libraries.ui.components.ReadoutPill
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.ToastAction
import com.dangerfield.movingeyes.libraries.ui.components.ToastBar
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import com.dangerfield.movingeyes.system.Target
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.demo_countdown
import movingeyes.libraries.resources.generated.resources.demo_ended
import movingeyes.libraries.resources.generated.resources.demo_keep
import movingeyes.libraries.resources.generated.resources.editor_eye_count
import movingeyes.libraries.resources.generated.resources.editor_lock
import movingeyes.libraries.resources.generated.resources.editor_readout_hint
import movingeyes.libraries.resources.generated.resources.editor_readout_ipd
import movingeyes.libraries.resources.generated.resources.editor_readout_millimeters
import movingeyes.libraries.resources.generated.resources.editor_readout_rotation
import movingeyes.libraries.resources.generated.resources.editor_readout_size
import movingeyes.libraries.resources.generated.resources.editor_readout_x
import movingeyes.libraries.resources.generated.resources.editor_readout_y
import movingeyes.libraries.resources.generated.resources.editor_redo
import movingeyes.libraries.resources.generated.resources.editor_undo
import movingeyes.libraries.resources.generated.resources.editor_unlock
import movingeyes.libraries.resources.generated.resources.panel_look
import movingeyes.libraries.resources.generated.resources.panel_motion
import movingeyes.libraries.resources.generated.resources.panel_place
import movingeyes.libraries.resources.generated.resources.panel_scene
import movingeyes.libraries.resources.generated.resources.scenes_default_name
import movingeyes.libraries.resources.generated.resources.scenes_open
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The canvas is the screen at 1:1 — no zoom, no pan, no insets. Black bleeds
 * to every edge, including under the notch, so on OLED the lit pixels are the
 * eyes and nothing else.
 *
 * The app opens straight onto this with eyes already blinking. That replaced
 * onboarding entirely: nobody reads three cards before they've seen the thing
 * work, and the product explains itself in a second if it's already moving when
 * you arrive.
 *
 * Chrome floats *over* the canvas rather than laying it out, so the canvas
 * bounds never change when the toolbar, readout or panel appears. If chrome
 * ever starts resizing the canvas, every alignment the user has done is
 * silently wrong — that is the failure this screen exists to avoid.
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    screenMetrics: ScreenMetrics,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.entitlements.isUnlocked.collectAsStateWithLifecycle()
    val activeTrial by viewModel.featureTrial.active.collectAsStateWithLifecycle()
    val justEnded by viewModel.featureTrial.justEnded.collectAsStateWithLifecycle()

    // Nothing is drawn until the autosave lookup answers. Showing a default
    // pair first and swapping it a frame later would flash two eyes the user
    // never placed, over the top of the scene they left.
    if (!state.isLoaded) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val haptics = LocalHapticFeedback.current

        val displayWidthPx = with(density) { maxWidth.toPx() }
        val displayHeightPx = with(density) { maxHeight.toPx() }
        val snapThresholdPx = with(density) { Motion.Snap.ThresholdDp.toPx() }
        val minimumTouchPx = with(density) { Target.Minimum.toPx() }

        // Re-keyed on the open scene as well as the display size, so opening a
        // scene from the drawer rebuilds the eyes rather than mutating the ones
        // already on screen.
        //
        // Keyed on the *display* size rather than the scene size on purpose: a
        // canvas turn changes the scene's dimensions, and re-keying on those
        // would rebuild the editor and throw away everything unsaved every time
        // someone tried a different mounting orientation.
        val openScene = state.openScene
        val editor = remember(openScene, displayWidthPx, displayHeightPx) {
            val scene = openScene ?: blankScene()
            EditorState(
                eyes = scene.toRenderedEyes(displayWidthPx, displayHeightPx),
                moods = scene.eyes.map { it.mood },
                canvas = CanvasSettings(
                    rotation = scene.canvasRotation,
                    color = scene.canvasColor,
                    brightness = scene.brightness,
                ),
            )
        }
        val sceneState = remember(editor) { EyeSceneState(eyes = editor.eyes) }

        // A quarter turn swaps what "across" and "down" mean. Everything that
        // works in scene coordinates — gestures, snapping, the readout — has to
        // use these rather than the display's own dimensions. Eye *sizes* are a
        // fraction of the short edge, which is the same number either way, so
        // they're deliberately untouched by a turn.
        val turned = editor.canvas.rotation.swapsAxes
        val canvasWidthPx = if (turned) displayHeightPx else displayWidthPx
        val canvasHeightPx = if (turned) displayWidthPx else displayHeightPx
        val shortEdgePx = min(canvasWidthPx, canvasHeightPx)

        var guides by remember { mutableStateOf<List<SnapGuide>>(emptyList()) }
        var wasSnapped by remember { mutableStateOf(false) }
        var dragSession by remember { mutableStateOf<DragSession?>(null) }
        var snappingEnabled by remember { mutableStateOf(true) }
        var panelExpanded by remember { mutableStateOf(true) }
        var tab by remember { mutableStateOf(PanelTab.Place) }
        var drawerOpen by remember { mutableStateOf(false) }

        // Read here rather than in the save handler: a string resource can only
        // be read from a composable, and the handler isn't one.
        val fallbackSceneName = stringResource(Res.string.scenes_default_name)

        fun autosave() {
            viewModel.takeAction(
                EditorAction.Autosave(
                    editor.toScene(
                        id = "autosave",
                        name = openScene?.name.orEmpty(),
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                    ),
                ),
            )
        }

        // The canvas turn is a mounting decision, so it rotates the whole
        // composition — eyes and selection overlay together — rather than being
        // baked into every stored coordinate. Gestures land inside this layer,
        // so Compose hands them back already in scene space.
        //
        // At 90° and 270° the layer takes the *swapped* dimensions before
        // rotating, so the turned scene still fills the screen exactly. Rotating
        // a portrait-sized layer instead would crop the sides and letterbox the
        // ends, which on a device taped behind cardboard means eyes off the edge
        // of the visible area.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                // `requiredSize`, not `size`: a turned canvas is wider than the
                // screen, and `size` is coerced into the parent's constraints,
                // which silently clamps it back to the display width and lays
                // the scene out at the wrong scale.
                .requiredSize(
                    width = if (turned) maxHeight else maxWidth,
                    height = if (turned) maxWidth else maxHeight,
                )
                .graphicsLayer { rotationZ = editor.canvas.rotation.degrees.toFloat() },
        ) {
            EyeCanvas(
                state = sceneState,
                modifier = Modifier
                    .fillMaxSize()
                    .editorGestures(
                        enabled = !editor.isLocked,
                        onGestureStart = { dragSession = editor.beginGesture() },
                        onTap = { position ->
                            val hit = editor.eyes.hitTest(
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
                                snapThresholdPx = if (snappingEnabled) snapThresholdPx else 0f,
                                onGuides = { guides = it },
                            )
                            // One light tick on capture and nothing on release.
                            // A tick in both directions turns a careful nudge
                            // into a buzzing mess.
                            if (snappedNow && !wasSnapped) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            wasSnapped = snappedNow
                        },
                        onTransform = { zoom, rotation, _ ->
                            transformSelection(editor, zoom, rotation)
                        },
                        onGestureEnd = {
                            // Guides never persist past touch-up. A guide left
                            // on screen stops being a hint and starts being
                            // clutter.
                            guides = emptyList()
                            wasSnapped = false
                            dragSession = null
                            autosave()
                        },
                        onUndo = { editor.undo() },
                        onRedo = { editor.redo() },
                    ),
                canvasColor = Color(editor.canvas.color),
            )

            SelectionOverlay(
                eyes = editor.eyes,
                selection = editor.selection,
                guides = guides,
                accent = AppTheme.colors.accentPrimary.color,
                revision = editor.transformRevision,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Software dim, layered over the canvas and under the chrome. This is
        // what lets the scene go below the OS brightness floor, which is the
        // single trick that makes it look right in a dark hallway.
        if (editor.canvas.brightness < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1f - editor.canvas.brightness)),
            )
        }

        EditorChrome(
            editor = editor,
            readout = readoutText(
                editor = editor,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                screenMetrics = screenMetrics,
            ),
            onOpenScenes = { drawerOpen = true },
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        )

        AdaptivePanel(
            expanded = panelExpanded,
            onExpandedChange = { panelExpanded = it },
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimension.D700)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimension.D600),
            ) {
                // Resolved up front: SegmentedControl's label is a plain
                // function, not a composable one.
                val tabLabels = PanelTab.entries.associateWith { stringResource(it.label) }
                SegmentedControl(
                    options = PanelTab.entries,
                    selected = tab,
                    onSelect = { tab = it },
                    label = { tabLabels.getValue(it) },
                )

                when (tab) {
                    PanelTab.Place -> PlacePanel(
                        editor = editor,
                        canvasWidthPx = canvasWidthPx,
                        canvasHeightPx = canvasHeightPx,
                        snappingEnabled = snappingEnabled,
                        onSnappingChange = { snappingEnabled = it },
                    )

                    PanelTab.Look -> LookPanel(
                        editor = editor,
                        isUnlocked = isUnlocked,
                        onLockedStyleTapped = { style ->
                            demo(viewModel, DemoControl.EyeStyle) {
                                val before = editor.snapshot()
                                editor.setStyle(style)
                                return@demo { editor.restore(before) }
                            }
                        },
                        shortEdgePx = shortEdgePx,
                    )

                    PanelTab.Motion -> MotionPanel(
                        editor = editor,
                        isUnlocked = isUnlocked,
                        onLockedControl = { control, apply ->
                            demo(viewModel, control) {
                                val before = editor.snapshot()
                                apply()
                                return@demo { editor.restore(before) }
                            }
                        },
                    )

                    PanelTab.Scene -> ScenePanel(
                        editor = editor,
                        onSave = {
                            viewModel.takeAction(
                                EditorAction.Save(
                                    editor.toScene(
                                        id = "",
                                        name = openScene?.name?.takeIf { it.isNotBlank() }
                                            ?: fallbackSceneName,
                                        canvasWidthPx = canvasWidthPx,
                                        canvasHeightPx = canvasHeightPx,
                                    ),
                                ),
                            )
                        },
                    )
                }
            }
        }

        // Sits below the toolbar rather than beside it. Centred at the same
        // height it would collide with Undo on a phone, and Undo is the one
        // control that must never be obscured.
        DemoCountdown(
            remainingSeconds = activeTrial?.remaining?.inWholeSeconds?.toInt(),
            isUrgent = activeTrial?.isUrgent == true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(top = ToolbarClearance),
        )

        // No dimming and no modal on expiry. The bar can be ignored, and it
        // goes away on its own — see FeatureTrial.
        val ended = justEnded
        ToastBar(
            visible = ended != null,
            message = stringResource(Res.string.demo_ended),
            onDismiss = { },
            actions = ended?.let { control ->
                listOf(
                    ToastAction(
                        label = stringResource(Res.string.demo_keep, stringResource(control.label)),
                        onSelect = { /* Paywall lands in Phase 7. */ },
                    ),
                )
            }.orEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(Dimension.D700),
        )

        if (drawerOpen) {
            ScenesDrawer(
                saved = state.savedScenes,
                onOpenScene = {
                    viewModel.takeAction(EditorAction.Open(it))
                    drawerOpen = false
                },
                onOpenPreset = { preset ->
                    viewModel.takeAction(
                        EditorAction.Open(preset.toScene(id = "preset", name = "")),
                    )
                    drawerOpen = false
                },
                onDeleteScene = { viewModel.takeAction(EditorAction.Delete(it)) },
                onNewBlank = {
                    viewModel.takeAction(EditorAction.Open(blankScene()))
                    drawerOpen = false
                },
                onDismiss = { drawerOpen = false },
                isUnlocked = isUnlocked,
            )
        }
    }
}

/**
 * Start a demo, or fall through to the paywall once this control's demo is
 * spent. [apply] performs the change and returns the undo for it, so the
 * caller states the change and its reversal in one place and they can't drift.
 */
private fun demo(
    viewModel: EditorViewModel,
    control: DemoControl,
    apply: () -> (() -> Unit),
) {
    if (!viewModel.featureTrial.isAvailable(control)) {
        // Phase 7 opens the paywall here. Until then a spent demo simply does
        // nothing, which is the correct *gating* behaviour — it just doesn't
        // yet offer the way to buy.
        return
    }
    val revert = apply()
    viewModel.featureTrial.start(control, revert)
}

@Composable
private fun EditorChrome(
    editor: EditorState,
    readout: String,
    onOpenScenes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Button(
            onClick = onOpenScenes,
            size = ButtonSize.Small,
            style = ButtonStyle.Outlined,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Dimension.D700),
        ) {
            Text(stringResource(Res.string.scenes_open))
        }

        // Lifted clear of the panel's collapsed grab edge. The readout is the
        // number someone cuts cardboard from, so it must never be half-hidden
        // behind a handle.
        ReadoutPill(
            text = readout,
            emphasized = editor.selection.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Dimension.D700, end = Dimension.D700, bottom = Dimension.D700)
                .padding(bottom = Motion.Panel.GrabEdgeDp),
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
                label = stringResource(Res.string.editor_undo),
                enabled = editor.canUndo,
                onClick = { editor.undo() },
            )
            ToolbarButton(
                label = stringResource(Res.string.editor_redo),
                enabled = editor.canRedo,
                onClick = { editor.redo() },
            )
            ToolbarButton(
                label = stringResource(
                    if (editor.isLocked) Res.string.editor_unlock else Res.string.editor_lock,
                ),
                enabled = true,
                isActive = editor.isLocked,
                onClick = { editor.toggleLock() },
            )
        }
    }
}

/** The amber pill. Mono, because it's a number that changes every second and
 *  a proportional face would make it jitter. */
@Composable
private fun DemoCountdown(
    remainingSeconds: Int?,
    isUrgent: Boolean,
    modifier: Modifier = Modifier,
) {
    if (remainingSeconds == null) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(AppTheme.colors.accentPrimary.color.copy(alpha = if (isUrgent) 1f else 0.85f))
            .padding(horizontal = Dimension.D500, vertical = Dimension.D300),
    ) {
        Text(
            text = stringResource(Res.string.demo_countdown, remainingSeconds),
            typography = AppTheme.typography.Readout.R400,
            color = AppTheme.colors.onAccentPrimary,
        )
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isActive: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        size = ButtonSize.Small,
        style = if (isActive) ButtonStyle.Filled else ButtonStyle.Outlined,
    ) {
        Text(label)
    }
}

private val PanelTab.label
    get() = when (this) {
        PanelTab.Place -> Res.string.panel_place
        PanelTab.Look -> Res.string.panel_look
        PanelTab.Motion -> Res.string.panel_motion
        PanelTab.Scene -> Res.string.panel_scene
    }

/**
 * What the app opens on before anything is saved. See `ScenePresets.blank` for
 * why it's the plainest thing the app can show rather than the spookiest.
 */
private fun blankScene() = Scene(
    id = "autosave",
    name = "",
    eyes = ScenePresets.blank(),
)

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
    val eyeCount = pluralStringResource(Res.plurals.editor_eye_count, editor.eyes.size, editor.eyes.size)
    if (selection.isEmpty()) {
        return stringResource(Res.string.editor_readout_hint, eyeCount)
    }

    val parts = mutableListOf(
        pluralStringResource(Res.plurals.editor_eye_count, selection.size, selection.size),
    )

    if (selection.size == 1) {
        val eye = editor.eyes[selection.first()]
        parts += stringResource(Res.string.editor_readout_x, (eye.centerX * canvasWidthPx).roundToInt())
        parts += stringResource(Res.string.editor_readout_y, (eye.centerY * canvasHeightPx).roundToInt())
        parts += stringResource(Res.string.editor_readout_size, eye.sizePx.roundToInt())
    } else {
        editor.selectedSpacingPx(canvasWidthPx, canvasHeightPx)?.let { distance ->
            parts += stringResource(Res.string.editor_readout_ipd, distance.roundToInt())
            screenMetrics.millimeters(distance)?.let { mm ->
                parts += stringResource(
                    Res.string.editor_readout_millimeters,
                    ((mm * 10).roundToInt() / 10f).toString(),
                )
            }
        }
    }

    val rotation = editor.eyes[selection.first()].rotationDegrees
    parts += stringResource(Res.string.editor_readout_rotation, rotation.roundToInt())

    return parts.joinToString(" · ")
}

/** Clears the floating toolbar row so the countdown can't sit on top of it. */
private val ToolbarClearance = 88.dp

private const val MinEyeSizePx = 24f
private const val MaxEyeSizePx = 2000f
