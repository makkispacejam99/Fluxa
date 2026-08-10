package com.makkispacejam.fluxa.ui.components.library.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.makkispacejam.fluxa.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

// Diálogo para recortar foto de perfil
@Composable
fun ImageCropperDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream)
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.adjust_image)) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = BitmapPainter(bitmap.asImageBitmap()),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = max(1f, scale * zoom)
                                    offset += pan
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x4D000000), CircleShape)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        val result = createBitmap(500, 500)
                        val canvas = Canvas(result)

                        val scaleInitial = min(500f / bitmap.width, 500f / bitmap.height)
                        val xOff = (500f - bitmap.width * scaleInitial) / 2
                        val yOff = (500f - bitmap.height * scaleInitial) / 2

                        val matrix = Matrix()
                        matrix.postScale(scaleInitial, scaleInitial)
                        matrix.postTranslate(xOff, yOff)
                        matrix.postScale(scale, scale, 250f, 250f)
                        matrix.postTranslate(offset.x, offset.y)

                        canvas.drawBitmap(bitmap, matrix, null)

                        val file = File(context.filesDir, "user_avatar_custom.jpg")
                        FileOutputStream(file).use { out ->
                            result.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        onConfirm(file.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onDismiss()
                    }
                }, shape = RoundedCornerShape(24.dp)) {
                    Text(stringResource(R.string.accept_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(24.dp)) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    } else {
        onDismiss()
    }
}
