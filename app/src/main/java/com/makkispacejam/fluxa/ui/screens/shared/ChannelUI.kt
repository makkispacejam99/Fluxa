package com.makkispacejam.fluxa.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.FluxaAvatar
import com.makkispacejam.fluxa.ui.components.core.SkeletonLine
import com.makkispacejam.fluxa.ui.components.core.shimmerEffect

@Composable
fun ChannelBanner(
    bannerUrl: String?,
    isLoading: Boolean
) {
    val bannerModifier = Modifier.fillMaxWidth().padding(16.dp).height(120.dp).clip(RoundedCornerShape(12.dp))
    Box(
        modifier = if (isLoading) bannerModifier.shimmerEffect() else bannerModifier.background(Color(0xFF262626)),
        contentAlignment = Alignment.Center
    ) {
        if (!bannerUrl.isNullOrEmpty()) {
            AsyncImage(model = bannerUrl, contentDescription = "Channel Banner", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun ChannelInfo(
    channelName: String,
    subscriberCount: String,
    avatarUrl: String?,
    isSubscribed: Boolean,
    isLoading: Boolean,
    onSubscribeClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(40.dp)).shimmerEffect())
            } else {
                FluxaAvatar(avatarUrl = avatarUrl, size = 80.dp, iconSize = 44.dp, placeholderName = channelName)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = channelName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (isLoading) { Spacer(modifier = Modifier.height(6.dp)); SkeletonLine(modifier = Modifier.width(100.dp), height = 12.dp) }
                else Text(text = subscriberCount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSubscribeClick, enabled = !isLoading, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ), shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe))
        }
    }
}

@Composable
fun ChannelFilterChips(
    selectedFilter: String,
    filters: List<String>,
    filterLabels: Map<String, String>,
    onFilterSelected: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter, onClick = { onFilterSelected(filter) },
                label = { Text(filterLabels[filter] ?: filter) }, shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}

@Composable
fun ChannelEmptyContent(filterLabels: Map<String, String>, selectedFilter: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.Center) {
        val lowerFilter = filterLabels[selectedFilter]?.lowercase() ?: selectedFilter.lowercase()
        Text(text = stringResource(R.string.no_content_uploaded, lowerFilter), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
