package com.makkispacejam.fluxa.ui.components.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

// Encabezado del perfil de usuario
@Composable
fun UserProfileHeader(
    userName: String,
    userAvatarPath: String?,
    onEditClick: () -> Unit,
    avatarVersion: Long = 0L
) {
    // Mensajes graciosos
    val funnyMessages = listOf(
        stringResource(R.string.funny_msg_1),
        stringResource(R.string.funny_msg_2),
        stringResource(R.string.funny_msg_3),
        stringResource(R.string.funny_msg_4),
        stringResource(R.string.funny_msg_5),
        stringResource(R.string.funny_msg_6),
        stringResource(R.string.funny_msg_7),
        stringResource(R.string.funny_msg_8),
        stringResource(R.string.funny_msg_9),
        stringResource(R.string.funny_msg_10)
    )
    val funnyMessage = remember(userName) { funnyMessages.random() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (userAvatarPath != null && File(userAvatarPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userAvatarPath)
                        .setParameter("avatar_version", avatarVersion)
                        .build(),
                    contentDescription = "User Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userName.firstOrNull()?.toString() ?: "F",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp).padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.editar_nombre),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = funnyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
