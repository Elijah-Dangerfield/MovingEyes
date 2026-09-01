package com.dangerfield.movingeyes.libraries.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import com.dangerfield.movingeyes.system.typography.TypographyResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val TYPE_SUBTITLE =
    "IBM Plex Sans for text, IBM Plex Mono for every number. Higher number = bigger. " +
        "Modifiers chain off any token: .Bold, .SemiBold, .Medium, …"

/** The type page body. Split into two halves so [DesignSystemPreview] can lay it out in two
 *  columns; call this combined version for a single-column page. */
@Composable
internal fun TypographyCatalogBody() {
    TypographyCatalogBodyHeadlines()
    TypographyCatalogBodyText()
}

/** First half — the big stuff: serif Display, the Brand face, and sans Heading. */
@Composable
internal fun TypographyCatalogBodyHeadlines() {
    val t = AppTheme.typography

    CatalogSection(
        "Display",
        "Hero headlines only — the paywall promise, a first-run card. Rare by design.",
    ) {
        TypeRow("Display.D1500", t.Display.D1500, "Make them move")
        TypeRow("Display.D1300", t.Display.D1300, "Make them move")
        TypeRow("Display.D1100", t.Display.D1100, "Make them move")
        TypeRow("Display.D1000", t.Display.D1000, "Make them move")
        TypeRow("Display.D900", t.Display.D900, "Make them move")
    }

    CatalogSection(
        "Heading · sans",
        "Section and screen titles. Step down the scale as the heading nests deeper.",
    ) {
        TypeRow("Heading.H1100", t.Heading.H1100, "Activity")
        TypeRow("Heading.H900", t.Heading.H900, "Activity")
        TypeRow("Heading.H800", t.Heading.H800, "Activity")
        TypeRow("Heading.H700", t.Heading.H700, "Activity")
        TypeRow("Heading.H600", t.Heading.H600, "Activity")
        TypeRow("Heading.H500", t.Heading.H500, "Activity")
        TypeRow("Heading.H400", t.Heading.H400, "Activity")
    }
}

/** Second half — running text: sans Body, Label, Caption. */
@Composable
internal fun TypographyCatalogBodyText() {
    val t = AppTheme.typography

    CatalogSection(
        "Body · sans",
        "Running prose — descriptions, dialog bodies, explainers. Optimized for reading, not labels.",
    ) {
        TypeRow("Body.B700", t.Body.B700, "Your changes sync automatically across devices.")
        TypeRow("Body.B600", t.Body.B600, "Your changes sync automatically across devices.")
        TypeRow("Body.B500", t.Body.B500, "Your changes sync automatically across devices.")
        TypeRow("Body.B400", t.Body.B400, "Your changes sync automatically across devices.")
    }

    CatalogSection(
        "Label · sans (UI elements)",
        "Buttons, chips, tabs — tight line height tuned for single-line UI, not paragraphs.",
    ) {
        TypeRow("Label.L700", t.Label.L700, "Continue")
        TypeRow("Label.L600", t.Label.L600, "Continue")
        TypeRow("Label.L500", t.Label.L500, "Continue")
        TypeRow("Label.L400", t.Label.L400, "Continue")
        TypeRow("Label.L300", t.Label.L300, "Continue")
    }

    CatalogSection(
        "Caption · sans (metadata)",
        "The smallest type — helper text, section eyebrows. Pair with textSecondary.",
    ) {
        TypeRow("Caption.C400", t.Caption.C400, "Free scenes blink on a fixed default")
        TypeRow("Caption.C300", t.Caption.C300, "Free scenes blink on a fixed default")
        TypeRow("Caption.C200", t.Caption.C200, "Free scenes blink on a fixed default")
    }

    CatalogSection(
        "Readout · mono (numbers)",
        "Every number a user sees. Monospace so a live value can't reflow the label beside it, " +
            "and because tabular digits are what make this read as an instrument.",
    ) {
        TypeRow("Readout.R500", t.Readout.R500, "2 eyes · 148 px · 31.3 mm · 0°")
        TypeRow("Readout.R400", t.Readout.R400, "X 597   Y 384   IPD 220 px")
        TypeRow("Readout.R300", t.Readout.R300, "86% · charging")
    }
}

@Preview(widthDp = 1100, heightDp = 2600)
@Composable
private fun TypographyCatalog() {
    CatalogPage(title = "Typography", subtitle = TYPE_SUBTITLE) { TypographyCatalogBody() }
}

@Composable
private fun TypeRow(token: String, typography: TypographyResource, sample: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimension.D700),
    ) {
        Text(
            text = token,
            typography = AppTheme.typography.Caption.C300,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.width(180.dp),
        )
        Column { Text(text = sample, typography = typography) }
    }
}
