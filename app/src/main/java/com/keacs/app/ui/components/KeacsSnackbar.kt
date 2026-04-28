package com.keacs.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import kotlinx.coroutines.delay

@Composable
fun KeacsSnackbar(
    message: String,
    isError: Boolean = false,
    durationMillis: Long = 2000L,
    atTop: Boolean = false,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        delay(durationMillis)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (atTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding())
            .padding(16.dp),
        contentAlignment = if (atTop) Alignment.TopCenter else Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (isError) KeacsColors.Error else KeacsColors.Surface,
            contentColor = if (isError) KeacsColors.Surface else KeacsColors.TextPrimary,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp,
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
