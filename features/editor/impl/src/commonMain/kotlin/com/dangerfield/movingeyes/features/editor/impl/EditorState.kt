@file:Suppress("MagicNumber", "TooManyFunctions")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.SceneDirector
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.eyes.reducedFlashing
import com.dangerfield.movingeyes.libraries.render.RenderedEye
import com.dangerfield.movingeyes.libraries.render.toSceneEyes
import com.dangerfield.movingeyes.libraries.scene.CanvasRotation
import com.dangerfield.movingeyes.libraries.scene.Scene
import kotlin.math.hypot
import kotlin.time.Duration
import kotlin.random.Random

/** Everything about the canvas that isn't an eye. */
data class CanvasSettings(
    val rotation: CanvasRotation = CanvasRotation.None,
    val color: Long = Scene.OpaqueBlack,
    val brightness: Float = 1f,

    /** Null runs all night. */
    val sleepTimer: Duration? = null,

    val reactivityEnabled: Boolean = false,
    val blinkTogether: Boolean = true,
)

/** One eye's full mutable state, as much of it as undo needs to restore. */
data class EyeSnapshot(
    val style: EyeStyle,
    val centerX: Float,
    val centerY: Float,
    val sizePx: Float,
    val rotationDegrees: Float,
    val scleraColor: Color,
    val irisColor: Color,
    val pupilColor: Color,
    val glowFraction: Float,
    val veinIntensity: Float,
    val mood: Mood,
    val behavior: BehaviorConfig,
)

/** The whole canvas at one moment. What an undo step actually is. */
data class SceneSnapshot(
    val eyes: List<EyeSnapshot>,
    val canvas: CanvasSettings,
)

/**
 * Selection, transforms, styling and undo.
 *
 * Two kinds of state, kept apart on purpose. Per-eye values live on
 * [RenderedEye] as plain fields, so a drag doesn't invalidate composition
 * sixty times a second. Selection, the eye *list* and canvas settings are
 * Compose state, because those change on a tap and the overlay and panels do
 * need to recompose. Adding an eye is Compose state; moving one is not.
 *
 * Every mutator works through [activeIndices] — the selection, or everything
 * when there isn't one. That rule holds across all four panels.
 */
class EditorState(
    eyes: List<RenderedEye>,
    moods: List<Mood>,
    canvas: CanvasSettings = CanvasSettings(),
    private val gaze: SceneDirector? = null,
) {

    private val _eyes = mutableStateListOf<RenderedEye>().apply { addAll(eyes) }
    val eyes: List<RenderedEye> get() = _eyes

    /**
     * Parallel to [eyes]. Held here rather than on `RenderedEye`, which only
     * needs the resolved config: keeping the *name* lets a saved scene say
     * "Frantic" rather than freezing today's numbers for it.
     */
    private val _moods = mutableStateListOf<Mood>().apply { addAll(moods) }
    val moods: List<Mood> get() = _moods

    var canvas by mutableStateOf(canvas)
        private set

    /**
     * Applied on the way to the runtime rather than baked into the scene, so
     * turning the setting off restores the mood the user actually chose.
     */
    var reduceFlashing: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            _eyes.forEachIndexed { index, eye ->
                eye.runtime.behavior = behaviorFor(_moods.getOrElse(index) { Mood.IdleScan })
            }
            syncGaze()
            transformChanged()
        }

    private fun behaviorFor(mood: Mood): BehaviorConfig =
        Moods.forMood(mood).let { if (reduceFlashing) it.reducedFlashing() else it }

    /** The scene looks with whatever the first eye is doing; a mood applied to
     *  a subset shouldn't leave the gaze on the old one. */
    private fun syncGaze() {
        gaze?.behavior = _eyes.firstOrNull()?.runtime?.behavior ?: return
    }

    private val _selection = mutableStateListOf<Int>()

    /** Indices into [eyes]. Empty means nothing selected. */
    val selection: List<Int> get() = _selection

    /** Bumped whenever a per-eye value changes, so the readout and panels
     *  recompose. The values themselves stay off Compose state — see the
     *  class doc. */
    var transformRevision by mutableStateOf(0)
        private set

    private val undoStack = ArrayDeque<SceneSnapshot>()
    private val redoStack = ArrayDeque<SceneSnapshot>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    // ---- Selection ------------------------------------------------------

    fun select(index: Int, additive: Boolean = false) {
        if (additive) {
            if (!_selection.remove(index)) _selection.add(index)
        } else {
            _selection.clear()
            _selection.add(index)
        }
    }

    fun selectAll() {
        _selection.clear()
        _selection.addAll(_eyes.indices)
    }

    fun clearSelection() = _selection.clear()

    fun setSelection(indices: Collection<Int>) {
        _selection.clear()
        _selection.addAll(indices.filter { it in _eyes.indices })
    }

    fun isSelected(index: Int): Boolean = index in _selection

    /** Indices the current operation applies to: the selection, or everything
     *  when nothing is selected. */
    fun activeIndices(): List<Int> =
        if (_selection.isEmpty()) _eyes.indices.toList() else _selection.toList()

    // ---- Undo -----------------------------------------------------------

    /** One undo step per gesture, not per frame: a drag recording every touch
     *  move would need two hundred undos to get back. */
    fun beginGesture(): DragSession = DragSession(pushUndo())

    /** For a discrete change — a colour, a style, a mood. */
    fun beginEdit() = pushUndo()

    private fun pushUndo(): SceneSnapshot {
        val start = snapshot()
        undoStack.addLast(start)
        if (undoStack.size > MaxUndoSteps) undoStack.removeFirst()
        redoStack.clear()
        refreshUndoState()
        return start
    }

    /** Call after mutating eyes so the readout and panels pick it up. */
    fun transformChanged() {
        transformRevision += 1
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot())
        restore(previous)
        refreshUndoState()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot())
        restore(next)
        refreshUndoState()
    }

    fun snapshot(): SceneSnapshot = SceneSnapshot(
        eyes = _eyes.mapIndexed { index, eye ->
            EyeSnapshot(
                style = eye.style,
                centerX = eye.centerX,
                centerY = eye.centerY,
                sizePx = eye.sizePx,
                rotationDegrees = eye.rotationDegrees,
                scleraColor = eye.scleraColor,
                irisColor = eye.irisColor,
                pupilColor = eye.pupilColor,
                glowFraction = eye.glowFraction,
                veinIntensity = eye.veinIntensity,
                mood = _moods.getOrElse(index) { Mood.IdleScan },
                behavior = eye.runtime.behavior,
            )
        },
        canvas = canvas,
    )

    /**
     * Restores in place: rebuilding a `RenderedEye` would reset its blink and
     * saccade timers, so undoing a colour change would make every eye blink in
     * unison. Only a differing eye *count* forces a rebuild.
     */
    fun restore(snapshot: SceneSnapshot) {
        if (snapshot.eyes.size != _eyes.size) {
            val rebuilt = snapshot.eyes.mapIndexed { index, saved ->
                RenderedEye(
                    style = saved.style,
                    centerX = saved.centerX,
                    centerY = saved.centerY,
                    sizePx = saved.sizePx,
                    rotationDegrees = saved.rotationDegrees,
                    scleraColor = saved.scleraColor,
                    irisColor = saved.irisColor,
                    pupilColor = saved.pupilColor,
                    veinIntensity = saved.veinIntensity,
                    glowFraction = saved.glowFraction,
                    behavior = saved.behavior,
                    random = Random(index),
                )
            }
            _eyes.clear()
            _eyes.addAll(rebuilt)
            _selection.removeAll { it !in _eyes.indices }
        } else {
            snapshot.eyes.forEachIndexed { index, saved ->
                _eyes[index].apply {
                    style = saved.style
                    centerX = saved.centerX
                    centerY = saved.centerY
                    sizePx = saved.sizePx
                    rotationDegrees = saved.rotationDegrees
                    scleraColor = saved.scleraColor
                    irisColor = saved.irisColor
                    pupilColor = saved.pupilColor
                    glowFraction = saved.glowFraction
                    veinIntensity = saved.veinIntensity
                    runtime.behavior = saved.behavior
                }
            }
        }

        _moods.clear()
        _moods.addAll(snapshot.eyes.map { it.mood })
        canvas = snapshot.canvas
        transformChanged()
    }

    private fun refreshUndoState() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    // ---- Look -----------------------------------------------------------

    fun setStyle(style: EyeStyle) = editActive { eye ->
        eye.style = style
    }

    fun setScleraColor(color: Color) = editActive { it.scleraColor = color }

    fun setIrisColor(color: Color) = editActive { it.irisColor = color }

    fun setPupilColor(color: Color) = editActive { it.pupilColor = color }

    fun setGlowFraction(fraction: Float) = editActive {
        it.glowFraction = fraction.coerceIn(0f, MaxGlowFraction)
    }

    fun setVeinIntensity(intensity: Float) = editActive {
        it.veinIntensity = intensity.coerceIn(0f, 1f)
    }

    fun setSizePx(sizePx: Float) = editActive {
        it.sizePx = sizePx.coerceIn(MinEyeSizePx, MaxEyeSizePx)
    }

    // ---- Motion ---------------------------------------------------------

    fun setMood(mood: Mood) {
        beginEdit()
        activeIndices().forEach { index ->
            _moods[index] = mood
            _eyes[index].runtime.behavior = behaviorFor(mood)
        }
        syncGaze()
        transformChanged()
    }

    /** Moves the eye to [Mood.Custom]: a scene claiming Frantic while running
     *  something else would come back wrong. */
    fun setBehavior(behavior: BehaviorConfig) {
        beginEdit()
        activeIndices().forEach { index ->
            _moods[index] = Mood.Custom
            _eyes[index].runtime.behavior =
                if (reduceFlashing) behavior.reducedFlashing() else behavior
        }
        syncGaze()
        transformChanged()
    }

    /**
     * The first active eye, for panels that display its values.
     *
     * Reads [transformRevision] so a caller composing against it re-runs when
     * those values change. Without that, a panel showing a colour or a blink
     * rate would go stale after an undo, a demo revert, or a settings change,
     * because none of it is Compose state.
     */
    fun activeEye(): RenderedEye? {
        @Suppress("UNUSED_EXPRESSION")
        transformRevision
        return activeIndices().firstOrNull()?.let { _eyes[it] }
    }

    fun activeBehavior(): BehaviorConfig = activeEye()?.runtime?.behavior ?: Moods.FreeDefault

    /** Null when the selection disagrees. */
    fun activeMood(): Mood? = activeIndices()
        .map { _moods.getOrElse(it) { Mood.IdleScan } }
        .distinct()
        .singleOrNull()

    /** Null when the selection disagrees. */
    fun activeStyle(): EyeStyle? {
        @Suppress("UNUSED_EXPRESSION")
        transformRevision
        return activeIndices().map { _eyes[it].style }.distinct().singleOrNull()
    }

    // ---- Place ----------------------------------------------------------

    /** Move by whole canvas pixels. The stepper's 1px and hold-10. */
    fun nudge(dxPx: Float, dyPx: Float, canvasWidthPx: Float, canvasHeightPx: Float) = editActive {
        it.centerX = (it.centerX + dxPx / canvasWidthPx).coerceIn(0f, 1f)
        it.centerY = (it.centerY + dyPx / canvasHeightPx).coerceIn(0f, 1f)
    }

    /** [RotationMode.Group] orbits about the shared centre as well as turning
     *  each; [RotationMode.Each] only turns in place. */
    fun rotateSelection(
        degrees: Float,
        mode: RotationMode,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        /** False during a drag, which already opened one undo step of its own. */
        recordUndo: Boolean = true,
    ) {
        val active = activeIndices()
        if (active.isEmpty()) return
        if (recordUndo) beginEdit()

        if (mode == RotationMode.Group && active.size > 1) {
            val points = active.map { pixelPointOf(it, canvasWidthPx, canvasHeightPx) }
            val turned = rotateAbout(points, centroidOf(points), degrees)
            active.forEachIndexed { slot, index ->
                _eyes[index].centerX = (turned[slot].x / canvasWidthPx).coerceIn(0f, 1f)
                _eyes[index].centerY = (turned[slot].y / canvasHeightPx).coerceIn(0f, 1f)
            }
        }

        active.forEach { _eyes[it].rotationDegrees += degrees }
        transformChanged()
    }

    /** Scale the active eyes about [anchor], the corner opposite the one held. */
    fun scaleAbout(factor: Float, anchor: CanvasPoint, canvasWidthPx: Float, canvasHeightPx: Float) {
        val active = activeIndices()
        if (active.isEmpty()) return

        active.forEach { index ->
            _eyes[index].apply {
                sizePx = (sizePx * factor).coerceIn(MinEyeSizePx, MaxEyeSizePx)
                // Positions scale about the anchor too, so a group keeps its
                // spacing rather than every eye growing in place.
                val x = centerX * canvasWidthPx
                val y = centerY * canvasHeightPx
                centerX = ((anchor.x + (x - anchor.x) * factor) / canvasWidthPx).coerceIn(0f, 1f)
                centerY = ((anchor.y + (y - anchor.y) * factor) / canvasHeightPx).coerceIn(0f, 1f)
            }
        }
        transformChanged()
    }

    fun alignEvenly(canvasWidthPx: Float, canvasHeightPx: Float) =
        reposition(canvasWidthPx, canvasHeightPx, ::distributeEvenly)

    fun mirror(canvasWidthPx: Float, canvasHeightPx: Float) =
        reposition(canvasWidthPx, canvasHeightPx, ::mirrorHorizontally)

    private fun reposition(
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        transform: (List<CanvasPoint>) -> List<CanvasPoint>,
    ) {
        val active = activeIndices()
        if (active.size < 2) return
        beginEdit()

        val moved = transform(active.map { pixelPointOf(it, canvasWidthPx, canvasHeightPx) })
        active.forEachIndexed { slot, index ->
            _eyes[index].centerX = (moved[slot].x / canvasWidthPx).coerceIn(0f, 1f)
            _eyes[index].centerY = (moved[slot].y / canvasHeightPx).coerceIn(0f, 1f)
        }
        transformChanged()
    }

    private fun pixelPointOf(index: Int, canvasWidthPx: Float, canvasHeightPx: Float) =
        CanvasPoint(_eyes[index].centerX * canvasWidthPx, _eyes[index].centerY * canvasHeightPx)

    // ---- The eye list ---------------------------------------------------

    /** Copies the first active eye so the new one lands as a plausible partner
     *  rather than a default that needs re-styling. Falls all the way back to a
     *  plain human eye, because a scene you have emptied still has to be one
     *  you can add to. */
    fun addEye() {
        beginEdit()
        val template = activeIndices().firstOrNull()?.let { _eyes[it] }
        val added = RenderedEye(
            style = template?.style ?: _eyes.firstOrNull()?.style ?: EyeStyles.HumanBasic,
            centerX = ((template?.centerX ?: 0.5f) + NewEyeOffset).coerceIn(0f, 1f),
            centerY = template?.centerY ?: 0.5f,
            sizePx = template?.sizePx ?: MinEyeSizePx * 4f,
            rotationDegrees = template?.rotationDegrees ?: 0f,
            scleraColor = template?.scleraColor ?: Color.White,
            irisColor = template?.irisColor ?: Color.Black,
            pupilColor = template?.pupilColor ?: Color.Black,
            veinIntensity = template?.veinIntensity ?: 0.5f,
            glowFraction = template?.glowFraction ?: 0f,
            behavior = template?.runtime?.behavior ?: Moods.FreeDefault,
            random = Random(_eyes.size + EyeSeedOffset),
        )
        _eyes.add(added)
        _moods.add(activeMood() ?: Mood.IdleScan)
        _selection.clear()
        _selection.add(_eyes.lastIndex)
        transformChanged()
    }

    /**
     * Every selected eye again, nudged clear of its original and selected in
     * its place, so the copies are what the next drag moves.
     *
     * The offset matters: a duplicate landing exactly on top of its original is
     * indistinguishable from nothing having happened, and the user's next move
     * is to press the button again.
     */
    fun duplicateSelection() {
        val sources = activeIndices().map { _eyes[it] to _moods.getOrElse(it) { Mood.IdleScan } }
        if (sources.isEmpty()) return
        beginEdit()

        val copies = sources.map { (eye, mood) ->
            RenderedEye(
                style = eye.style,
                centerX = (eye.centerX + NewEyeOffset).coerceIn(0f, 1f),
                centerY = (eye.centerY + NewEyeOffset).coerceIn(0f, 1f),
                sizePx = eye.sizePx,
                rotationDegrees = eye.rotationDegrees,
                scleraColor = eye.scleraColor,
                irisColor = eye.irisColor,
                pupilColor = eye.pupilColor,
                veinIntensity = eye.veinIntensity,
                glowFraction = eye.glowFraction,
                behavior = eye.runtime.behavior,
                random = Random(_eyes.size + EyeSeedOffset),
            ) to mood
        }

        _selection.clear()
        copies.forEach { (eye, mood) ->
            _eyes.add(eye)
            _moods.add(mood)
            _selection.add(_eyes.lastIndex)
        }
        transformChanged()
    }

    /** The one mutator that does *not* fall through to "everything" when
     *  nothing is selected. */
    fun deleteSelection() {
        if (_selection.isEmpty() || _selection.size == _eyes.size) return
        beginEdit()

        _selection.sortedDescending().forEach { index ->
            _eyes.removeAt(index)
            _moods.removeAt(index)
        }
        _selection.clear()
        transformChanged()
    }

    // ---- Scene ----------------------------------------------------------

    fun setCanvasRotation(rotation: CanvasRotation) {
        beginEdit()
        canvas = canvas.copy(rotation = rotation)
    }

    fun setCanvasColor(color: Long) {
        beginEdit()
        canvas = canvas.copy(color = color)
    }

    fun setSleepTimer(timer: Duration?) {
        beginEdit()
        canvas = canvas.copy(sleepTimer = timer)
    }

    fun setReactivityEnabled(enabled: Boolean) {
        beginEdit()
        canvas = canvas.copy(reactivityEnabled = enabled)
    }

    fun setBlinkTogether(together: Boolean) {
        beginEdit()
        canvas = canvas.copy(blinkTogether = together)
    }

    /** Not undoable: brightness is a live comfort control, not an edit. */
    fun setBrightness(brightness: Float) {
        canvas = canvas.copy(brightness = brightness.coerceIn(MinBrightness, 1f))
    }

    fun toScene(id: String, name: String, canvasWidthPx: Float, canvasHeightPx: Float) = Scene(
        id = id,
        name = name,
        eyes = _eyes.toSceneEyes(canvasWidthPx, canvasHeightPx, _moods),
        canvasRotation = canvas.rotation,
        canvasColor = canvas.color,
        brightness = canvas.brightness,
        sleepTimerMinutes = canvas.sleepTimer?.inWholeMinutes?.toInt(),
        reactivityEnabled = canvas.reactivityEnabled,
        blinkTogether = canvas.blinkTogether,
    )

    /** Null unless exactly two eyes are selected. */
    fun selectedSpacingPx(canvasWidthPx: Float, canvasHeightPx: Float): Float? {
        if (_selection.size != 2) return null
        val a = _eyes[_selection[0]]
        val b = _eyes[_selection[1]]
        return hypot((b.centerX - a.centerX) * canvasWidthPx, (b.centerY - a.centerY) * canvasHeightPx)
    }

    private inline fun editActive(block: (RenderedEye) -> Unit) {
        val active = activeIndices()
        if (active.isEmpty()) return
        beginEdit()
        active.forEach { block(_eyes[it]) }
        transformChanged()
    }

    private companion object {
        const val MaxUndoSteps = 50

        const val NewEyeOffset = 0.12f

        /** Clear of the index seeds used when loading a scene, so a new eye
         *  can't share a phase with an existing one. */
        const val EyeSeedOffset = 1000

        /** Past this, glow reads as fog rather than bloom. */
        const val MaxGlowFraction = 0.35f

        /** Never black: a slider that reaches zero looks like a crash. */
        const val MinBrightness = 0.05f

        const val MinEyeSizePx = 24f
        const val MaxEyeSizePx = 2000f
    }
}

/**
 * One drag, tracking raw finger travel so a snap can't become sticky. The
 * drawn position is the snapped one, but the next frame is measured from where
 * the finger actually is — otherwise each frame re-snaps from the corrected
 * position and the eye welds itself to the first guide it touches.
 */
class DragSession(private val start: SceneSnapshot) {
    private var panX = 0f
    private var panY = 0f

    /** Accumulated finger travel, ignoring any snapping that happened. */
    fun accumulate(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    /** Where [index] would be with no snapping at all. */
    fun rawPosition(index: Int): Pair<Float, Float>? {
        val eye = start.eyes.getOrNull(index) ?: return null
        return (eye.centerX + panX) to (eye.centerY + panY)
    }
}

/** Which eye a touch landed on, or null for empty canvas. */
fun List<RenderedEye>.hitTest(
    x: Float,
    y: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    minimumTouchPx: Float,
): Int? = indices
    .map { index ->
        val eye = this[index]
        val cx = eye.centerX * canvasWidth
        val cy = eye.centerY * canvasHeight
        // A small eye is still a 56dp target; fingers don't shrink with it.
        val radius = maxOf(eye.sizePx / 2f, minimumTouchPx / 2f)
        index to hypot(x - cx, y - cy) / radius
    }
    .filter { it.second <= 1f }
    // Proportionally nearest, so a small eye on top of a big one is reachable.
    .minByOrNull { it.second }
    ?.first
