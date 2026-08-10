package com.makkispacejam.fluxa.ui.components.library.dialogs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.makkispacejam.fluxa.R
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

// Diálogo de editar perfil
@Composable
fun EditProfileDialog(
    userName: String,
    userAvatarPath: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    avatarVersion: Long = 0L,
    onNameChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE) }
    var tempName by remember { mutableStateOf(userName) }
    var currentAvatarPath by remember { mutableStateOf(userAvatarPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Editar perfil") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentAvatarPath != null && File(currentAvatarPath!!).exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentAvatarPath)
                                .setParameter("avatar_version", avatarVersion)
                                .build(),
                            contentDescription = "Edit Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (currentAvatarPath == null) {
                        TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Text("Cargar foto")
                        }
                    } else {
                        TextButton(onClick = {
                            currentAvatarPath = null
                        }) {
                            Text("Borrar foto", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                OutlinedTextField(
                    value = tempName,
                    onValueChange = {
                        tempName = it
                        onNameChange(it)
                    },
                    label = { Text("Nombre") },
                    placeholder = { Text("Fluxa") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (currentAvatarPath == null) {
                    prefs.edit { remove("user_avatar_path") }
                    File(context.filesDir, "user_avatar_custom.jpg").delete()
                }
                onSave(tempName, currentAvatarPath)
            }, shape = RoundedCornerShape(24.dp)) {
                Text(stringResource(R.string.save_btn))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(24.dp)) {
                Text(stringResource(R.string.cancel_btn))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
