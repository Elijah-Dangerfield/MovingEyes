@file:Suppress("MagicNumber")

package com.dangerfield.movingeyes.features.paywall.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dangerfield.movingeyes.libraries.billing.BillingProduct
import com.dangerfield.movingeyes.libraries.eyes.EyeStyles
import com.dangerfield.movingeyes.libraries.eyes.Moods
import com.dangerfield.movingeyes.libraries.render.EyePreview
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.paywall_buy
import movingeyes.libraries.resources.generated.resources.paywall_buy_no_price
import movingeyes.libraries.resources.generated.resources.paywall_close
import movingeyes.libraries.resources.generated.resources.paywall_live_label
import movingeyes.libraries.resources.generated.resources.paywall_once
import movingeyes.libraries.resources.generated.resources.paywall_restore
import movingeyes.libraries.resources.generated.resources.paywall_title
import movingeyes.libraries.resources.generated.resources.paywall_value_motion
import movingeyes.libraries.resources.generated.resources.paywall_value_reactivity
import movingeyes.libraries.resources.generated.resources.paywall_value_styles
import org.jetbrains.compose.resources.stringResource

/**
 * The strip across the top runs Frantic live rather than listing what Frantic
 * is. Motion is the whole product and the one thing a screenshot can't carry,
 * so the pitch has to be the thing itself.
 *
 * No countdown and no scarcity: this gets bought on impulse by someone who
 * already likes the app, and pressure only produces refunds.
 */
@Composable
fun PaywallScreen(
    product: BillingProduct?,
    isPurchasing: Boolean,
    message: String?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.surfacePrimary.color)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D700),
    ) {
        LiveStrip()

        Text(
            text = stringResource(Res.string.paywall_title),
            typography = AppTheme.typography.Display.D1000,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimension.D400)) {
            ValueRow(stringResource(Res.string.paywall_value_motion))
            ValueRow(stringResource(Res.string.paywall_value_styles))
            ValueRow(stringResource(Res.string.paywall_value_reactivity))
        }

        Text(
            text = stringResource(Res.string.paywall_once),
            typography = AppTheme.typography.Body.B600,
            color = AppTheme.colors.textSecondary,
        )

        if (message != null) {
            Text(
                text = message,
                typography = AppTheme.typography.Body.B600,
                color = AppTheme.colors.accentPrimary,
            )
        }

        Button(
            onClick = onPurchase,
            enabled = !isPurchasing,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                product?.displayPrice
                    ?.let { stringResource(Res.string.paywall_buy, it) }
                    ?: stringResource(Res.string.paywall_buy_no_price),
            )
        }

        // Permanently visible, not hidden behind a menu: Apple requires a
        // restore mechanism for non-consumables whether or not anyone needs it.
        Button(
            onClick = onRestore,
            enabled = !isPurchasing,
            size = ButtonSize.Medium,
            style = ButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.paywall_restore))
        }

        Button(
            onClick = onClose,
            size = ButtonSize.Medium,
            style = ButtonStyle.Text,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.paywall_close))
        }
    }
}

@Composable
private fun LiveStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StripHeight)
            .clip(RoundedCornerShape(StripCornerRadius))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D700)) {
            EyePreview(
                style = EyeStyles.HumanRealistic,
                sizeDp = StripEyeSize,
                behavior = Moods.Frantic,
                seed = 1,
            )
            EyePreview(
                style = EyeStyles.HumanRealistic,
                sizeDp = StripEyeSize,
                behavior = Moods.Frantic,
                seed = 2,
            )
        }

        Text(
            text = stringResource(Res.string.paywall_live_label),
            typography = AppTheme.typography.Caption.C300,
            color = AppTheme.colors.accentPrimary,
            allCaps = true,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimension.D500),
        )
    }
}

@Composable
private fun ValueRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimension.D400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BulletSize)
                .clip(RoundedCornerShape(percent = 50))
                .background(AppTheme.colors.accentPrimary.color),
        )
        Text(text = text, typography = AppTheme.typography.Body.B700)
    }
}

private val StripHeight = 180.dp
private val StripEyeSize = 84.dp
private val StripCornerRadius = 14.dp
private val BulletSize = 8.dp
