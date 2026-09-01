package com.dangerfield.movingeyes.system.typography

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import movingeyes.libraries.ui.generated.resources.IBMPlexMono_Medium
import movingeyes.libraries.ui.generated.resources.IBMPlexMono_Regular
import movingeyes.libraries.ui.generated.resources.IBMPlexMono_SemiBold
import movingeyes.libraries.ui.generated.resources.IBMPlexSans_Bold
import movingeyes.libraries.ui.generated.resources.IBMPlexSans_Medium
import movingeyes.libraries.ui.generated.resources.IBMPlexSans_Regular
import movingeyes.libraries.ui.generated.resources.IBMPlexSans_SemiBold
import movingeyes.libraries.ui.generated.resources.Res

/**
 * Two families, both IBM Plex, both bundled — no system fallbacks, so a
 * readout lines up identically on a Pixel and an iPad.
 *
 * The pairing is the whole visual argument: Plex Sans carries text at heavier
 * weights than a typical app would use, and Plex Mono carries **every number** —
 * coordinates, millimetres, hex, battery, countdown. Numbers being monospaced
 * is the tell that this is an instrument rather than a toy, and it's the reason
 * a mono is worth 460KB of binary.
 *
 * Light and Italic are deliberately absent. Nothing in this UI is quiet enough
 * to want Light at arm's length in a dark room, and there is no long-form prose
 * to italicise.
 */
val SansSerifFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(resource = Res.font.IBMPlexSans_Regular, weight = FontWeight.Normal),
        Font(resource = Res.font.IBMPlexSans_Medium, weight = FontWeight.Medium),
        Font(resource = Res.font.IBMPlexSans_SemiBold, weight = FontWeight.SemiBold),
        Font(resource = Res.font.IBMPlexSans_Bold, weight = FontWeight.Bold),
    )

/** Every number in the app. See [SansSerifFontFamily] for why. */
val MonoFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(resource = Res.font.IBMPlexMono_Regular, weight = FontWeight.Normal),
        Font(resource = Res.font.IBMPlexMono_Medium, weight = FontWeight.Medium),
        Font(resource = Res.font.IBMPlexMono_SemiBold, weight = FontWeight.SemiBold),
    )

/**
 * The app has no display face. Kept as an alias so callers that reach for a
 * "brand" family get Plex Sans rather than a compile error or, worse, a
 * script font on a haunted painting.
 */
val BrandFontFamily: FontFamily
    @Composable get() = SansSerifFontFamily
