package com.makkispacejam.fluxa.ui.components.home.topbar.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R

// Píldoras para filtrar búsquedas
@Composable
fun SearchFilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    filters: List<String> = listOf("Todo", "Videos", "Canales", "Listas", "En Vivo")
) {
    val filterLabels = mapOf(
        "Todo" to stringResource(R.string.filter_all),
        "Videos" to stringResource(R.string.filter_videos),
        "Canales" to stringResource(R.string.filter_channels),
        "Listas" to stringResource(R.string.filter_playlists),
        "En Vivo" to stringResource(R.string.filter_live)
    )

    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = filters,
            key = { it },
            contentType = { "filter_chip" }
        ) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filterLabels[filter] ?: filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
