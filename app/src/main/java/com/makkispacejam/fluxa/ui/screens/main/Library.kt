package com.makkispacejam.fluxa.ui.screens.main

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.library.dialogs.EditProfileDialog
import com.makkispacejam.fluxa.ui.components.library.dialogs.ImageCropperDialog
import com.makkispacejam.fluxa.ui.components.library.sections.HistorySection
import com.makkispacejam.fluxa.ui.components.library.sections.LocalPlaylistsSection
import com.makkispacejam.fluxa.ui.components.library.sections.SubscriptionSection
import com.makkispacejam.fluxa.ui.components.library.sections.UserProfileHeader
import com.makkispacejam.fluxa.ui.components.library.skeleton.LibrarySkeleton
import com.makkispacejam.fluxa.ui.components.system.ErrorScreen
import com.makkispacejam.fluxa.ui.components.system.ErrorType
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel

// Pantalla de colecciones
@SuppressLint("UseKtx")
@Composable
fun FluxaCollections(
    modifier: Modifier = Modifier,
    errorType: ErrorType?,
    errorMessage: String?,
    isLoading: Boolean,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onShowSubscriptions: () -> Unit,
    onShowHistory: () -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onRetry: () -> Unit,
    isIncognito: Boolean = false,
    interactionViewModel: InteractionViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE) }
    
    val userName = remember { mutableStateOf(prefs.getString("user_name", "Fluxa") ?: "Fluxa") }
    val userAvatarPath = remember { mutableStateOf(prefs.getString("user_avatar_path", null)) }
    val avatarVersion = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val showEditDialog = remember { mutableStateOf(false) }
    val croppingUri = remember { mutableStateOf<Uri?>(null) }

    val subscriptions by interactionViewModel.subscriptionSummary.collectAsState(initial = emptyList())
    val history by interactionViewModel.getHistory().collectAsState(initial = emptyList())
    val userPlaylists by interactionViewModel.getUserPlaylists().collectAsState(initial = emptyList())

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { croppingUri.value = it }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
    if (errorType != null) {
            ErrorScreen(errorType = errorType, errorMessage = errorMessage, onRetry = onRetry)
        } else if (isLoading) {
        LibrarySkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.nav_library),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                item {
                    UserProfileHeader(
                        userName = userName.value,
                        userAvatarPath = userAvatarPath.value,
                        onEditClick = { showEditDialog.value = true },
                        avatarVersion = avatarVersion.longValue
                    )
                }

                item {
                    SubscriptionSection(
                        subscriptions = subscriptions,
                        onShowAll = onShowSubscriptions,
                        onChannelClick = onChannelClick
                    )
                }

                item {
                    HistorySection(
                        history = history,
                        onShowAll = onShowHistory,
                        onVideoClick = onVideoClick
                    )
                }

                item {
                    LocalPlaylistsSection(
                        userPlaylists = userPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        onCreatePlaylist = { name -> interactionViewModel.createPlaylist(name) },
                        onDeletePlaylist = { playlist ->
                            interactionViewModel.deletePlaylist(
                                playlist
                            )
                        },
                        onRenamePlaylist = { playlist, newName ->
                            playlist.name = newName
                            interactionViewModel.updatePlaylist(playlist)
                        }
                    )
                }
            }
        }

        if (isIncognito) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = stringResource(R.string.incognito_collections_blocked),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }

    if (showEditDialog.value) {
        val originalName = remember { userName.value }
        EditProfileDialog(
            userName = userName.value,
            userAvatarPath = userAvatarPath.value,
            onDismiss = {
                userName.value = originalName
                showEditDialog.value = false
            },
            onSave = { newName, newAvatarPath ->
                if (newName.isNotBlank()) {
                    userName.value = newName
                    prefs.edit { putString("user_name", newName) }
                }
                userAvatarPath.value = newAvatarPath
                avatarVersion.longValue = System.currentTimeMillis()
                showEditDialog.value = false
            },
            imagePickerLauncher = imagePickerLauncher,
            avatarVersion = avatarVersion.longValue,
            onNameChange = { userName.value = it }
        )
    }

    croppingUri.value?.let { uri ->
        ImageCropperDialog(
            uri = uri,
            onDismiss = { croppingUri.value = null },
            onConfirm = { path ->
                userAvatarPath.value = path
                prefs.edit {
                    putString("user_avatar_path", path)
                    putString("user_name", userName.value)
                }
                avatarVersion.longValue = System.currentTimeMillis()
                croppingUri.value = null
                showEditDialog.value = false
            }
        )
    }
}
