package com.makkispacejam.fluxa.ui.screens.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ThemeMode
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.local.BackupRestoreManager
import com.makkispacejam.fluxa.ui.components.dialogs.OptionDialog
import com.makkispacejam.fluxa.ui.components.settings.dialogs.*
import com.makkispacejam.fluxa.ui.components.system.NotificationBanner
import com.makkispacejam.fluxa.viewmodels.user.TranslationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UseKtx", "NonObservableLocale", "LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    mainPadding: PaddingValues,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    amoledMode: Boolean,
    onAmoledModeChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    showMiniPlayer: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backupRestoreManager = remember { BackupRestoreManager(context) }
    val prefs = remember { UserPreferences(context) }
    val translationViewModel: TranslationViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )

    val regionMap = remember {
        mapOf("HN" to R.string.region_hn, "MX" to R.string.region_mx, "AR" to R.string.region_ar,
            "CO" to R.string.region_co, "ES" to R.string.region_es, "US" to R.string.region_us,
            "BR" to R.string.region_br, "CL" to R.string.region_cl, "PE" to R.string.region_pe,
            "GB" to R.string.region_gb, "JP" to R.string.region_jp)
    }
    val languageMap = remember {
        mapOf("en" to R.string.lang_english, "es" to R.string.lang_spanish,
            "pt" to R.string.lang_portuguese, "fr" to R.string.lang_french, "ja" to R.string.lang_japanese)
    }
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" }
        catch (_: Exception) { "1.0.0" }
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTranslationDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showExportInstructionsDialog by remember { mutableStateOf(false) }
    var showImportInstructionsDialog by remember { mutableStateOf(false) }
    var showImportYoutubeCsvDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    var showBanner by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf("") }

    var selectedQuality by remember { mutableStateOf(prefs.videoQuality) }
    var contentLanguage by remember { mutableStateOf(prefs.contentLanguage) }
    var contentRegion by remember { mutableStateOf(prefs.contentRegion) }

    val selectedLanguage = stringResource(languageMap[contentLanguage] ?: R.string.lang_english)
    val translationLangCode = translationViewModel.getTranslationLanguage()
    val selectedTranslationLang = if (translationLangCode != null) stringResource(languageMap[translationLangCode] ?: R.string.none) else stringResource(R.string.none)
    val selectedRegion = stringResource(regionMap[contentRegion] ?: R.string.region_hn)

    val backupExportedMsg = stringResource(R.string.backup_exported)
    val backupExportErrorMsg = stringResource(R.string.backup_export_error)
    val exportCancelledMsg = stringResource(R.string.export_cancelled)
    val importSuccessMsg = stringResource(R.string.import_success)
    val importErrorMsg = stringResource(R.string.import_error)
    val importCancelledMsg = stringResource(R.string.import_cancelled)
    val importSubsSuccessMsg = stringResource(R.string.import_subs_success)
    val invalidFileFormatMsg = stringResource(R.string.invalid_file_format)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        scope.launch {
            if (uri != null) {
                try { backupRestoreManager.exportData(uri); snackbarHostState.showSnackbar(backupExportedMsg, duration = SnackbarDuration.Short) }
                catch (e: Exception) { snackbarHostState.showSnackbar(backupExportErrorMsg.format(e.localizedMessage), duration = SnackbarDuration.Long) }
            } else snackbarHostState.showSnackbar(exportCancelledMsg, duration = SnackbarDuration.Short)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        scope.launch {
            if (uri != null) {
                val loadingJob = launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_importing_data), duration = SnackbarDuration.Indefinite) }
                try {
                    backupRestoreManager.importData(uri); loadingJob.cancel()
                    snackbarHostState.showSnackbar(importSuccessMsg, duration = SnackbarDuration.Long)
                    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(this)
                    }
                } catch (e: Exception) {
                    loadingJob.cancel()
                    val msg = if (e.message == "FILE_FORMAT_ERROR") invalidFileFormatMsg else importErrorMsg.format(e.localizedMessage)
                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
                }
            } else snackbarHostState.showSnackbar(importCancelledMsg, duration = SnackbarDuration.Short)
        }
    }
    val importYoutubeCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        scope.launch {
            if (uri != null) {
                val loadingJob = launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_importing_subs), duration = SnackbarDuration.Indefinite) }
                try {
                    backupRestoreManager.importFromCSV(uri) { _, _ -> }; loadingJob.cancel()
                    snackbarHostState.showSnackbar(importSubsSuccessMsg, duration = SnackbarDuration.Long)
                } catch (e: Exception) {
                    loadingJob.cancel()
                    val msg = if (e.message == "FILE_FORMAT_ERROR") invalidFileFormatMsg else importErrorMsg.format(e.localizedMessage)
                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long)
                }
            } else snackbarHostState.showSnackbar(importCancelledMsg, duration = SnackbarDuration.Short)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(mainPadding)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 20.dp, top = 10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SettingsGeneralSection(
                    themeMode = themeMode, amoledMode = amoledMode, onAmoledModeChange = onAmoledModeChange,
                    incognitoMode = incognitoMode, onIncognitoModeChange = {
                        onIncognitoModeChange(it)
                        bannerText = context.getString(if (it) R.string.incognito_toast_on else R.string.incognito_toast_off)
                        showBanner = true
                    },
                    selectedLanguage = selectedLanguage, selectedTranslationLang = selectedTranslationLang,
                    selectedQuality = selectedQuality, selectedRegion = selectedRegion,
                    onThemeClick = { showThemeDialog = true }, onLanguageClick = { showLanguageDialog = true },
                    onTranslationClick = { showTranslationDialog = true }, onQualityClick = { showQualityDialog = true },
                    onRegionClick = { showRegionDialog = true }
                )
            }
            item {
                SettingsAccountSection(
                    onImportCsvClick = { showImportYoutubeCsvDialog = true }, onImportBackupClick = { showImportInstructionsDialog = true },
                    onExportBackupClick = { showExportInstructionsDialog = true }, onClearDataClick = { showClearDataDialog = true }
                )
            }
            item {
                SettingsInfoSection(
                    onSupportClick = { showSupportDialog = true }, onDonateClick = { showDonationDialog = true },
                    onAboutClick = { showAboutDialog = true }
                )
            }
        }

        if (showThemeDialog) {
            val options = ThemeMode.entries.associateBy { stringResource(it.titleRes) }
            OptionDialog(title = stringResource(R.string.select_appearance), options = options.keys.toList(), selectedOption = stringResource(themeMode.titleRes),
                onOptionSelected = { selectedTitle -> options[selectedTitle]?.let { onThemeChange(it) }; showThemeDialog = false },
                onDismiss = { showThemeDialog = false })
        }
        if (showTranslationDialog) TranslationDialog(onDismiss = { showTranslationDialog = false }, translationViewModel = translationViewModel, languageMap = languageMap)
        if (showQualityDialog) {
            OptionDialog(title = stringResource(R.string.video_quality),
                options = listOf(stringResource(R.string.quality_1080p), stringResource(R.string.quality_720p), stringResource(R.string.quality_480p), stringResource(R.string.quality_360p), stringResource(R.string.quality_data_saving)),
                selectedOption = selectedQuality, onOptionSelected = { selectedQuality = it; prefs.videoQuality = it; showQualityDialog = false },
                onDismiss = { showQualityDialog = false })
        }
        if (showLanguageDialog) {
            val optionsMap = languageMap.entries.associate { stringResource(it.value) to it.key }
            OptionDialog(title = stringResource(R.string.select_language), options = optionsMap.keys.toList(), selectedOption = selectedLanguage,
                onOptionSelected = { selected -> val langCode = optionsMap[selected] ?: "en"; prefs.contentLanguage = langCode; AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode)); (context as? Activity)?.recreate(); showLanguageDialog = false },
                onDismiss = { showLanguageDialog = false })
        }
        if (showRegionDialog) {
            val optionsMap = regionMap.entries.associate { stringResource(it.value) to it.key }
            OptionDialog(title = stringResource(R.string.select_region), options = optionsMap.keys.toList(), selectedOption = selectedRegion,
                onOptionSelected = { selected -> contentRegion = optionsMap[selected] ?: "HN"; prefs.contentRegion = contentRegion; showRegionDialog = false },
                onDismiss = { showRegionDialog = false })
        }

        if (showExportInstructionsDialog) {
            val dateSuffix = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Date())
            ExportBackupDialog(onDismiss = { showExportInstructionsDialog = false }, onConfirm = { showExportInstructionsDialog = false; exportLauncher.launch("fluxa_backup_$dateSuffix.json") })
        }
        if (showImportInstructionsDialog) ImportBackupDialog(onDismiss = { showImportInstructionsDialog = false }, onConfirm = { showImportInstructionsDialog = false; importLauncher.launch(arrayOf("application/json")) })
        if (showImportYoutubeCsvDialog) ImportYoutubeCsvDialog(onDismiss = { showImportYoutubeCsvDialog = false }, onConfirm = { showImportYoutubeCsvDialog = false; importYoutubeCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) })
        if (showClearDataDialog) {
            ClearDataDialog(onDismiss = { showClearDataDialog = false }, onConfirm = {
                scope.launch { backupRestoreManager.clearAllData(); context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(this) } }
                showClearDataDialog = false
            })
        }
        if (showDonationDialog) DonationDialog(onDismiss = { showDonationDialog = false }, context = context)
        if (showSupportDialog) SupportDialog(onDismiss = { showSupportDialog = false }, context = context)
        if (showAboutDialog) AboutDialog(onDismiss = { showAboutDialog = false }, context = context)
        if (showUpdateDialog) {
            UpdateDialog(onDismiss = { showUpdateDialog = false }, onConfirm = {
                showUpdateDialog = false
                try { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/makkispacejam99/Fluxa/releases/latest"))) } catch (_: Exception) {}
            })
        }

        Button(onClick = { showUpdateDialog = true }, modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp), shape = RoundedCornerShape(35.dp)) {
            Text(stringResource(R.string.check_updates), fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.app_version_info, versionName), modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        if (showMiniPlayer) {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

        NotificationBanner(
            visible = showBanner,
            text = bannerText,
            onDismiss = { showBanner = false },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
