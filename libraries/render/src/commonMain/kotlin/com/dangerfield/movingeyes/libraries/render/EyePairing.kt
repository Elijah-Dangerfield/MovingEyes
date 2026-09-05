package com.dangerfield.movingeyes.libraries.render

import kotlin.math.sqrt

/**
 * Works out which eyes are a pair, so each pair can blink on its own timeline.
 *
 * Derived from where the eyes actually are rather than declared anywhere. A
 * preset could carry its pairs, but a scene someone assembles by hand could
 * not, and "the two eyes I put next to each other blink together" is not a
 * thing anyone should have to configure.
 *
 * The rule is mutual nearest neighbour: two eyes pair when each is the other's
 * closest. That is deliberately strict. A row of eyes at even spacing has no
 * mutual pairs beyond the ends and correctly gets none — three eyes in a line
 * are not a face and a half.
 *
 * Anything unpaired gets a group to itself, so it keeps blinking rather than
 * being lumped in with a stranger.
 */
internal fun blinkGroupsFor(eyes: List<RenderedEye>): IntArray {
    val groups = IntArray(eyes.size) { UNASSIGNED }
    if (eyes.size < 2) return IntArray(eyes.size)

    val nearest = IntArray(eyes.size) { index ->
        var best = -1
        var bestDistance = Float.MAX_VALUE
        eyes.indices.forEach { other ->
            if (other == index) return@forEach
            val distance = separation(eyes[index], eyes[other])
            if (distance < bestDistance) {
                bestDistance = distance
                best = other
            }
        }
        best
    }

    var next = 0
    eyes.indices.forEach { index ->
        if (groups[index] != UNASSIGNED) return@forEach
        val partner = nearest[index]
        // Mutual, or it isn't a pair — the eye at the end of a row is somebody's
        // nearest without them being its.
        if (partner >= 0 && nearest[partner] == index) {
            groups[index] = next
            groups[partner] = next
        } else {
            groups[index] = next
        }
        next++
    }
    return groups
}

/**
 * Distance in units of the two eyes' own size, not in pixels.
 *
 * A wall has near eyes and far ones, and in raw pixels a far pair's separation
 * is smaller than a near eye's own width — so a distant pair would look closer
 * to a near eye than to its own partner.
 */
private fun separation(a: RenderedEye, b: RenderedEye): Float {
    val dx = a.centerX - b.centerX
    val dy = a.centerY - b.centerY
    val scale = (a.sizePx + b.sizePx) / 2f
    return if (scale <= 0f) Float.MAX_VALUE else sqrt(dx * dx + dy * dy) / scale
}

private const val UNASSIGNED = -1
