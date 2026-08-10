package com.makkispacejam.fluxa.ui.components.system

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Pantalla de error
@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    errorType: ErrorType,
    errorMessage: String?,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val errorIcon = when (errorType) {
            ErrorType.NO_INTERNET -> Icons.Default.WifiOff
            ErrorType.SERVER_ERROR -> Icons.Default.CloudOff
        }

        Icon(
            imageVector = errorIcon,
            contentDescription = "Error Status",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage ?: "Ocurrió un problema inesperado",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(28.dp))
            FilledTonalButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.retry_btn), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}