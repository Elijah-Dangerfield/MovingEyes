@file:Suppress("MagicNumber", "TooManyFunctions")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeStyle
import com.dangerfield.movingeyes.libraries.eyes.Mood
import com.dangerfield.movingeyes.libraries.eyes.Moods
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
 * Selection, transforms, styling and undo for the editor.
 *
 * ## Two kinds of state, deliberately kept apart
 *
 * **Per-eye values live on [RenderedEye]** and are plain mutable fields. The
 * renderer reads them inside its draw lambda every frame, so moving an eye
 * shows up on the next frame with no recomposition at all — a drag doesn't
 * invalidate composition sixty times a second.
 *
 * **Selection, the eye *list*, and canvas settings live in Compose state**,
 * because those change on a tap rather than on a frame, and the overlay,
 * readout and panels genuinely need to recompose when they do. Note the
 * distinction on the list: adding an eye is Compose state, moving one is not.
 *
 * Getting that split wrong is the difference between a drag that feels direct
 * and one that stutters, which on this app is the difference between an
 * alignment you trust and one you don't.
 *
 * ## Everything acts on the selection, or on everything
 *
 * Every mutator here works through [activeIndices], which is the selection when
 * there is one and the whole scene when there isn't. That one rule means a user
 * who hasn't learned to select yet can still recolour their whole scene, and it
 * holds identically across all four panels.
 */
class EditorState(
    eyes: List<RenderedEye>,
    moods: List<Mood>,
    canvas: CanvasSettings = CanvasSettings(),
) {

    private val _eyes = mutableStateListOf<RenderedEye>().apply { addAll(eyes) }
    val eyes: List<RenderedEye> get() = _eyes

    /**
     * Which named mood each eye is on, parallel to [eyes].
     *
     * Held here rather than on `RenderedEye` because the renderer has no use
     * for it — it only needs the resolved [BehaviorConfig]. Keeping the name is
     * what lets a saved scene say "Frantic" rather than freezing today's
     * numbers for Frantic, so a later tuning pass reaches scenes already saved.
     */
    private val _moods = mutableStateListOf<Mood>().apply { addAll(moods) }
    val moods: List<Mood> get() = _moods

    var canvas by mutableStateOf(canvas)
        private set

    private val _selection = mutableStateListOf<Int>()

    /** Indices into [eyes]. Empty means nothing selected. */
    val selection: List<Int> get() = _selection

    /**
     * The canvas ignores touches. Not a "kid mode" — it's for the moment the
     * tablet leaves your hands and goes behind the painting, which is when an
     * accidental drag costs you the alignment you just spent four minutes on.
     * The eyes keep animating, so a locked scene still reads as alive.
     */
    var isLocked by mutableStateOf(false)
        private set

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

    fun toggleLock() {
        isLocked = !isLocked
        if (isLocked) clearSelection()
    }

    fun select(index: Int, additive: Boolean = false) {
        if (isLocked) return
        if (additive) {
            if (!_selection.remove(index)) _selection.add(index)
        } else {
            _selection.clear()
            _selection.add(index)
        }
    }

    fun selectAll() {
        if (isLocked) return
        _selection.clear()
        _selection.addAll(_eyes.indices)
    }

    fun clearSelection() = _selection.clear()

    fun setSelection(indices: Collection<Int>) {
        if (isLocked) return
        _selection.clear()
        _selection.addAll(indices.filter { it in _eyes.indices })
    }

    fun isSelected(index: Int): Boolean = index in _selection

    /** Indices the current operation applies to: the selection, or everything
     *  when nothing is selected. */
    fun activeIndices(): List<Int> =
        if (_selection.isEmpty()) _eyes.indices.toList() else _selection.toList()

    // ---- Undo -----------------------------------------------------------

    /**
     * Take a snapshot before a gesture starts.
     *
     * One undo step per *gesture*, not per frame — a drag that recorded every
     * touch move would need two hundred undos to get back to where it started,
     * which is not undo, it's a punishment.
     */
    fun beginGesture(): DragSession? {
        if (isLocked) return null
        val start = pushUndo() ?: return null
        return DragSession(start)
    }

    /**
     * Record an undo step for a discrete change — a colour, a style, a mood.
     * Unlike [beginGesture] these have no duration, so there's nothing to
     * accumulate and no drag session to hand back.
     */
    fun beginEdit(): Boolean = pushUndo() != null

    private fun pushUndo(): SceneSnapshot? {
        if (isLocked) return null
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
     * Restore in place wherever possible.
     *
     * Rebuilding a `RenderedEye` would give it a fresh `EyeRuntime`, resetting
     * its blink and saccade timers — so undoing a colour change would make
     * every eye on screen blink in unison. Eyes are only reconstructed when the
     * *count* differs, which is the one case where there's nothing to restore
     * onto.
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
        if (!beginEdit()) return
        activeIndices().forEach { index ->
            _moods[index] = mood
            _eyes[index].runtime.behavior = Moods.forMood(mood)
        }
        transformChanged()
    }

    /**
     * Apply a hand-tuned config. Moves the eye to [Mood.Custom], because a
     * scene claiming to be on Frantic while running something else would come
     * back wrong the next time it was opened.
     */
    fun setBehavior(behavior: BehaviorConfig) {
        if (!beginEdit()) return
        activeIndices().forEach { index ->
            _moods[index] = Mood.Custom
            _eyes[index].runtime.behavior = behavior
        }
        transformChanged()
    }

    /** The config the panels should show: the first active eye's. */
    fun activeBehavior(): BehaviorConfig =
        activeIndices().firstOrNull()?.let { _eyes[it].runtime.behavior } ?: Moods.FreeDefault

    /** The mood the panels should show, or null when the selection disagrees. */
    fun activeMood(): Mood? = activeIndices()
        .map { _moods.getOrElse(it) { Mood.IdleScan } }
        .distinct()
        .singleOrNull()

    /** The style the panels should show, or null when the selection disagrees. */
    fun activeStyle(): EyeStyle? = activeIndices()
        .map { _eyes[it].style }
        .distinct()
        .singleOrNull()

    // ---- Place ----------------------------------------------------------

    /** Move by whole canvas pixels. The stepper's 1px and hold-10. */
    fun nudge(dxPx: Float, dyPx: Float, canvasWidthPx: Float, canvasHeightPx: Float) = editActive {
        it.centerX = (it.centerX + dxPx / canvasWidthPx).coerceIn(0f, 1f)
        it.centerY = (it.centerY + dyPx / canvasHeightPx).coerceIn(0f, 1f)
    }

    /**
     * Rotate the active eyes. [RotationMode.Group] orbits them about their
     * shared centre as well as turning each; [RotationMode.Each] only turns
     * them in place.
     */
    fun rotateSelection(
        degrees: Float,
        mode: RotationMode,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
    ) {
        val active = activeIndices()
        if (active.isEmpty() || !beginEdit()) return

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
        if (active.size < 2 || !beginEdit()) return

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

    /**
     * Add an eye, matching the first active one so it lands as a plausible
     * partner rather than a default that has to be re-styled before it's
     * useful. Offset slightly so it isn't hidden under whatever it copied.
     */
    fun addEye() {
        if (!beginEdit()) return
        val template = activeIndices().firstOrNull()?.let { _eyes[it] }
        val added = RenderedEye(
            style = template?.style ?: _eyes.firstOrNull()?.style ?: return,
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
            // A distinct seed, so the new eye doesn't blink in lockstep with
            // the one it was copied from.
            random = Random(_eyes.size + EyeSeedOffset),
        )
        _eyes.add(added)
        _moods.add(activeMood() ?: Mood.IdleScan)
        _selection.clear()
        _selection.add(_eyes.lastIndex)
        transformChanged()
    }

    /**
     * Delete the selection, never the whole scene. With nothing selected this
     * does nothing at all rather than falling through to "everything", because
     * [activeIndices]'s convenience becomes a catastrophe when the verb is
     * delete.
     */
    fun deleteSelection() {
        if (_selection.isEmpty() || _selection.size == _eyes.size) return
        if (!beginEdit()) return

        _selection.sortedDescending().forEach { index ->
            _eyes.removeAt(index)
            _moods.removeAt(index)
        }
        _selection.clear()
        transformChanged()
    }

    // ---- Scene ----------------------------------------------------------

    fun setCanvasRotation(rotation: CanvasRotation) {
        if (!beginEdit()) return
        canvas = canvas.copy(rotation = rotation)
    }

    fun setCanvasColor(color: Long) {
        if (!beginEdit()) return
        canvas = canvas.copy(color = color)
    }

    fun setSleepTimer(timer: Duration?) {
        if (!beginEdit()) return
        canvas = canvas.copy(sleepTimer = timer)
    }

    /** Not undoable, and shouldn't be: brightness is a live comfort control
     *  someone drags while looking at the room, not an edit to the scene. */
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
    )

    /** Distance between the two selected eyes in canvas pixels, for the IPD
     *  readout and the matched-spacing snap. Null unless exactly two. */
    fun selectedSpacingPx(canvasWidthPx: Float, canvasHeightPx: Float): Float? {
        if (_selection.size != 2) return null
        val a = _eyes[_selection[0]]
        val b = _eyes[_selection[1]]
        return hypot((b.centerX - a.centerX) * canvasWidthPx, (b.centerY - a.centerY) * canvasHeightPx)
    }

    private inline fun editActive(block: (RenderedEye) -> Unit) {
        val active = activeIndices()
        if (active.isEmpty() || !beginEdit()) return
        active.forEach { block(_eyes[it]) }
        transformChanged()
    }

    private companion object {
        /**
         * Deep enough to walk back a whole placement session, shallow enough
         * that the stack can't grow unbounded on a device left running for
         * five hours.
         */
        const val MaxUndoSteps = 50

        const val NewEyeOffset = 0.12f

        /** Keeps a new eye's seed clear of the index seeds used when a scene
         *  is loaded, so it can't collide with an existing eye's phase. */
        const val EyeSeedOffset = 1000

        /** Glow beyond about a third of the eye stops reading as bloom and
         *  starts reading as fog. */
        const val MaxGlowFraction = 0.35f

        /** Never fully black: a brightness slider that can reach zero looks
         *  exactly like a crash, and the way back is invisible. */
        const val MinBrightness = 0.05f

        const val MinEyeSizePx = 24f
        const val MaxEyeSizePx = 2000f
    }
}

/**
 * One drag, from finger-down to finger-up.
 *
 * Exists because a snap must not be sticky. The eye's *drawn* position is the
 * snapped one, but the next frame's movement has to be measured from where the
 * finger actually is — otherwise each frame re-snaps from the already-corrected
 * position, the correction cancels the movement, and the eye welds itself to
 * the first guide it touches and never lets go.
 *
 * So the raw position is tracked here and the snap is applied on top of it for
 * display only. That's also exactly the behaviour the design asks for: the
 * guide holds while the finger stays within the threshold, and releases when
 * it doesn't.
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
        // A generous radius: a small eye is still a 56dp target, because
        // fingers are the same size whatever the eye is.
        val radius = maxOf(eye.sizePx / 2f, minimumTouchPx / 2f)
        index to hypot(x - cx, y - cy) / radius
    }
    .filter { it.second <= 1f }
    // Topmost-feeling hit: the one whose centre you're proportionally nearest,
    // so a small eye sitting on top of a big one is still reachable.
    .minByOrNull { it.second }
    ?.first
