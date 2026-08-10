package com.makkispacejam.fluxa.ui.components.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.data.avatars.AvatarUtils
import com.makkispacejam.fluxa.data.local.SubscriptionEntity
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import kotlinx.coroutines.delay


// Apartado de suscripciones
@Composable
fun SubscriptionSection(
    subscriptions: List<SubscriptionEntity>,
    onShowAll: () -> Unit,
    onChannelClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.tus_canales),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.ver_todo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { onShowAll() }
            )
        }
        if (subscriptions.isEmpty()) {
            Text(
                stringResource(R.string.no_subscriptions_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            val displayedSubs = remember(subscriptions) { subscriptions.take(6) }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(displayedSubs, key = { it.channelId }) { sub ->
                    SubscriptionChannelItem(sub, onChannelClick)
                }
            }
        }
    }
}

// Elementos del apartado de suscripciones
@Composable
fun SubscriptionChannelItem(
    sub: SubscriptionEntity,
    onChannelClick: (String) -> Unit,
    interactionVM: InteractionViewModel = viewModel()
) {
    val cachedAvatar = remember(sub.channelId, sub.avatarUrl) { 
        interactionVM.getAvatar(sub.channelId, sub.avatarUrl) 
    }
    var avatarUrl by remember(sub.channelId, cachedAvatar) { mutableStateOf(cachedAvatar) }

    LaunchedEffect(sub.channelId, sub.avatarUrl) {
        if (avatarUrl.isNullOrBlank()) {
            delay((500..3000).random().toLong())
            val real = AvatarUtils.fetchChannelAvatar(sub.channelId)
            if (!real.isNullOrEmpty()) {
                avatarUrl = real
                interactionVM.avatarCache[sub.channelId] = real
                interactionVM.updateSubscriptionAvatar(sub.channelId, real)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(65.dp).clickable { onChannelClick(sub.channelName) }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(30.dp)
            )

            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sub.channelName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
