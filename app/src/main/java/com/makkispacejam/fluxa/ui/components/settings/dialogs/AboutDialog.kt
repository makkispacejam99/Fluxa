package com.makkispacejam.fluxa.ui.components.settings.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.theme.ScallopedShape

// Diálogo de acerca de
@SuppressLint("UseKtx")
@Composable
fun AboutDialog(onDismiss: () -> Unit, context: Context) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accept_btn)) }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = ScallopedShape,
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = "https://avatars.githubusercontent.com/u/135391683?v=4",
                            contentDescription = "Developer Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onState = { imageState = it }
                        )
                        
                        if (imageState is AsyncImagePainter.State.Error) {
                            Icon(
                                painter = painterResource(R.drawable.github),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("makkispacejam", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.lead_developer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/makkispacejam99".toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.github), null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.about_dev_text),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}
