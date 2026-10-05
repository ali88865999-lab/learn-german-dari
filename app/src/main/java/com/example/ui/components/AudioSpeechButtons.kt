package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioSpeechButtons(
    textToSpeak: String,
    onPlayAudio: (String, Boolean) -> Unit, // text, isSlow
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Normal Speed Speaker (🔊)
        IconButton(
            onClick = { onPlayAudio(textToSpeak, false) },
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("audio_btn_normal")
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "شنیدن با سرعت عادی",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size((size * 0.55).dp)
            )
        }

        // Slow Speed Button (🐢 ۰.۷x)
        IconButton(
            onClick = { onPlayAudio(textToSpeak, true) },
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .testTag("audio_btn_slow")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "۰.۶x",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
