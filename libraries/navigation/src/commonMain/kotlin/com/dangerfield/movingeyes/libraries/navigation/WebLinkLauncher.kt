package com.dangerfield.movingeyes.libraries.navigation

import com.dangerfield.movingeyes.libraries.core.Catching

fun interface WebLinkLauncher {
    fun open(url: String): Catching<Unit>
}
