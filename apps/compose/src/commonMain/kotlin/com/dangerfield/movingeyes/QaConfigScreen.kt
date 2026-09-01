package com.dangerfield.movingeyes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.libraries.config.ConfiguredValue
import com.dangerfield.movingeyes.libraries.ui.components.HorizontalDivider
import com.dangerfield.movingeyes.libraries.ui.components.Switch
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension

/**
 * The QA menu: every [ConfiguredValue] in the app graph, with a local override.
 *
 * Values contribute themselves into a `Set<QaConfigValue>` multibinding, so a
 * new flag shows up here the moment it exists — nobody has to remember to add
 * a row. Overrides persist locally and outrank the remote config, which is
 * what makes `RealPurchasesEnabled` flippable on a device that has no
 * provisioned store catalog.
 *
 * Debug-only, like everything else behind the shake.
 */
@Composable
fun QaConfigScreen(
    values: List<ConfiguredValue<*>>,
    overrides: Map<String, Any>,
    isUnlocked: Boolean,
    lastBillingResult: String?,
    onOverride: (path: String, value: Any) -> Unit,
    onClearAll: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onClearEntitlement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = values
        .filter { it.showInQADashboard }
        .sortedBy { it.path }
        .groupBy { it.group }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D700),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Dimension.D300)) {
                Text(text = "Config", typography = AppTheme.typography.Display.D1000)
                Text(
                    text = "Local overrides. These outrank the remote config and survive a " +
                        "restart, so remember to clear them before trusting a bug report.",
                    typography = AppTheme.typography.Body.B400,
                    color = AppTheme.colors.textSecondary,
                )
                Button(
                    onClick = onClearAll,
                    size = ButtonSize.Small,
                    style = ButtonStyle.Outlined,
                ) {
                    Text("Clear all overrides")
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                Text(
                    text = "billing",
                    typography = AppTheme.typography.Readout.R300,
                    color = AppTheme.colors.accentPrimary,
                    allCaps = true,
                )
                Text(
                    text = if (isUnlocked) "Unlocked" else "Free tier",
                    typography = AppTheme.typography.Heading.H600,
                    color = if (isUnlocked) AppTheme.colors.accentPrimary else AppTheme.colors.text,
                )
                lastBillingResult?.let {
                    Text(
                        text = it,
                        typography = AppTheme.typography.Readout.R400,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimension.D400)) {
                    Button(onClick = onPurchase, size = ButtonSize.Small) { Text("Buy") }
                    Button(onClick = onRestore, size = ButtonSize.Small, style = ButtonStyle.Outlined) {
                        Text("Restore")
                    }
                    Button(onClick = onClearEntitlement, size = ButtonSize.Small, style = ButtonStyle.Text) {
                        Text("Clear")
                    }
                }
                HorizontalDivider()
            }
        }

        grouped.forEach { (group, groupValues) ->
            item {
                Text(
                    text = group,
                    typography = AppTheme.typography.Readout.R300,
                    color = AppTheme.colors.accentPrimary,
                    allCaps = true,
                )
            }

            items(groupValues, key = { it.path }) { value ->
                ConfigRow(
                    value = value,
                    isOverridden = overrides.containsKey(value.path),
                    onOverride = onOverride,
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ConfigRow(
    value: ConfiguredValue<*>,
    isOverridden: Boolean,
    onOverride: (path: String, value: Any) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimension.D400),
        verticalArrangement = Arrangement.spacedBy(Dimension.D300),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = value.name, typography = AppTheme.typography.Label.L500.SemiBold)
                Text(
                    text = value.path,
                    typography = AppTheme.typography.Readout.R300,
                    color = if (isOverridden) {
                        AppTheme.colors.accentPrimary
                    } else {
                        AppTheme.colors.textTertiary
                    },
                )
            }

            val current = value.value
            if (current is Boolean) {
                Switch(
                    checked = current,
                    onCheckedChange = { onOverride(value.path, it) },
                )
            }
        }

        value.description?.let {
            Text(
                text = it,
                typography = AppTheme.typography.Caption.C400,
                color = AppTheme.colors.textSecondary,
            )
        }

        val current = value.value
        if (current !is Boolean) {
            OutlinedTextField(
                value = current.toString(),
                onValueChange = { onOverride(value.path, it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                typographyToken = AppTheme.typography.Readout.R500,
            )
        }
    }
}
