package com.makkispacejam.fluxa.ui.components.settings.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.theme.ScallopedShape

// Diálogo de donaciones
@Composable
fun DonationDialog(onDismiss: () -> Unit, context: Context) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_btn)) }
        },
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = ScallopedShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.heart_active),
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.support_project),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.support_project_desc),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://ko-fi.com/makkispacejam".toUri())
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(painterResource(R.drawable.kofi), null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.buy_coffee), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}
