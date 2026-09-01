@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.editor.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dangerfield.movingeyes.libraries.render.RenderedEye
import kotlin.math.hypot

/** One eye's placement, as much of it as undo needs to restore. */
data class EyeTransform(
    val centerX: Float,
    val centerY: Float,
    val sizePx: Float,
    val rotationDegrees: Float,
)

/** The whole canvas at one moment. What an undo step actually is. */
data class SceneSnapshot(val transforms: List<EyeTransform>)

/**
 * Selection, transforms, and undo for the editor.
 *
 * ## Two kinds of state, deliberately kept apart
 *
 * **Positions live on [RenderedEye]** and are plain mutable fields. The
 * renderer reads them inside its draw lambda every frame, so moving an eye
 * shows up on the next frame with no recomposition at all — a drag doesn't
 * invalidate composition sixty times a second.
 *
 * **Selection lives in Compose state**, because it changes on a tap rather
 * than on a frame, and the overlay and readout genuinely need to recompose
 * when it does.
 *
 * Getting that split wrong is the difference between a drag that feels direct
 * and one that stutters, which on this app is the difference between an
 * alignment you trust and one you don't.
 */
class EditorState(val eyes: List<RenderedEye>) {

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

    /** Bumped whenever a transform changes, so the readout recomposes.
     *  Positions themselves stay off Compose state — see the class doc. */
    var transformRevision by mutableStateOf(0)
        private set

    private val undoStack = ArrayDeque<SceneSnapshot>()
    private val redoStack = ArrayDeque<SceneSnapshot>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

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
        _selection.addAll(eyes.indices)
    }

    fun clearSelection() = _selection.clear()

    fun setSelection(indices: Collection<Int>) {
        if (isLocked) return
        _selection.clear()
        _selection.addAll(indices)
    }

    /** True when [index] is selected, or when nothing is and everything moves. */
    fun isSelected(index: Int): Boolean = index in _selection

    /**
     * Take a snapshot before a gesture starts.
     *
     * One undo step per *gesture*, not per frame — a drag that recorded every
     * touch move would need two hundred undos to get back to where it started,
     * which is not undo, it's a punishment.
     */
    fun beginGesture(): DragSession? {
        if (isLocked) return null
        val start = snapshot()
        undoStack.addLast(start)
        if (undoStack.size > MaxUndoSteps) undoStack.removeFirst()
        redoStack.clear()
        refreshUndoState()
        return DragSession(start)
    }

    /** Call after mutating transforms so the readout picks it up. */
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
        eyes.map { EyeTransform(it.centerX, it.centerY, it.sizePx, it.rotationDegrees) },
    )

    fun restore(snapshot: SceneSnapshot) {
        snapshot.transforms.forEachIndexed { index, transform ->
            eyes.getOrNull(index)?.apply {
                centerX = transform.centerX
                centerY = transform.centerY
                sizePx = transform.sizePx
                rotationDegrees = transform.rotationDegrees
            }
        }
        transformChanged()
    }

    /** Indices the current gesture applies to: the selection, or everything
     *  when nothing is selected. */
    fun activeIndices(): List<Int> =
        if (_selection.isEmpty()) eyes.indices.toList() else _selection.toList()

    private fun refreshUndoState() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private companion object {
        /**
         * Deep enough to walk back a whole placement session, shallow enough
         * that the stack can't grow unbounded on a device left running for
         * five hours.
         */
        const val MaxUndoSteps = 50
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
        val transform = start.transforms.getOrNull(index) ?: return null
        return (transform.centerX + panX) to (transform.centerY + panY)
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
