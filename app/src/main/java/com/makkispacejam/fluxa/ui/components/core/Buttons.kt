package com.makkispacejam.fluxa.ui.components.core

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Botones de acción
@Composable
fun ActionButton(
    text: String? = null,
    icon: ImageVector,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onDisabledClick: () -> Unit = {}
) {
    Surface(
        onClick = { if (enabled) onClick() else onDisabledClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (enabled) {
            if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) {
                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )

           if (text != null) {
               Spacer(modifier = Modifier.width(8.dp))
               Text(
                   text = text,
                   style = MaterialTheme.typography.labelLarge,
                   color = if (enabled) {
                       if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                   } else {
                       MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                   }
               )
           }
        }
    }
}