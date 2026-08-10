package com.makkispacejam.fluxa.ui.components.settings.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R

// Diálogo de importar
@Composable
fun ImportBackupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.import_backup)) },
        text = { Text(stringResource(R.string.import_dialog_text)) },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.continue_btn)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.cancel_btn)) }
        }
    )
}
