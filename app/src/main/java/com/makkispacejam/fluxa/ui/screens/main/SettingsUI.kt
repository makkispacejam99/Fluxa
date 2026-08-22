package com.makkispacejam.fluxa.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ThemeMode
import com.makkispacejam.fluxa.ui.components.settings.components.SettingItem
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
    onRegionClick: () -> Unit
) {
    Text(stringResource(R.string.section_general), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp)) {
        Column {
            SettingItem(stringResource(R.string.appearance), stringResource(themeMode.titleRes), onClick = onThemeClick)
            SettingItemWithSwitch(title = stringResource(R.string.amoled_mode), subtitle = stringResource(R.string.amoled_mode_desc), checked = amoledMode, onCheckedChange = onAmoledModeChange)
            SettingItemWithSwitch(title = stringResource(R.string.incognito_mode), subtitle = stringResource(R.string.incognito_mode_desc), checked = incognitoMode, onCheckedChange = onIncognitoModeChange)
            SettingItem(stringResource(R.string.language), selectedLanguage, onClick = onLanguageClick)
            SettingItem(stringResource(R.string.content_translation), selectedTranslationLang, onClick = onTranslationClick)
            SettingItem(stringResource(R.string.video_quality), selectedQuality, onClick = onQualityClick)
            SettingItem(stringResource(R.string.region), selectedRegion, onClick = onRegionClick)
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
    Spacer(modifier = Modifier.height(12.dp))
    Text(stringResource(R.string.section_account_security), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp)) {
        Column {
            SettingItem(stringResource(R.string.import_youtube_csv), stringResource(R.string.import_youtube_csv_desc), onClick = onImportCsvClick)
            SettingItem(stringResource(R.string.import_backup), stringResource(R.string.import_backup_desc), onClick = onImportBackupClick)
            SettingItem(stringResource(R.string.export_backup), stringResource(R.string.export_backup_desc), onClick = onExportBackupClick)
            SettingItem(stringResource(R.string.clear_user_data), stringResource(R.string.clear_user_data_desc), onClick = onClearDataClick)
        }
    }
}

@Composable
fun SettingsInfoSection(
    onSupportClick: () -> Unit,
    onDonateClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(stringResource(R.string.section_info), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            SettingItem(stringResource(R.string.support), stringResource(R.string.support_desc), onClick = onSupportClick)
            SettingItem(stringResource(R.string.donate), stringResource(R.string.donate_desc), onClick = onDonateClick)
            SettingItem(stringResource(R.string.about_us), stringResource(R.string.about_us_desc), onClick = onAboutClick)
        }
    }
}
