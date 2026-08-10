package com.makkispacejam.fluxa.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.avatars.AvatarUtils
import com.makkispacejam.fluxa.data.local.SubscriptionEntity
import com.makkispacejam.fluxa.ui.components.library.dialogs.UnsubscribeDialog
import com.makkispacejam.fluxa.ui.components.library.dialogs.UnblockChannelDialog
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel

// Pantalla de suscripciones
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    interactionViewModel: InteractionViewModel,
    onChannelClick: (String) -> Unit
) {
    val subscriptions by interactionViewModel.getAllSubscriptions().collectAsState(initial = emptyList())
    val blockedSubs by interactionViewModel.getBlockedSubscriptions().collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var isAscending by remember { mutableStateOf(true) }
    var isShowingBlocked by remember { mutableStateOf(false) }
    
    val channelToDelete = remember { mutableStateOf<SubscriptionEntity?>(null) }
    val channelToUnblock = remember { mutableStateOf<SubscriptionEntity?>(null) }

    val currentList = if (isShowingBlocked) blockedSubs else subscriptions

    val filteredList = remember(currentList, searchQuery, isAscending) {
        currentList
            .filter { it.channelName.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.channelName.lowercase() }
            .let { if (isAscending) it else it.reversed() }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clipToBounds()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isShowingBlocked) stringResource(R.string.blocked_channels) else stringResource(R.string.your_subscriptions),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            
            Row {
                IconButton(onClick = { isShowingBlocked = !isShowingBlocked }) {
                    Icon(
                        Icons.Rounded.Block,
                        contentDescription = stringResource(R.string.blocked_channels),
                        tint = if (isShowingBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { isAscending = !isAscending }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = null,
                        tint = if (!isAscending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(52.dp),
            placeholder = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Search,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isShowingBlocked) stringResource(R.string.search_blocked) else stringResource(R.string.search_subscriptions),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            shape = RoundedCornerShape(26.dp),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, null)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        // Lista de canales normales y bloqueados
        if (filteredList.isEmpty() && searchQuery.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (isShowingBlocked) stringResource(R.string.no_blocked_channels) else stringResource(R.string.no_subscriptions_yet),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.channelId }) { item ->
                    SubscriptionItem(
                        subscription = item,
                        isBlockedMode = isShowingBlocked,
                        interactionVM = interactionViewModel,
                        onClick = { if (!isShowingBlocked) onChannelClick(item.channelName) },
                        onActionButtonClick = {
                            if (isShowingBlocked) channelToUnblock.value = item
                            else channelToDelete.value = item
                        }
                    )
                }
            }
        }
    }

    UnsubscribeDialog(
        channel = channelToDelete.value,
        onDismiss = { channelToDelete.value = null },
        onConfirm = {
            interactionViewModel.toggleSubscription(it.channelId, it.channelName, it.avatarUrl, true)
            channelToDelete.value = null
        }
    )

    UnblockChannelDialog(
        channel = channelToUnblock.value,
        onDismiss = { channelToUnblock.value = null },
        onConfirm = {
            interactionViewModel.toggleSubscription(it.channelId, it.channelName, it.avatarUrl, true)
            channelToUnblock.value = null
        }
    )
}

@Composable
fun SubscriptionItem(
    subscription: SubscriptionEntity,
    isBlockedMode: Boolean = false,
    onClick: () -> Unit,
    onActionButtonClick: () -> Unit,
    interactionVM: InteractionViewModel
) {
    val cachedAvatar = remember(subscription.channelId, subscription.avatarUrl) { 
        interactionVM.getAvatar(subscription.channelId, subscription.avatarUrl) 
    }
    var avatarUrl by remember(subscription.channelId, cachedAvatar) { mutableStateOf(cachedAvatar) }

    LaunchedEffect(subscription.channelId, subscription.avatarUrl) {
        if (avatarUrl.isNullOrBlank()) {
            kotlinx.coroutines.delay((500..4000).random().toLong()) // Delay generoso
            val real = AvatarUtils.fetchChannelAvatar(subscription.channelId)
            if (!real.isNullOrEmpty()) {
                avatarUrl = real
                interactionVM.avatarCache[subscription.channelId] = real
                interactionVM.updateSubscriptionAvatar(subscription.channelId, real)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subscription.channelName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subscription.subscriberCount.isNullOrBlank()) {
                Text(
                    text = subscription.subscriberCount,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Button(
            onClick = onActionButtonClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBlockedMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isBlockedMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                if (isBlockedMode) stringResource(R.string.status_blocked) else stringResource(R.string.status_subscribed),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
