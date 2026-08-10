@file:Suppress("unused")
package com.makkispacejam.fluxa.ui.components.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Tarjeta de canales
@Composable
fun ChannelCard(
    name: String,
    subs: String,
    videoCount: String,
    avatarUrl: String = "",
    onChannelClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChannelClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FluxaAvatar(
            avatarUrl = avatarUrl.ifEmpty { null },
            size = 80.dp,
            iconSize = 40.dp,
            placeholderName = name
        )

        // Información y botones
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            TranslatedText(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChannelClick,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Ver canal",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
