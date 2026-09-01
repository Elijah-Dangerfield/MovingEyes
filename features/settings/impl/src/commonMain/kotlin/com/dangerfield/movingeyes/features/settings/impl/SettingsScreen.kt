package com.dangerfield.movingeyes.features.settings.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dangerfield.movingeyes.libraries.ui.components.HorizontalDivider
import com.dangerfield.movingeyes.libraries.ui.components.Switch
import com.dangerfield.movingeyes.libraries.ui.components.button.Button
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonSize
import com.dangerfield.movingeyes.libraries.ui.components.button.ButtonStyle
import com.dangerfield.movingeyes.libraries.ui.components.text.Text
import com.dangerfield.movingeyes.system.AppTheme
import com.dangerfield.movingeyes.system.Dimension
import movingeyes.libraries.resources.generated.resources.Res
import movingeyes.libraries.resources.generated.resources.settings_mute
import movingeyes.libraries.resources.generated.resources.settings_plan_free
import movingeyes.libraries.resources.generated.resources.settings_plan_unlocked
import movingeyes.libraries.resources.generated.resources.settings_privacy
import movingeyes.libraries.resources.generated.resources.settings_reduce_flashing
import movingeyes.libraries.resources.generated.resources.settings_reduce_flashing_detail
import movingeyes.libraries.resources.generated.resources.settings_restore
import movingeyes.libraries.resources.generated.resources.settings_support
import movingeyes.libraries.resources.generated.resources.settings_title
import movingeyes.libraries.resources.generated.resources.settings_unlock
import movingeyes.libraries.resources.generated.resources.settings_version
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    isUnlocked: Boolean,
    reduceFlashing: Boolean,
    muteAllSound: Boolean,
    versionName: String,
    onReduceFlashingChange: (Boolean) -> Unit,
    onMuteChange: (Boolean) -> Unit,
    onUnlock: () -> Unit,
    onRestore: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimension.D800),
        verticalArrangement = Arrangement.spacedBy(Dimension.D600),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            typography = AppTheme.typography.Display.D1000,
        )

        Text(
            text = stringResource(
                if (isUnlocked) Res.string.settings_plan_unlocked else Res.string.settings_plan_free,
            ),
            typography = AppTheme.typography.Body.B700,
            color = if (isUnlocked) AppTheme.colors.accentPrimary else AppTheme.colors.textSecondary,
        )

        if (!isUnlocked) {
            Button(
                onClick = onUnlock,
                size = ButtonSize.Medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.settings_unlock))
            }
        }

        // Visible whether or not the plan says unlocked: someone who switched
        // store accounts sees "Free" and this is the button that fixes it.
        Button(
            onClick = onRestore,
            size = ButtonSize.Medium,
            style = ButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.settings_restore))
        }

        HorizontalDivider()

        ToggleRow(
            title = stringResource(Res.string.settings_reduce_flashing),
            detail = stringResource(Res.string.settings_reduce_flashing_detail),
            checked = reduceFlashing,
            onCheckedChange = onReduceFlashingChange,
        )

        ToggleRow(
            title = stringResource(Res.string.settings_mute),
            detail = null,
            checked = muteAllSound,
            onCheckedChange = onMuteChange,
        )

        HorizontalDivider()

        LinkRow(stringResource(Res.string.settings_privacy), onOpenPrivacy)
        LinkRow(stringResource(Res.string.settings_support), onOpenSupport)

        Text(
            text = stringResource(Res.string.settings_version, versionName),
            typography = AppTheme.typography.Caption.C300,
            color = AppTheme.colors.textTertiary,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimension.D200),
        ) {
            Text(text = title, typography = AppTheme.typography.Body.B700)
            if (detail != null) {
                Text(
                    text = detail,
                    typography = AppTheme.typography.Caption.C300,
                    color = AppTheme.colors.textTertiary,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        typography = AppTheme.typography.Body.B700,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimension.D300),
    )
}
