@file:Suppress("MagicNumber", "LongMethod")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.dangerfield.movingeyes.libraries.ui.fadingEdge
import androidx.compose.foundation.layout.size
import com.dangerfield.movingeyes.libraries.ui.components.icon.Icon
import com.dangerfield.movingeyes.libraries.ui.components.icon.IconSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.movingeyes.features.editor.impl.panels.LookPanel
import com.dangerfield.movingeyes.features.editor.impl.panels.MotionPanel
import com.dangerfield.movingeyes.features.editor.impl.panels.PanelTab
import com.dangerfield.movingeyes.features.editor.impl.panels.PlacePanel
import com.dangerfield.movingeyes.features.editor.impl.panels.ScenePanel
import com.dangerfield.movingeyes.features.paywall.PaywallRoute
import com.dangerfield.movingeyes.features.paywall.PaywallTrigger
import com.dangerfield.movingeyes.features.settings.SettingsRoute
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.eyes.isStrobing
import com.dangerfield.movingeyes.libraries.navigation.Router
import com.dangerfield.movingeyes.libraries.device.ScreenMetrics
import com.dangerfield.movingeyes.libraries.device.dimLevelsFor
import com.dangerfield.movingeyes.libraries.device.millimeters
import com.dangerfield.movingeyes.libraries.render.EyeCanvas
import com.dangerfield.movingeyes.libraries.render.EyeSceneState
import com.dangerfield.movingeyes.libraries.render.RenderedEye
import com.dangerfield.movingeyes.libraries.render.toRenderedEyes
import com.dangerfield.movingeyes.libraries.scene.Scene
import com.dangerfield.movingeyes.libraries.scene.ScenePreset
import com.dangerfield.movingeyes.libraries.scene.ScenePresets
import com.dangerfield.movingeyes.libraries.ui.components.AdaptivePanel
import com.dangerfield.movingeyes.libraries.ui.components.HorizontalDivider
import com.dangerfield.movingeyes.libraries.ui.components.icon.IconButton
import com.dangerfield.movingeyes.libraries.ui.components.icon.Icons
import com.dangerfield.movingeyes.libraries.ui.components.rememberPanelState
import com.dangerfield.movingeyes.libraries.ui.components.ReadoutPill
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.Motion
import com.dangerfield.movingeyes.system.Target
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.display_enter
import movingeyes.libraries.resources.generated.resources.editor_eye_count
import movingeyes.libraries.resources.generated.resources.editor_readout_ipd
import movingeyes.libraries.resources.generated.resources.editor_readout_millimeters
import movingeyes.libraries.resources.generated.resources.editor_readout_rotation
import movingeyes.libraries.resources.generated.resources.editor_readout_size
import movingeyes.libraries.resources.generated.resources.editor_readout_x
import movingeyes.libraries.resources.generated.resources.editor_readout_y
import movingeyes.libraries.resources.generated.resources.editor_redo
import movingeyes.libraries.resources.generated.resources.editor_controls
import movingeyes.libraries.resources.generated.resources.editor_measure
import movingeyes.libraries.resources.generated.resources.editor_undo
import movingeyes.libraries.resources.generated.resources.panel_look
import movingeyes.libraries.resources.generated.resources.panel_motion
import movingeyes.libraries.resources.generated.resources.panel_place
import movingeyes.libraries.resources.generated.resources.place_add_eye
import movingeyes.libraries.resources.generated.resources.place_delete
import movingeyes.libraries.resources.generated.resources.place_duplicate
import movingeyes.libraries.resources.generated.resources.panel_scene
import movingeyes.libraries.resources.generated.resources.scenes_default_name
import movingeyes.libraries.resources.generated.resources.scenes_open
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The canvas is the screen at 1:1 — no zoom, no pan, no insets — and the app
 * opens straight onto it with eyes already blinking, which is what replaced
 * onboarding.
 *
 * Chrome floats *over* the canvas rather than laying it out. If chrome ever
 * starts resizing it, every alignment the user has done is silently wrong.
 */
// BackHandler is the only supported way to intercept the system back gesture,
// which is the sole exit from display mode.
@OptIn(ExperimentalComposeUiApi::class, FlowPreview::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    screenMetrics: ScreenMetrics,
    router: Router,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.entitlements.isUnlocked.collectAsStateWithLifecycle()

    // Drawing a default pair first would flash two eyes the user never placed.
    if (!state.isLoaded) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current

        val displayWidthPx = with(density) { maxWidth.toPx() }
        val displayHeightPx = with(density) { maxHeight.toPx() }
        val snapThresholdPx = with(density) { Motion.Snap.ThresholdDp.toPx() }
        val minimumTouchPx = with(density) { Target.Minimum.toPx() }
        val handleTouchPx = minimumTouchPx / 2f
        val rotateGapPx = with(density) { RotateHandleGap.toPx() }

        // Keyed on the *display* size, not the scene size: a canvas turn
        // changes the scene's dimensions, and re-keying on those would discard
        // everything unsaved each time someone tried a mounting orientation.
        val openScene = state.openScene
        // One director for the scene, so every eye looks at the same thing.
        val gaze = remember(openScene) {
            SceneDirector((openScene ?: blankScene("")).eyes.firstOrNull()?.behavior() ?: Moods.FreeDefault)
        }
        val editor = remember(openScene, displayWidthPx, displayHeightPx) {
            val scene = openScene ?: blankScene("")
            EditorState(
                gaze = gaze,
                eyes = scene.toRenderedEyes(displayWidthPx, displayHeightPx, gaze),
                moods = scene.eyes.map { it.mood },
                canvas = CanvasSettings(
                    rotation = scene.canvasRotation,
                    color = scene.canvasColor,
                    brightness = scene.brightness,
                    sleepTimer = scene.sleepTimerMinutes?.minutes,
                    reactivityEnabled = scene.reactivityEnabled,
                    blinkTogether = scene.blinkTogether,
                ),
            )
        }
        val sceneState = remember(editor) { EyeSceneState(eyes = editor.eyes, gaze = gaze) }

        // Pushed rather than passed at construction: the toggle has to take
        // effect on a running scene, not only on the next one built.
        gaze.blinksTogether = editor.canvas.blinkTogether

        // A quarter turn swaps what "across" and "down" mean, so gestures,
        // snapping and the readout all work in these rather than the display's
        // own dimensions. Sizes are a short-edge fraction and so unaffected.
        val turned = editor.canvas.rotation.swapsAxes
        val canvasWidthPx = if (turned) displayHeightPx else displayWidthPx
        val canvasHeightPx = if (turned) displayWidthPx else displayHeightPx
        val shortEdgePx = min(canvasWidthPx, canvasHeightPx)

        var guides by remember { mutableStateOf<List<SnapGuide>>(emptyList()) }
        var wasSnapped by remember { mutableStateOf(false) }
        var dragSession by remember { mutableStateOf<DragSession?>(null) }
        var isManipulating by remember { mutableStateOf(false) }
        var isOverTrash by remember { mutableStateOf(false) }
        var isRenaming by remember { mutableStateOf(false) }
        var showMeasurements by remember { mutableStateOf(false) }
        var snappingEnabled by remember { mutableStateOf(true) }
        val panel = rememberPanelState()
        var tab by remember { mutableStateOf(PanelTab.Place) }
        var drawerOpen by remember { mutableStateOf(false) }

        val display = remember(editor) {
            DisplayModeState(sleepTimer = editor.canvas.sleepTimer)
        }
        display.sleepTimer = editor.canvas.sleepTimer

        DisplayModeEffects(
            state = display,
            brightness = editor.canvas.brightness,
            displayController = viewModel.displayController,
            batteryStatus = viewModel.batteryStatus,
            onSessionEnded = { viewModel.takeAction(EditorAction.DisplaySessionEnded(it)) },
        )

        BackHandler(enabled = display.isActive) { display.exit() }

        // The scene shrinks into whatever the panel leaves rather than hiding
        // behind it, driven by the panel's own live position so the two move
        // together frame for frame. Normalised coordinates are untouched — this
        // is a preview scale, not a change to the composition — and sliding the
        // panel away returns it to true 1:1, which is what the mm readout
        // describes.
        val occupied = if (display.isActive) 0f else panel.occupiedPx
        val isRail = maxWidth >= RailBreakpointDp
        // No inset while the panel is away: the resting editor is the scene at
        // full size, edge to edge, with nothing charging rent on it.
        val insetPx = if (panel.isVisible) with(density) { CanvasInset.toPx() } * 2f else 0f
        // Display mode is exactly 1:1 with no inset and no edge. A percent of
        // scale there would make every millimetre on screen a lie.
        val canvasScale = if (display.isActive) {
            1f
        } else {
            minOf(
                1f,
                (displayWidthPx - (if (isRail) occupied else 0f) - insetPx) / displayWidthPx,
                (displayHeightPx - (if (isRail) 0f else occupied) - insetPx) / displayHeightPx,
            ).coerceAtLeast(MinCanvasScale)
        }
        val isScaled = !display.isActive && canvasScale < 0.999f
        val shiftX = if (isRail) -occupied / 2f else 0f
        val shiftY = if (isRail) 0f else -occupied / 2f

        // Read here rather than in the save handler: a string resource can only
        // be read from a composable, and the handler isn't one.
        val fallbackSceneName = stringResource(Res.string.scenes_default_name)

        editor.reduceFlashing = state.reduceFlashing

        // A strobing mood waits behind its warning until the user has chosen.
        var pendingStrobingMood by remember { mutableStateOf<Mood?>(null) }
        var showMicExplanation by remember { mutableStateOf(false) }
        var showMicDenied by remember { mutableStateOf(false) }

        LaunchedEffect(viewModel) {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    EditorEvent.MicrophoneGranted -> editor.setReactivityEnabled(true)
                    EditorEvent.MicrophoneUnavailable -> {
                        editor.setReactivityEnabled(false)
                        showMicDenied = true
                    }
                    else -> Unit
                }
            }
        }

        // Runs wherever the setting is on, editor included. It used to wait for
        // display mode, which meant switching it on did nothing you could see
        // and there was no way to tell a working microphone from a broken one
        // without taping the tablet to a wall first. The user has just asked
        // for this and granted the permission; the OS shows its own mic
        // indicator, so nothing about it is secret.
        val reactivityWanted = editor.canvas.reactivityEnabled
        LaunchedEffect(reactivityWanted) {
            viewModel.takeAction(
                if (reactivityWanted) EditorAction.StartReactivity else EditorAction.StopReactivity,
            )
        }

        LaunchedEffect(reactivityWanted) {
            if (!reactivityWanted) return@LaunchedEffect
            viewModel.reactivity.events.collect { event ->
                sceneState.startleAll(event.direction, event.intensity)
            }
        }

        var lockedItem by remember { mutableStateOf<LockedItem?>(null) }

        fun enableReactivity(enabled: Boolean) {
            if (!enabled) {
                editor.setReactivityEnabled(false)
                return
            }
            if (viewModel.needsMicrophoneExplanation()) {
                showMicExplanation = true
            } else {
                editor.setReactivityEnabled(true)
            }
        }

        // Every route to a mood ends here, so neither the paywall gate nor the
        // photosensitivity warning can be stepped around.
        fun applyMood(mood: Mood, warned: Boolean = false) {
            if (!warned && mood.isStrobing && mood.name !in state.moodsWarnedAbout &&
                !state.reduceFlashing
            ) {
                pendingStrobingMood = mood
                return
            }
            // Picking the free default is never a paid action: a free user must
            // be able to get back to where they started.
            if (isUnlocked || mood in Moods.Free) {
                editor.setMood(mood)
            } else {
                lockedItem = LockedItem.Mood(mood)
            }
        }

        // Watches every edit rather than only gesture ends, so panel changes —
        // a colour, a mood, the sleep timer — survive a kill too. Debounced, so
        // a slider drag writes once when it stops rather than per frame.
        LaunchedEffect(editor, openScene) {
            snapshotFlow { editor.transformRevision to editor.canvas }
                .drop(1)
                .debounce(AutosaveDebounce)
                .collect {
                    viewModel.takeAction(
                        EditorAction.Autosave(
                            editor.toScene(
                                id = openScene?.id.orEmpty(),
                                name = openScene?.name?.takeIf { it.isNotBlank() }
                                    ?: fallbackSceneName,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                            ),
                        ),
                    )
                }
        }

        // Rotates eyes and overlay together rather than baking degrees into
        // stored coordinates. Gestures land inside this layer, so Compose hands
        // them back already in scene space.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                // `requiredSize`, not `size`: a turned canvas is wider than the
                // screen, and `size` is coerced into the parent's constraints.
                .requiredSize(
                    width = if (turned) maxHeight else maxWidth,
                    height = if (turned) maxWidth else maxHeight,
                )
                .graphicsLayer {
                    rotationZ = editor.canvas.rotation.degrees.toFloat()
                    scaleX = canvasScale
                    scaleY = canvasScale
                    translationX = shiftX
                    translationY = shiftY
                },
        ) {
            EyeCanvas(
                state = sceneState,
                modifier = Modifier
                    .fillMaxSize()
                    .editorGestures(
                        enabled = !display.isActive,
                        handleAt = { position ->
                            selectionBounds(
                                eyes = editor.eyes,
                                selection = editor.selection,
                                canvasWidth = canvasWidthPx,
                                canvasHeight = canvasHeightPx,
                            )?.let { bounds ->
                                handleAt(position, bounds, handleTouchPx, rotateGapPx)
                            }
                        },
                        onGestureStart = {
                            dragSession = editor.beginGesture()
                            isManipulating = true
                        },
                        onTap = { position ->
                            val hit = editor.eyes.hitTest(
                                x = position.x,
                                y = position.y,
                                canvasWidth = canvasWidthPx,
                                canvasHeight = canvasHeightPx,
                                minimumTouchPx = minimumTouchPx,
                            )
                            when {
                                hit == null -> {
                                    editor.clearSelection()
                                    scope.launch { panel.hide() }
                                }
                                // Tapping an already-selected eye is asking
                                // about that eye, so it opens the inspector.
                                editor.isSelected(hit) -> {
                                    tab = PanelTab.Look
                                    scope.launch { panel.expand() }
                                }
                                else -> {
                                    editor.select(hit)
                                    scope.launch { panel.show() }
                                }
                            }
                        },
                        onDoubleTap = { editor.selectAll() },
                        onDrag = { pan ->
                            val snappedNow = dragSelection(
                                editor = editor,
                                session = dragSession,
                                pan = pan,
                                canvasWidthPx = canvasWidthPx,
                                canvasHeightPx = canvasHeightPx,
                                snapThresholdPx = if (snappingEnabled) {
                                    snapThresholdPx / canvasScale
                                } else {
                                    0f
                                },
                                onGuides = { guides = it },
                            )
                            val overTrashNow = isOverTrash(
                                editor = editor,
                                canvasHeightPx = canvasHeightPx,
                                thresholdFraction = TrashCatchFraction,
                            )
                            if (overTrashNow != isOverTrash) {
                                isOverTrash = overTrashNow
                                if (overTrashNow) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }

                            // On capture only: a tick in both directions turns
                            // a careful nudge into a buzzing mess.
                            if (snappedNow && !wasSnapped) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            wasSnapped = snappedNow
                        },
                        onHandleDrag = { handle, from, to ->
                            when (handle) {
                                is SelectionHandle.Corner -> editor.scaleAbout(
                                    factor = cornerScale(handle.anchor, from, to),
                                    anchor = CanvasPoint(handle.anchor.x, handle.anchor.y),
                                    canvasWidthPx = canvasWidthPx,
                                    canvasHeightPx = canvasHeightPx,
                                )

                                is SelectionHandle.Rotate -> editor.rotateSelection(
                                    degrees = rotationBetween(handle.pivot, from, to),
                                    mode = RotationMode.Group,
                                    canvasWidthPx = canvasWidthPx,
                                    canvasHeightPx = canvasHeightPx,
                                    recordUndo = false,
                                )
                            }
                        },
                        onTransform = { zoom, rotation, _ ->
                            transformSelection(editor, zoom, rotation)
                        },
                        onGestureEnd = {
                            guides = emptyList()
                            wasSnapped = false
                            dragSession = null
                            isManipulating = false
                            if (isOverTrash) {
                                editor.deleteSelection()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isOverTrash = false
                                scope.launch { panel.hide() }
                            }
                        },
                        onUndo = { editor.undo() },
                        onRedo = { editor.redo() },
                    ),
                canvasColor = Color(editor.canvas.color),
            )

            SelectionOverlay(
                measurements = if (showMeasurements) {
                    measurementsFor(editor, canvasWidthPx, canvasHeightPx, screenMetrics)
                } else {
                    emptyList()
                },
                showCanvasEdge = isScaled,
                showCenterLines = dragSession != null,
                eyes = editor.eyes,
                selection = editor.selection,
                guides = guides,
                accent = AppTheme.colors.accentPrimary.color,
                revision = editor.transformRevision,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Under the chrome, over the canvas: the backlight handles the top of
        // the range and this the bottom, letting the scene go below the OS floor.
        val overlayAlpha = dimLevelsFor(editor.canvas.brightness).overlayAlpha
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha)),
            )
        }

        // Chrome dissolves; it does not slide, and the canvas does not move.
        AnimatedVisibility(
            visible = !display.isActive,
            enter = fadeIn(Motion.Chrome.restore()),
            exit = fadeOut(Motion.Chrome.dissolve()),
        ) {
            EditorChrome(
                editor = editor,
                readout = readoutText(
                    editor = editor,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    screenMetrics = screenMetrics,
                ),
                // Which edge the panel is eating depends on its shape, and the
                // chrome has to dodge the same one. A tablet's rail sits
                // exactly where the floating toolbar would otherwise be.
                bottomClearance = with(density) { (if (isRail) 0f else occupied).toDp() },
                endClearance = with(density) { (if (isRail) occupied else 0f).toDp() },
                isPanelOpen = panel.isExpanded,
                isRecessed = isManipulating,
                sceneName = openScene?.name?.takeIf { it.isNotBlank() } ?: fallbackSceneName,
                onRenameScene = { isRenaming = true },
                onOpenScenes = { drawerOpen = true },
                onTogglePanel = { scope.launch { panel.toggle() } },
                isMeasuring = showMeasurements,
                onToggleMeasuring = { showMeasurements = !showMeasurements },
                onAddEye = {
                    editor.addEye()
                    scope.launch { panel.show() }
                },
                onDuplicate = { editor.duplicateSelection() },
                onEnterDisplay = {
                    editor.clearSelection()
                    display.enter()
                },
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            )
        }

        TrashTarget(
            visible = isManipulating && editor.selection.isNotEmpty(),
            isArmed = isOverTrash,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = Dimension.D1000)
                .padding(bottom = with(density) { (if (isRail) 0f else panel.occupiedPx).toDp() }),
        )

        // Faded, not slid away. The canvas scales itself against the panel's
        // occupied height, so actually moving the panel mid-drag would rescale
        // the canvas and slide the eye out from under the finger — the one
        // thing a placement gesture must never do. Fading gives the screen back
        // to the composition without moving a pixel of it.
        val panelAlpha = animateFloatAsState(
            targetValue = if (isManipulating) RecessedAlpha else 1f,
            animationSpec = Motion.Chrome.dissolve(),
            label = "panelAlpha",
        )

        AnimatedVisibility(
            visible = !display.isActive,
            modifier = Modifier.graphicsLayer { alpha = panelAlpha.value },
            enter = fadeIn(Motion.Chrome.restore()),
            exit = fadeOut(Motion.Chrome.dissolve()),
        ) {
        // SegmentedControl's label is a plain function, not composable.
        val tabLabels = PanelTab.entries.associateWith { stringResource(it.label) }
        AdaptivePanel(
            state = panel,
            header = {
                SegmentedControl(
                    options = PanelTab.entries,
                    selected = tab,
                    onSelect = {
                        tab = it
                        // Picking a tab is asking to see it. Without this the
                        // only way in is a bare handle, and "how do I change
                        // the colour" has no visible answer.
                        scope.launch { panel.expand() }
                    },
                    label = { tabLabels.getValue(it) },
                    modifier = Modifier.padding(
                        start = Dimension.D700,
                        end = Dimension.D700,
                        bottom = Dimension.D500,
                    ),
                )
            },
        ) {
            val panelScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(start = Dimension.D700, end = Dimension.D700)
                    .fadingEdge(panelScroll)
                    .verticalScroll(panelScroll),
                verticalArrangement = Arrangement.spacedBy(Dimension.D600),
            ) {
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
                        onLockedStyleTapped = { style -> lockedItem = LockedItem.Style(style) },
                        shortEdgePx = shortEdgePx,
                    )

                    PanelTab.Motion -> MotionPanel(
                        editor = editor,
                        isUnlocked = isUnlocked,
                        onMoodPicked = { applyMood(it) },
                        onReactivityChange = { enabled ->
                            if (isUnlocked) enableReactivity(enabled)
                        },
                        onLocked = { trigger -> router.navigate(PaywallRoute(trigger)) },
                    )

                    PanelTab.Scene -> ScenePanel(editor = editor)
                }

                // The sheet is capped at half the screen, so without this the
                // last control sits flush against the bottom edge and there is
                // nothing left to drag against to reach it.
                Spacer(modifier = Modifier.height(PanelBottomReach))
            }
        }
        }

        if (display.isActive) {
            DisplayOverlay(state = display, isListening = reactivityWanted)
        }

        // Last, so it covers the overlay too.
        SleepFade(display.sleepFade)

        lockedItem?.let { item ->
            LockedPreviewSheet(
                title = stringResource(item.label),
                onUnlock = {
                    lockedItem = null
                    router.navigate(PaywallRoute(item.trigger))
                },
                onDismiss = { lockedItem = null },
                preview = { item.Preview() },
            )
        }

        if (isRenaming) {
            RenameSceneDialog(
                currentName = openScene?.name.orEmpty(),
                placeholderName = fallbackSceneName,
                onRename = { name ->
                    isRenaming = false
                    openScene?.let { viewModel.takeAction(EditorAction.Rename(it, name)) }
                },
                onDismiss = { isRenaming = false },
            )
        }

        if (showMicExplanation) {
            MicrophoneExplanationDialog(
                onAllow = {
                    showMicExplanation = false
                    // Straight to the OS prompt, so the explanation the user
                    // just read is what the system dialog is answering.
                    viewModel.takeAction(EditorAction.RequestMicrophone)
                },
                onDismiss = { showMicExplanation = false },
            )
        }

        if (showMicDenied) {
            MicrophoneDeniedDialog(
                onOpenSettings = {
                    showMicDenied = false
                    viewModel.takeAction(EditorAction.OpenAppSettings)
                },
                onDismiss = { showMicDenied = false },
            )
        }

        pendingStrobingMood?.let { mood ->
            FlashingWarningDialog(
                mood = mood,
                onContinue = {
                    viewModel.takeAction(EditorAction.FlashingWarningSeen(mood))
                    pendingStrobingMood = null
                    applyMood(mood, warned = true)
                },
                onReduceFlashing = {
                    viewModel.takeAction(EditorAction.ReduceFlashing)
                    pendingStrobingMood = null
                    applyMood(mood, warned = true)
                },
                onDismiss = { pendingStrobingMood = null },
            )
        }

        if (drawerOpen) {
            ScenesDrawer(
                saved = state.savedScenes,
                onOpenScene = {
                    viewModel.takeAction(EditorAction.Open(it))
                    drawerOpen = false
                },
                onOpenPreset = { preset, name ->
                    // A blank id forks a scene of the user's own rather than
                    // handing back the preset every time they reopen it.
                    viewModel.takeAction(
                        EditorAction.Open(preset.toScene(id = "", name = name)),
                    )
                    drawerOpen = false
                },
                onDeleteScene = { viewModel.takeAction(EditorAction.Delete(it)) },
                onNewBlank = {
                    viewModel.takeAction(EditorAction.Open(blankScene(fallbackSceneName)))
                    drawerOpen = false
                },
                onOpenSettings = {
                    drawerOpen = false
                    router.navigate(SettingsRoute())
                },
                onDismiss = { drawerOpen = false },
                isUnlocked = isUnlocked,
            )
        }
    }
}

/**
 * The controls that aren't the canvas: a menu in the corner and a rail down the
 * side, both of which get out of the way the moment you touch an eye.
 *
 * **They fade during manipulation.** You are aligning something with a hole cut
 * in cardboard, judging it by eye, and a floating slab of buttons sitting over
 * the thing you're judging is the one thing that makes that harder. Chrome that
 * recedes while you work and returns when you stop is the standard direct-
 * manipulation bargain — Procreate, Photos, Figma all take it — and it costs
 * nothing because during a drag you are not reaching for a button anyway.
 *
 * **The menu is not in the rail.** The rail is what you do to *this scene*; the
 * menu leaves it. Top-left is where that has lived since the hamburger was
 * invented, and separating them means a thumb resting on the rail can't open
 * the drawer by accident.
 */
@Composable
private fun EditorChrome(
    editor: EditorState,
    readout: String?,
    bottomClearance: Dp,
    endClearance: Dp,
    isPanelOpen: Boolean,
    isRecessed: Boolean,
    isMeasuring: Boolean,
    sceneName: String,
    onOpenScenes: () -> Unit,
    onRenameScene: () -> Unit,
    onTogglePanel: () -> Unit,
    onToggleMeasuring: () -> Unit,
    onAddEye: () -> Unit,
    onDuplicate: () -> Unit,
    onEnterDisplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kept as State and read in the draw phase. Unwrapped with `by` it would
    // recompose the whole rail — five icon buttons and their labels — on every
    // frame of the fade, which is exactly the wrong thing to be doing while the
    // user is dragging an eye.
    val chromeAlpha = animateFloatAsState(
        targetValue = if (isRecessed) RecessedAlpha else 1f,
        animationSpec = Motion.Chrome.dissolve(),
        label = "chromeAlpha",
    )

    Box(modifier = modifier.graphicsLayer { alpha = chromeAlpha.value }) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Dimension.D700)
                .clip(RoundedCornerShape(RailCornerRadius))
                .background(AppTheme.colors.surfacePrimary.color.copy(alpha = 0.92f))
                .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(RailCornerRadius))
                .padding(RailPadding),
        ) {
            RailButton(
                icon = Icons.Menu,
                contentDescription = stringResource(Res.string.scenes_open),
                onClick = onOpenScenes,
            )
        }

        // Beside the menu rather than centred: the rail owns the other corner,
        // and a name floating in the middle of a black canvas reads as part of
        // the scene rather than as chrome.
        Text(
            text = sceneName,
            typography = AppTheme.typography.Label.L500,
            color = AppTheme.colors.textSecondary,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = SceneNameIndent, top = Dimension.D900)
                .widthIn(max = SceneNameMaxWidth)
                .clip(RoundedCornerShape(RailCornerRadius))
                .clickable(onClick = onRenameScene)
                .padding(horizontal = Dimension.D400, vertical = Dimension.D300),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = endClearance)
                .padding(Dimension.D700)
                .clip(RoundedCornerShape(RailCornerRadius))
                .background(AppTheme.colors.surfacePrimary.color.copy(alpha = 0.92f))
                .border(1.dp, AppTheme.colors.border.color, RoundedCornerShape(RailCornerRadius))
                // Scrolls only when it has to. Seven buttons and a divider come
                // to more than a phone has height for in landscape, and a
                // toolbar that runs off the bottom of the screen takes Play
                // with it.
                .verticalScroll(rememberScrollState())
                .padding(RailPadding),
            verticalArrangement = Arrangement.spacedBy(RailPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RailButton(
                icon = Icons.Add,
                contentDescription = stringResource(Res.string.place_add_eye),
                onClick = onAddEye,
            )
            RailButton(
                icon = Icons.Copy,
                contentDescription = stringResource(Res.string.place_duplicate),
                enabled = editor.selection.isNotEmpty(),
                onClick = onDuplicate,
            )
            RailButton(
                icon = Icons.Ruler,
                contentDescription = stringResource(Res.string.editor_measure),
                isActive = isMeasuring,
                onClick = onToggleMeasuring,
            )
            RailButton(
                icon = Icons.Pencil,
                contentDescription = stringResource(Res.string.editor_controls),
                isActive = isPanelOpen,
                onClick = onTogglePanel,
            )

            // Never inside a collapsible panel: the way back from a fat-finger
            // must be visible at the moment it happens.
            RailButton(
                icon = Icons.Undo,
                contentDescription = stringResource(Res.string.editor_undo),
                enabled = editor.canUndo,
                onClick = { editor.undo() },
            )
            RailButton(
                icon = Icons.Redo,
                contentDescription = stringResource(Res.string.editor_redo),
                enabled = editor.canRedo,
                onClick = { editor.redo() },
            )

            // Explicitly narrow: a divider left to fill would drag the whole
            // rail out to the screen's width.
            HorizontalDivider(
                modifier = Modifier
                    .width(RailDividerWidth)
                    .padding(vertical = Dimension.D100),
            )

            RailButton(
                icon = Icons.Play,
                contentDescription = stringResource(Res.string.display_enter),
                isPrimary = true,
                onClick = onEnterDisplay,
            )
        }

        // Only when it has something to say. A permanent chip reporting the
        // screen's own dimensions is furniture: it never changes, so it stops
        // being read, and it sits on the canvas the whole time anyway.
        AnimatedVisibility(
            visible = readout != null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Dimension.D700, end = Dimension.D700, bottom = Dimension.D700)
                .padding(bottom = bottomClearance),
            enter = fadeIn(Motion.Chrome.restore()),
            exit = fadeOut(Motion.Chrome.dissolve()),
        ) {
            ReadoutPill(text = readout.orEmpty(), emphasized = editor.selection.isNotEmpty())
        }
    }
}

/**
 * Where a dragged eye goes to die.
 *
 * **Only while dragging.** A delete button sitting on screen permanently is a
 * button someone eventually hits by accident, and an eye deleted by accident is
 * an alignment destroyed.
 *
 * **Deliberately hard to reach.** It arms only in the bottom [TrashCatchFraction]
 * of the canvas, which is further than any normal placement drag travels, and
 * it takes a release to commit — sliding back out disarms it. Undo still
 * catches the rest.
 *
 * Haptics on arming and again on delete, because by then the eye is under a
 * finger and cannot be seen.
 */
@Composable
private fun TrashTarget(visible: Boolean, isArmed: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(Motion.Chrome.restore()),
        exit = fadeOut(Motion.Chrome.dissolve()),
    ) {
        Box(
            modifier = Modifier
                .size(TrashSize)
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    if (isArmed) {
                        AppTheme.colors.danger.color
                    } else {
                        AppTheme.colors.surfacePrimary.color.copy(alpha = 0.92f)
                    },
                )
                .border(
                    width = if (isArmed) 2.dp else 1.dp,
                    color = if (isArmed) {
                        AppTheme.colors.danger.color
                    } else {
                        AppTheme.colors.border.color
                    },
                    shape = RoundedCornerShape(percent = 50),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon = Icons.Delete(stringResource(Res.string.place_delete)),
                size = IconSize.Medium,
                color = if (isArmed) AppTheme.colors.onAccentPrimary else AppTheme.colors.text,
            )
        }
    }
}

/**
 * True when every selected eye has been dragged into the bottom band.
 *
 * Every, not any: dragging a pair should not delete it because one eye's edge
 * strayed low.
 */
private fun isOverTrash(
    editor: EditorState,
    canvasHeightPx: Float,
    thresholdFraction: Float,
): Boolean {
    val active = editor.activeIndices()
    if (active.isEmpty() || canvasHeightPx <= 0f) return false
    return active.all { editor.eyes[it].centerY >= thresholdFraction }
}

/** How far down the canvas an eye must be dragged to arm the trash. */
private const val TrashCatchFraction = 0.88f

private val TrashSize = 64.dp

/**
 * Icon-only, so the rail stays one thumb wide, which is the whole reason it can
 * float over the canvas instead of eating a strip of it. The label survives as
 * the content description rather than being dropped.
 */
@Composable
private fun RailButton(
    icon: Icons,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isActive: Boolean = false,
    isPrimary: Boolean = false,
) {
    // An active toggle goes amber, not one shade of grey darker. The accent
    // means "live" everywhere else in this app — the snap guides, the readout,
    // Play — and a raised surface at 12% contrast on a dark rail is a state you
    // have to go looking for.
    val background = when {
        isPrimary -> AppTheme.colors.accentPrimary
        isActive -> AppTheme.colors.accentPrimary
        else -> null
    }
    IconButton(
        icon = icon(contentDescription),
        onClick = onClick,
        enabled = enabled,
        backgroundColor = background,
        iconColor = when {
            isPrimary || isActive -> AppTheme.colors.onAccentPrimary
            !enabled -> AppTheme.colors.textDisabled
            else -> AppTheme.colors.text
        },
    )
}

/** Room past the last control, so the bottom of a long tab is reachable
 *  rather than pinned under the screen edge. */
private val PanelBottomReach = 72.dp

/** Clear of the menu button that sits in the same corner. */
private val SceneNameIndent = 84.dp
private val SceneNameMaxWidth = 180.dp

private val RailCornerRadius = 18.dp
private val RailPadding = 6.dp
private val RailDividerWidth = 28.dp

/** Faded, not gone: you should still be able to find undo without lifting your
 *  finger to make the buttons come back. */
private const val RecessedAlpha = 0.15f

/**
 * The spans worth drawing while measuring: the gap between neighbouring eyes,
 * and how wide each eye is.
 *
 * Edge to edge, not centre to centre. Centres are what the app snaps by, but
 * they are not what anyone holds a ruler against — someone marking cardboard
 * measures the *gap* between two holes and the width of each, because those are
 * the cuts. A centre-to-centre figure has to be converted before it is usable,
 * and converting it requires knowing the two radii, which is the thing the
 * readout wasn't telling you either.
 *
 * Chained left to right rather than every pair to every other, because
 * n-squared lines answer "how far apart are these two" by burying it. Follows
 * the selection when there is one, so a crowded scene narrows to the pair being
 * worked on.
 */
@Composable
private fun measurementsFor(
    editor: EditorState,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    screenMetrics: ScreenMetrics,
): List<Measurement> {
    @Suppress("UNUSED_EXPRESSION")
    editor.transformRevision

    val indices = editor.selection.takeIf { it.size >= 2 }
        ?: editor.eyes.indices.toList().takeIf { it.size in 1..MaxUnselectedMeasurements }
        ?: return emptyList()

    val placed = indices
        .map { editor.eyes[it] }
        .map { eye -> eye to Offset(eye.centerX * canvasWidthPx, eye.centerY * canvasHeightPx) }
        .sortedBy { it.second.x }

    // zipWithNext and map are inline, so the composable string lookups inside
    // them are legal — the same reason the readout can format inside its loop.
    val gaps = placed.zipWithNext { (fromEye, fromAt), (toEye, toAt) ->
        val along = toAt - fromAt
        val length = along.getDistance()
        if (length < 1f) return@zipWithNext null
        val unit = along / length

        // From the near edge of each, not from the centres: the gap is what
        // gets cut away, and the radius along this exact bearing is what the
        // eye's own ellipse gives at that angle.
        val start = fromAt + unit * fromEye.radiusAlong(unit)
        val end = toAt - unit * toEye.radiusAlong(unit)
        if ((end - start).getDistance() < 1f) return@zipWithNext null

        Measurement(start, end, spanLabel((end - start).getDistance(), screenMetrics))
    }.filterNotNull()

    // Widths hang below their eye rather than crossing it. Drawn on the centre
    // line they merged with the gap spans either side into one long rule, and a
    // number sitting on a continuous line does not say which part of it it is
    // talking about.
    val drop = with(LocalDensity.current) { WidthMeasurementDrop.toPx() }
    val widths = placed.map { (eye, at) ->
        val half = eye.sizePx / 2f
        Measurement(
            from = Offset(at.x - half, at.y),
            to = Offset(at.x + half, at.y),
            label = spanLabel(eye.sizePx, screenMetrics),
            leader = eye.sizePx * eye.style.aspectRatio / 2f + drop,
        )
    }

    return gaps + widths
}

/** The eye's radius on a given bearing. An ellipse is not a circle, and a pair
 *  measured on the horizontal is nowhere near one measured on the diagonal. */
private fun RenderedEye.radiusAlong(unit: Offset): Float {
    val semiWidth = sizePx / 2f
    val semiHeight = sizePx * style.aspectRatio / 2f
    val denominator = sqrt(
        (semiHeight * unit.x) * (semiHeight * unit.x) + (semiWidth * unit.y) * (semiWidth * unit.y),
    )
    return if (denominator <= 0f) semiWidth else semiWidth * semiHeight / denominator
}

@Composable
private fun spanLabel(distancePx: Float, screenMetrics: ScreenMetrics): String {
    val millimetres = screenMetrics.millimeters(distancePx)
    return if (millimetres != null) {
        stringResource(
            Res.string.editor_readout_millimeters,
            ((millimetres * 10).roundToInt() / 10f).toString(),
        )
    } else {
        stringResource(Res.string.editor_readout_size, distancePx.roundToInt())
    }
}

/** How far below an eye its own width is measured, clear of the gap spans that
 *  run through its centre. */
private val WidthMeasurementDrop = 18.dp

/** Past this many, chained dimension lines are a thicket rather than a
 *  measurement. Select the ones you care about instead. */
private const val MaxUnselectedMeasurements = 8

private val PanelTab.label
    get() = when (this) {
        PanelTab.Place -> Res.string.panel_place
        PanelTab.Look -> Res.string.panel_look
        PanelTab.Motion -> Res.string.panel_motion
        PanelTab.Scene -> Res.string.panel_scene
    }

/** What the app opens on before anything is saved. */
private fun blankScene(name: String) = Scene(
    id = "",
    name = name,
    eyes = ScenePresets.blank(),
)

/**
 * Snaps the selection's centre and carries every eye rigidly with it.
 *
 * The anchor is the middle of the same box the overlay draws, so the guide
 * lights up when the thing you can see is centred. Snapping each eye
 * independently would drift a pair apart mid-drag; snapping the *first* eye,
 * which is what this used to do, quietly centred the left eye and left the
 * pair sitting off to the right of the line claiming it was aligned.
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

    // From each eye's own start plus raw finger travel, so last frame's snap
    // can't feed back into this one.
    active.forEach { index ->
        val raw = session.rawPosition(index) ?: return@forEach
        editor.eyes[index].apply {
            centerX = raw.first.coerceIn(0f, 1f)
            centerY = raw.second.coerceIn(0f, 1f)
        }
    }

    val anchor = selectionBounds(
        eyes = editor.eyes,
        selection = active,
        canvasWidth = canvasWidthPx,
        canvasHeight = canvasHeightPx,
        paddingPx = 0f,
    )?.center ?: return false

    val result = resolveSnap(
        dragged = SnapCandidate(
            id = active.first(),
            center = CanvasPoint(anchor.x, anchor.y),
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
        val correctionX = (result.position.x - anchor.x) / canvasWidthPx
        val correctionY = (result.position.y - anchor.y) / canvasHeightPx
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

/** Rotation detents to 15° but holds any angle past one: a picture rail is
 *  sometimes at 7°. */
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
 * The readout reports, it never instructs, and it says nothing when it has
 * nothing to report.
 *
 * Null with no selection: the canvas's own size never changes, so a chip
 * repeating it forever is furniture that stops being read while still covering
 * part of the scene. The measurement toggle is where "how big is this" lives
 * now, and it answers with lines between the actual points rather than one
 * figure in a corner.
 *
 * Millimetres appear only when the platform actually knows the screen's
 * physical size: a figure the app can't stand behind is worse than none when
 * someone is about to cut a hole from it.
 */
@Composable
private fun readoutText(
    editor: EditorState,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    screenMetrics: ScreenMetrics,
): String? {
    // Read so the readout recomposes as a drag moves non-Compose state.
    @Suppress("UNUSED_EXPRESSION")
    editor.transformRevision

    val selection = editor.selection
    if (selection.isEmpty()) return null

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

private val ToolbarClearance = 88.dp

/** Breathing room around the scaled canvas, so its edge reads as an edge. */
private val CanvasInset = 12.dp

/** Matches AdaptivePanel's own breakpoint; the editor needs to know which
 *  dimension the panel is eating. */
private val RailBreakpointDp = 720.dp

/** Below this the scene is too small to work with; better to let the panel
 *  cover a little than to shrink to a postage stamp. */
private const val MinCanvasScale = 0.35f

/** Long enough that a slider drag writes once, short enough that a kill a
 *  second later still keeps the edit. */
private val AutosaveDebounce = 1.seconds

private const val MinEyeSizePx = 24f
private const val MaxEyeSizePx = 2000f
