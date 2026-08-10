@file:Suppress("UNUSED_VALUE")

package com.makkispacejam.fluxa.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

@Composable
fun FluxaAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 30.dp,
    placeholderName: String? = null,
    backgroundColor: Color = Color.DarkGray,
    iconColor: Color = Color.LightGray,
    isLoading: Boolean = false
) {

    val processedUrl = remember(avatarUrl) {
        when {
            avatarUrl.isNullOrEmpty() -> null
            avatarUrl.startsWith("//") -> "https:$avatarUrl"
            else -> avatarUrl
        }
    }

    val nameColor = if (!placeholderName.isNullOrEmpty()) MaterialTheme.colorScheme.primary else backgroundColor
    val contentColor = if (!placeholderName.isNullOrEmpty()) MaterialTheme.colorScheme.onPrimary else Color.White

    var showShimmer by remember(processedUrl) { mutableStateOf(isLoading || processedUrl != null) }
    var showErrorFallback by remember(processedUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (showShimmer) {
                    Modifier.shimmerEffect()
                } else if (showErrorFallback || processedUrl == null) {
                    Modifier.background(nameColor)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showErrorFallback || processedUrl == null) {
            val cleanName = placeholderName?.replace("@", "")?.trim()
            if (!cleanName.isNullOrEmpty()) {
                Text(
                    text = cleanName.firstOrNull()?.uppercase() ?: "",
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45).sp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (processedUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(processedUrl)
                    .crossfade(true)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    showErrorFallback = when (state) {
                        is AsyncImagePainter.State.Success -> {
                            false
                        }

                        is AsyncImagePainter.State.Error -> {
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }
            )
        }
    }
}
