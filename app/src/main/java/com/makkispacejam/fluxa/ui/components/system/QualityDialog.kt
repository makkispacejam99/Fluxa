package com.makkispacejam.fluxa.ui.components.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

@Composable
fun DialogOptions(
    currentResolution: Int,
    availableResolutions: List<Int>,
    onResolutionSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val q4k = stringResource(R.string.quality_4k)
    val q2k = stringResource(R.string.quality_2k)
    val q1080 = stringResource(R.string.quality_1080p)
    val q720 = stringResource(R.string.quality_720p)
    val q480 = stringResource(R.string.quality_480p_balanced)
    val q360 = stringResource(R.string.quality_360p_saving)
    val q240 = stringResource(R.string.quality_240p_saving)

    val baseResoluciones = if (availableResolutions.isNotEmpty()) {
        availableResolutions.map { res ->
            val label = when (res) {
                2160 -> q4k
                1440 -> q2k
                1080 -> q1080
                720 -> q720
                480 -> q480
                360 -> q360
                else -> "${res}p"
            }
            Pair(label, res)
        }
    } else {
        listOf(
            Pair(q1080, 1080), Pair(q720, 720), Pair(q480, 480), Pair(q360, 360), Pair(q240, 240)
        )
    }

    val resoluciones = baseResoluciones.distinctBy { it.second }.sortedByDescending { it.second }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.video_quality),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.85f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(resoluciones) { (nombreCalidad, valorPixeles) ->
                        val isSelected = valorPixeles == currentResolution
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onResolutionSelected(valorPixeles)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = nombreCalidad,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
