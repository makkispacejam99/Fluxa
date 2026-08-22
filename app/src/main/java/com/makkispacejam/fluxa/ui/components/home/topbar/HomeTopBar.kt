package com.makkispacejam.fluxa.ui.components.home.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.FluxaAvatar

/* Elementos ubicados en la parte
superior de la pantalla principal */

@Composable
fun HomeTopBar(
    isShowingResults: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onSearch: () -> Unit,
    onCloseResults: () -> Unit,
    onProfileClick: () -> Unit,
    userAvatarPath: String?,
    userName: String,
    isIncognito: Boolean = false
) {

    // Bloque de búsqueda expandido
    if (isShowingResults) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = 52.dp),
            placeholder = {
                Text(
                    searchQuery.ifEmpty { stringResource(R.string.search_placeholder) },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            },
            trailingIcon = {
                IconButton(onClick = onCloseResults) {
                    Icon(Icons.Rounded.Close, null)
                }
            },
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (searchQuery.isNotBlank()) onSearch()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fluxa_horizontal),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .height(28.dp)
                    .width(82.dp)
                    .padding(horizontal = 2.dp)
            )


            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                // Barra de Búsqueda
                IconButton(onClick = onSearchClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                }

                // Videos Recientes
                IconButton(onClick = onNotificationsClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                }

                // Avatar personalizado
                if (isIncognito) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    FluxaAvatar(
                        avatarUrl = userAvatarPath,
                        size = 38.dp,
                        placeholderName = userName,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick() }
                    )
                }
            }
        }
    }
}
