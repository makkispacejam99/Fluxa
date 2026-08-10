package com.makkispacejam.fluxa.ui.components.shorts.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.ui.components.dialogs.OptionDialog

// Diálogo de calidad de video
@Composable
fun ShortsQualityDialog(
    userPreferences: UserPreferences,
    onQualityChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val q1080 = stringResource(R.string.quality_1080p)
    val q720 = stringResource(R.string.quality_720p)
    val q480 = stringResource(R.string.quality_480p)
    val q360 = stringResource(R.string.quality_360p)
    val qSaving = stringResource(R.string.quality_data_saving)

    OptionDialog(
        title = stringResource(R.string.video_quality),
        options = listOf(q1080, q720, q480, q360, qSaving),
        selectedOption = userPreferences.shortsVideoQuality,
        onOptionSelected = {
            userPreferences.shortsVideoQuality = it
            onQualityChanged(it)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
