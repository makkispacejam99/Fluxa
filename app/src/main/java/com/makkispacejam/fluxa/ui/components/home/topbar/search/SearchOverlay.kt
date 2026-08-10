package com.makkispacejam.fluxa.ui.components.home.topbar.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.animations.FluxaAnimations
import com.makkispacejam.fluxa.ui.components.home.topbar.recents.RecentSearchItem

@Composable
fun SearchOverlay(
    visible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    recentSearches: List<String>,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    focusRequester: FocusRequester
) {
    AnimatedVisibility(
        visible = visible,
        enter = FluxaAnimations.searchOverlayEnter(),
        exit = FluxaAnimations.searchOverlayExit()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(min = 52.dp)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, null)
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotEmpty()) {
                            onSearch()
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = recentSearches, key = { it }) { search ->
                        RecentSearchItem(
                            search = search,
                            onClick = {
                                onSearchQueryChange(search)
                                onSearch()
                            },
                            onRemove = { onRemoveRecentSearch(search) }
                        )
                    }

                    if (recentSearches.isNotEmpty()) {
                        item {
                            TextButton(
                                onClick = onClearRecentSearches,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Text(stringResource(R.string.limpiar_historial), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
