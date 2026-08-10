package com.makkispacejam.fluxa.ui.components.home.topbar.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R

// Píldoras para filtrar búsquedas
@Composable
fun SearchFilterRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    filters: List<String> = listOf("Todo", "Videos", "Canales", "Listas")
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
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.defaultMinSize(minWidth = 100.dp),
                shape = RoundedCornerShape(25.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(filterLabels[filter] ?: filter, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
