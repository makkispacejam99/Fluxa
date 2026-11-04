@file:Suppress("FunctionName")

package com.makkispacejam.fluxa.video.video.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.utils.SubtitleFont

@Composable
fun SubtitleDisplay(subtitleText: String, controlsVisible: Boolean) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val fontFamily = remember(prefs.subtitleFont) { SubtitleFont.fromKey(prefs.subtitleFont).fontFamily() }
    val textSize = prefs.subtitleTextSize
    AnimatedVisibility(
        visible = subtitleText.isNotBlank(),
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = if (controlsVisible) 72.dp else 24.dp)
            ) {
                Text(
                    text = subtitleText,
                    color = Color.White,
                    fontFamily = fontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = textSize.sp,
                    lineHeight = (textSize * 1.35f).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
