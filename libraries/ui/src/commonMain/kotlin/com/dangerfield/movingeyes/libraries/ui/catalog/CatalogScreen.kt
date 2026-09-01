package com.dangerfield.movingeyes.libraries.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.libraries.ui.components.SegmentedControl
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension

/** The areas of the system, as separate pages rather than one long scroll. */
private enum class CatalogArea(val label: String, val blurb: String) {
    Colour("Colour", "Surfaces, the text ramp, the one accent, and the border ladder."),
    Type("Type", "IBM Plex Sans for text, IBM Plex Mono for every number."),
    Editor("Editor", "The parts that exist because this is an alignment instrument."),
    Controls("Controls", "Buttons, fields, toggles, and the radius scale."),
}

/**
 * The design system, on a device.
 *
 * The IDE previews in this package are the reference documentation, but they
 * render on a bright desktop monitor — the one context this app is never used
 * in. Safelight is graphite on true black at arm's length in a dark room, and
 * whether a contrast step holds up there is not a question a preview pane can
 * answer. So the catalog is also reachable in the running app, via shake →
 * debug menu (or `movingeyes://design-system`), in debug builds.
 *
 * Paged rather than one scroll, because a single column of the whole system is
 * several minutes of thumb work to cross and nobody audits what they can't
 * reach.
 */
@Composable
fun CatalogScreen(modifier: Modifier = Modifier) {
    var area by remember { mutableStateOf(CatalogArea.Colour) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D700),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D300)) {
            Text(text = "Safelight", typography = AppTheme.typography.Display.D1000)
            Text(
                text = area.blurb,
                typography = AppTheme.typography.Body.B400,
                color = AppTheme.colors.textSecondary,
            )
        }

        SegmentedControl(
            options = CatalogArea.entries,
            selected = area,
            onSelect = { area = it },
            label = { it.label },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimension.D900),
        ) {
            when (area) {
                CatalogArea.Colour -> {
                    ColorSurfacesContent()
                    CatalogDivider()
                    ColorAccents()
                    CatalogDivider()
                    ColorStatusBorders()
                }

                CatalogArea.Type -> TypographyCatalogBody()

                CatalogArea.Editor -> EditorCatalogBody()

                CatalogArea.Controls -> {
                    ButtonCatalogBody()
                    CatalogDivider()
                    FormCatalogBody()
                    CatalogDivider()
                    RadiiCatalogBody()
                }
            }
        }
    }
}
