package com.dangerfield.movingeyes.libraries.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.components.ColorField
import com.dangerfield.movingeyes.libraries.ui.components.LockBadge
import com.dangerfield.movingeyes.libraries.ui.components.LockLabel
import com.dangerfield.movingeyes.libraries.ui.components.ReadoutField
import com.dangerfield.movingeyes.libraries.ui.components.ReadoutPill
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.Stepper
import com.dangerfield.movingeyes.libraries.ui.components.StyleCard
import com.dangerfield.movingeyes.libraries.ui.components.Tip
import com.dangerfield.movingeyes.libraries.ui.components.TipsSheet
import com.dangerfield.movingeyes.libraries.ui.components.ToastAction
import com.dangerfield.movingeyes.libraries.ui.components.ToastBar
import com.dangerfield.movingeyes.system.Dimension
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The Safelight-specific parts of the system — the pieces that exist because
 * this app is an alignment instrument rather than a generic mobile app.
 *
 * Everything here is live, including the steppers and the hex fields. The point
 * of having the catalog on a device is to poke it in the dark room it's for.
 */
@Composable
internal fun EditorCatalogBody() {
    CatalogSection(
        "Segmented control",
        "The editor's Place / Look / Motion / Scene tabs, and Group / Each on the transform row. " +
            "Four options max — beyond that a segment stops clearing the 56dp touch minimum.",
    ) {
        var tab by remember { mutableStateOf("Look") }
        var rotationMode by remember { mutableStateOf("Group") }
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D500)) {
            SegmentedControl(
                options = listOf("Place", "Look", "Motion", "Scene"),
                selected = tab,
                onSelect = { tab = it },
                label = { it },
            )
            SegmentedControl(
                options = listOf("Group", "Each"),
                selected = rotationMode,
                onSelect = { rotationMode = it },
                label = { it },
            )
        }
    }

    CatalogSection(
        "Readouts",
        "Every number the user sees, in mono. A live value must never reflow the label beside it.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D500)) {
            ReadoutPill(text = "2 eyes · 148 px · 31.3 mm · 0°")
            ReadoutPill(text = "Snapped to centre and to 46.6 mm")
            ReadoutPill(text = "86% · charging", emphasized = false)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D800)) {
                ReadoutField(label = "X", value = "597")
                ReadoutField(label = "Y", value = "384")
                ReadoutField(label = "IPD", value = "220 px")
            }
        }
    }

    CatalogSection(
        "Stepper",
        "One unit a tap, ten a tick while held, with a haptic on each. What's left of the nudge " +
            "pad: dragging moves an eye, but the last two millimetres aren't a drag problem.",
    ) {
        var value by remember { mutableIntStateOf(597) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimension.D800),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stepper(onStep = { value += it })
            ReadoutField(label = "X", value = value.toString())
        }
    }

    CatalogSection(
        "Colour",
        "Hex entry is required, not a nicety — a hue wheel alone is unusable with low vision, and " +
            "people match eyes to a colour they already have. No eyedropper: it would mean a camera.",
    ) {
        var sclera by remember { mutableStateOf(Color(0xFFF0E9D8)) }
        var iris by remember { mutableStateOf(Color(0xFFC8D24A)) }
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D600)) {
            ColorField(
                label = "Sclera",
                color = sclera,
                onColorChange = { sclera = it },
                recents = listOf(Color(0xFFF1EBE0), Color(0xFFCFC9BE), Color(0xFF2A1A16)),
            )
            ColorField(label = "Iris", color = iris, onColorChange = { iris = it })
        }
    }

    CatalogSection(
        "Style card & lock",
        "A locked style is never dimmed. It keeps animating at full strength and carries a badge, " +
            "because motion is the thing a screenshot can't sell.",
    ) {
        var selected by remember { mutableStateOf("Cartoon") }
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D600)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D500)) {
                StyleSample("Cartoon", selected, Color(0xFF4FA3C7), locked = false) { selected = it }
                StyleSample("Human", selected, Color(0xFF7A5A34), locked = false) { selected = it }
                StyleSample("Feline", selected, Color(0xFFC8D24A), locked = true) { selected = it }
            }
            LockLabel(text = "Unlock $4.99")
        }
    }

    CatalogSection(
        "Toast bar",
        "Never dims, never takes focus. It appears over a canvas someone spent four minutes " +
            "aligning, sometimes over a scene already mounted behind cardboard.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D500)) {
            ToastBar(
                visible = true,
                message = "Kept your sizes.",
                onDismiss = {},
                actions = listOf(ToastAction("Refit") {}, ToastAction("Undo") {}),
                autoDismissAfter = null,
            )
            ToastBar(
                visible = true,
                message = "Demo ended",
                supporting = "Back to the free motion.",
                onDismiss = {},
                actions = listOf(ToastAction("Keep Frantic") {}),
                autoDismissAfter = null,
            )
        }
    }

    CatalogSection(
        "Tips sheet",
        "What replaced onboarding. Sits under a canvas that's already blinking; never covers it, " +
            "never blocks a gesture, Skip on every page.",
    ) {
        var page by remember { mutableIntStateOf(0) }
        TipsSheet(
            tips = SampleTips,
            pageIndex = page,
            onNext = { page = (page + 1) % SampleTips.size },
            onSkip = { page = 0 },
            eyebrow = "TIPS",
            skipLabel = "Skip",
            nextLabel = "Next",
            doneLabel = "Done",
            counter = { page, total -> "$page of $total" },
        )
    }
}

@Composable
private fun StyleSample(
    label: String,
    selected: String,
    iris: Color,
    locked: Boolean,
    onSelect: (String) -> Unit,
) {
    StyleCard(
        label = label,
        isSelected = selected == label,
        isLocked = locked,
        onClick = { onSelect(label) },
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(iris),
        )
    }
}

private val SampleTips = listOf(
    Tip(
        title = "Drag an eye. Pinch to size it.",
        body = "The canvas is the screen at actual size, so what you see here is what gets taped " +
            "to the cardboard. Eyes snap to each other and to the middle of the screen as you go.",
    ),
    Tip(
        title = "Turn a pair with the handle above the box.",
        body = "Select both, then twist with two fingers or pull the handle. Switch to Each in " +
            "the Place tab when you want them squinting at each other instead.",
    ),
    Tip(
        title = "Lock the canvas before you mount it.",
        body = "The padlock in the toolbar freezes everything in place. Useful the moment the " +
            "tablet leaves your hands and goes behind the painting.",
    ),
    Tip(
        title = "Press play when it looks right.",
        body = "The chrome fades out and the eyes take over. Swipe back from the edge to return. " +
            "Nothing on screen will respond to a poke, which is the whole idea.",
    ),
)

@Preview(widthDp = 1000, heightDp = 2600)
@Composable
private fun EditorCatalog() {
    CatalogPage(
        title = "Editor parts",
        subtitle = "The pieces that exist because this is an alignment instrument, not a generic app.",
    ) {
        EditorCatalogBody()
    }
}
