package com.makkispacejam.fluxa.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ThemeMode
import com.makkispacejam.fluxa.ui.components.settings.components.SettingItem
import com.makkispacejam.fluxa.ui.components.settings.components.SettingItemPosition
import com.makkispacejam.fluxa.ui.components.settings.components.SettingItemWithSwitch

@Composable
fun SettingsGeneralSection(
    themeMode: ThemeMode,
    amoledMode: Boolean,
    onAmoledModeChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    selectedLanguage: String,
    selectedTranslationLang: String,
    selectedQuality: String,
    selectedRegion: String,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onTranslationClick: () -> Unit,
    onQualityClick: () -> Unit,
    onRegionClick: () -> Unit,
    audioNormalizerEnabled: Boolean,
    onAudioNormalizerChange: (Boolean) -> Unit,
    onOpenLinksClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.section_general),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingItem(
                    title = stringResource(R.string.appearance),
                    subtitle = stringResource(themeMode.titleRes),
                    onClick = onThemeClick,
                    position = SettingItemPosition.First
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItemWithSwitch(
                    title = stringResource(R.string.amoled_mode),
                    subtitle = stringResource(R.string.amoled_mode_desc),
                    checked = amoledMode,
                    onCheckedChange = onAmoledModeChange,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItemWithSwitch(
                    title = stringResource(R.string.incognito_mode),
                    subtitle = stringResource(R.string.incognito_mode_desc),
                    checked = incognitoMode,
                    onCheckedChange = onIncognitoModeChange,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItemWithSwitch(
                    title = stringResource(R.string.normalize_audio),
                    subtitle = stringResource(R.string.normalize_audio_desc),
                    checked = audioNormalizerEnabled,
                    onCheckedChange = onAudioNormalizerChange,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.language),
                    subtitle = selectedLanguage,
                    onClick = onLanguageClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.content_translation),
                    subtitle = selectedTranslationLang,
                    onClick = onTranslationClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.video_quality),
                    subtitle = selectedQuality,
                    onClick = onQualityClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.region),
                    subtitle = selectedRegion,
                    onClick = onRegionClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.open_links),
                    subtitle = stringResource(R.string.open_links_desc),
                    onClick = onOpenLinksClick,
                    position = SettingItemPosition.Last
                )
            }
        }
    }
}

@Composable
fun SettingsShortsTutorialSection(
    gesturesTutorialEnabled: Boolean,
    onGesturesTutorialChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = stringResource(R.string.section_assistance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingItemWithSwitch(
                    title = stringResource(R.string.shorts_assistance),
                    subtitle = stringResource(R.string.gestures_tutorial_desc),
                    checked = gesturesTutorialEnabled,
                    onCheckedChange = onGesturesTutorialChange,
                    position = SettingItemPosition.Single
                )
            }
        }
    }
}

@Composable
fun SettingsAccountSection(
    onImportCsvClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onClearDataClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = stringResource(R.string.section_account_security),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingItem(
                    title = stringResource(R.string.import_youtube_csv),
                    subtitle = stringResource(R.string.import_youtube_csv_desc),
                    onClick = onImportCsvClick,
                    position = SettingItemPosition.First
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.import_backup),
                    subtitle = stringResource(R.string.import_backup_desc),
                    onClick = onImportBackupClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.export_backup),
                    subtitle = stringResource(R.string.export_backup_desc),
                    onClick = onExportBackupClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.clear_user_data),
                    subtitle = stringResource(R.string.clear_user_data_desc),
                    onClick = onClearDataClick,
                    position = SettingItemPosition.Last
                )
            }
        }
    }
}

@Composable
fun SettingsInfoSection(
    onSupportClick: () -> Unit,
    onDonateClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            text = stringResource(R.string.section_info),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingItem(
                    title = stringResource(R.string.support),
                    subtitle = stringResource(R.string.support_desc),
                    onClick = onSupportClick,
                    position = SettingItemPosition.First
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.donate),
                    subtitle = stringResource(R.string.donate_desc),
                    onClick = onDonateClick,
                    position = SettingItemPosition.Middle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                SettingItem(
                    title = stringResource(R.string.about_us),
                    subtitle = stringResource(R.string.about_us_desc),
                    onClick = onAboutClick,
                    position = SettingItemPosition.Last
                )
            }
        }
    }
}
