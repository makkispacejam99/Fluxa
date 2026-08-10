package com.makkispacejam.fluxa.ui.components.library.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.local.SubscriptionEntity

// Diálogo para anular suscripción
@Composable
fun UnsubscribeDialog(
    channel: SubscriptionEntity?,
    onDismiss: () -> Unit,
    onConfirm: (SubscriptionEntity) -> Unit
) {
    if (channel == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.unsubscribe_title)) },
        text = { Text(stringResource(R.string.unsubscribe_confirm, channel.channelName)) },
        confirmButton = {
            Button(
                onClick = { onConfirm(channel) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(24.dp)
            ) { Text(stringResource(R.string.btn_unsubscribe)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.cancel_btn)) }
        }
    )
}
