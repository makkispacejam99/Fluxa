package com.makkispacejam.fluxa.ui.components.layout

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.models.Screen

// Barra de navegación inferior
@Composable
fun BottomBar(
    selectedTab: Screen,
    onTabClick: (Screen) -> Unit
) {
    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        Screen.entries.forEach { screen ->
            val icon = when (screen) {
                Screen.Home -> Icons.Rounded.Home
                Screen.Library -> Icons.Rounded.VideoLibrary
                Screen.Settings -> Icons.Rounded.Settings
                Screen.Shorts -> null
            }

            NavigationBarItem(
                selected = selectedTab == screen,
                onClick = { onTabClick(screen) },
                label = {
                    Text(
                        stringResource(screen.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = {
                    if (screen == Screen.Shorts) {
                        Icon(
                            painter = painterResource(R.drawable.videoreel),
                            contentDescription = stringResource(screen.titleRes),
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            imageVector = icon!!,
                            contentDescription = stringResource(screen.titleRes),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}