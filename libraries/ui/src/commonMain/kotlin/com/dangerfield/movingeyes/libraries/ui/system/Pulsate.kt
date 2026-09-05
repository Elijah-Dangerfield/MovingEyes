package com.dangerfield.movingeyes.libraries.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.dangerfield.movingeyes.libraries.ui.components.rememberLoopingFloat

/**
 * Breathes between 1x and [scale], forever.
 *
 * The value is read inside `graphicsLayer`, so the animation invalidates draw
 * and nothing else. Reading it in the composable body would recompose whatever
 * this modifier is attached to sixty times a second for as long as it is on
 * screen — and a pulsate is on screen precisely to draw attention to something,
 * which usually means it is attached to text.
 */
fun Modifier.pulsate(scale: Float = 1.2f) = composed {
    val pulse = rememberLoopingFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(
            animation = tween(PulseMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulsate",
    )

    graphicsLayer {
        scaleX = pulse.value
        scaleY = pulse.value
    }
}

private const val PulseMillis = 500
