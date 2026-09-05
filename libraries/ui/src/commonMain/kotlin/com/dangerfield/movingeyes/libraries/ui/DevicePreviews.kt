package com.dangerfield.movingeyes.libraries.ui

import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Renders a composable at both shapes this app actually ships in.
 *
 * Worth having as one annotation rather than two hand-written `@Preview`s:
 * Moving Eyes is a tablet appliance that people also set up on a phone, and the
 * layout genuinely differs — past [RailBreakpointDp] the controls become a side
 * rail and stop eating height. A component previewed only at phone width has
 * never been seen in the shape most owners will use it in.
 *
 * The sizes are dp of a real 10" tablet in landscape and a common phone in
 * portrait, not round numbers, so the tablet case lands the correct side of the
 * breakpoint by a realistic margin rather than by one pixel.
 */
@Preview(name = "Phone", widthDp = PhoneWidthDp, heightDp = PhoneHeightDp)
@Preview(name = "Tablet", widthDp = TabletWidthDp, heightDp = TabletHeightDp)
annotation class PhoneAndTabletPreview

/** Landscape 10", the reference device this is built to run on. */
const val TabletWidthDp = 1024
const val TabletHeightDp = 640

const val PhoneWidthDp = 411
const val PhoneHeightDp = 891
