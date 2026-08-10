package com.makkispacejam.fluxa.ui.components.library.skeleton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.components.core.SkeletonAvatar
import com.makkispacejam.fluxa.ui.components.core.SkeletonLine

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

// Esqueleto del perfil de usuario
@Composable
fun LibrarySkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonAvatar(size = 60.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    SkeletonLine(modifier = Modifier.width(100.dp), height = 20.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonLine(modifier = Modifier.width(180.dp), height = 12.dp)
                }
            }
        }

        // Esqueleto de canales
        item {
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
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(5) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(65.dp)
                    ) {
                        SkeletonAvatar(size = 60.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(modifier = Modifier.width(50.dp), height = 12.dp)
                    }
                }
            }
        }

        // Esqueleto de historial
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.historial),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.ver_todo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(3) {
                    Column(modifier = Modifier.width(160.dp)) {
                        SkeletonLine(
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            cornerRadius = 8.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonLine(modifier = Modifier.fillMaxWidth(0.9f), height = 12.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), height = 12.dp)
                    }
                }
            }
        }

        // Esqueleto de "Tus Listas"
        item {
            Text(
                stringResource(R.string.tus_listas),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonLine(modifier = Modifier.width(140.dp), height = 16.dp)
                    }
                }
            }
        }
    }
}
