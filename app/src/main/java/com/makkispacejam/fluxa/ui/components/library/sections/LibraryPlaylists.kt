package com.makkispacejam.fluxa.ui.components.library.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.data.local.PlaylistEntity
import com.makkispacejam.fluxa.ui.components.core.TranslatedText

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

// Apartado de listas de reproducción
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistsSection(
    userPlaylists: List<PlaylistEntity>,
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onRenamePlaylist: (PlaylistEntity, String) -> Unit
) {
    val showCreateDialog = remember { mutableStateOf(false) }
    val playlistToManage = remember { mutableStateOf<PlaylistEntity?>(null) }
    val showOptionsSheet = remember { mutableStateOf(false) }
    val showRenameDialog = remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.tus_listas),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { 
                        if (userPlaylists.size < 10) showCreateDialog.value = true 
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.agregar),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (userPlaylists.size < 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (userPlaylists.size < 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (userPlaylists.size >= 10) {
            Text(
                stringResource(R.string.playlist_limit_reached),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Surface(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Playlists del sistema
                LocalPlaylistItem(
                    title = stringResource(R.string.watch_later),
                    subtitle = stringResource(R.string.videos_guardados),
                    isSystem = true,
                    onClick = { onPlaylistClick("Ver más tarde") }
                )
                LocalPlaylistItem(
                    title = stringResource(R.string.favoritos),
                    subtitle = stringResource(R.string.tus_videos_preferidos),
                    isSystem = true,
                    onClick = { onPlaylistClick("Favoritos") }
                )
                
                // Playlists del usuario
                userPlaylists.forEach { playlist ->
                    LocalPlaylistItem(
                        title = playlist.name,
                        subtitle = stringResource(R.string.lista_personalizada),
                        isSystem = false,
                        onClick = { onPlaylistClick(playlist.name) },
                        onOptionsClick = {
                            playlistToManage.value = playlist
                            showOptionsSheet.value = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo para crear playlist
    if (showCreateDialog.value) {
        val newName = remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog.value = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.nueva_lista_reproducion)) },
            text = {
                OutlinedTextField(
                    value = newName.value,
                    onValueChange = { newName.value = it },
                    label = { Text(stringResource(R.string.nombre_de_la_lista)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.value.isNotBlank()) {
                            onCreatePlaylist(newName.value)
                            showCreateDialog.value = false
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                ) { Text(stringResource(R.string.crear)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog.value = false }, shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.cancel_btn)) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Diálogo para renombrar
    if (showRenameDialog.value && playlistToManage.value != null) {
        val newName = remember { mutableStateOf(playlistToManage.value!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog.value = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.renombrar_lista)) },
            text = {
                OutlinedTextField(
                    value = newName.value,
                    onValueChange = { newName.value = it },
                    label = { Text(stringResource(R.string.nuevo_nombre)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.value.isNotBlank()) {
                            onRenamePlaylist(playlistToManage.value!!, newName.value)
                            showRenameDialog.value = false
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                ) { Text(stringResource(R.string.guardar)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog.value = false }, shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.cancel_btn)) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Menu de opciones para playlists
    if (showOptionsSheet.value && playlistToManage.value != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet.value = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                TranslatedText(
                    text = playlistToManage.value!!.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )
                
                OptionItem(Icons.Rounded.PlayArrow, stringResource(R.string.play)) {
                    showOptionsSheet.value = false
                    onPlaylistClick(playlistToManage.value!!.name)
                }
                OptionItem(Icons.Rounded.Edit, stringResource(R.string.editar_nombre)) {
                    showOptionsSheet.value = false
                    showRenameDialog.value = true
                }
                OptionItem(Icons.Rounded.Delete, stringResource(R.string.borrar_lista), isError = true) {
                    showOptionsSheet.value = false
                    onDeletePlaylist(playlistToManage.value!!)
                }
            }
        }
    }
}

// Elementos de la playlist local
@Composable
fun LocalPlaylistItem(
    title: String,
    subtitle: String,
    isSystem: Boolean,
    onClick: () -> Unit,
    onOptionsClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TranslatedText(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        
        if (!isSystem && onOptionsClick != null) {
            IconButton(onClick = onOptionsClick) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.options_title),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// Elementos del menu de opciones
@Composable
fun OptionItem(
    icon: ImageVector,
    label: String,
    isError: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
