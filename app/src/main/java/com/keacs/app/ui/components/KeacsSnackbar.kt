package com.keacs.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    durationMillis: Long = 3000L,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        delay(durationMillis)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = if (isError) KeacsColors.Error else KeacsColors.Surface,
            contentColor = if (isError) KeacsColors.Surface else KeacsColors.TextPrimary,
            action = {
                TextButton(onClick = onDismiss) {
                    Text("关闭", color = if (isError) KeacsColors.Surface else KeacsColors.Primary)
                }
            }
        ) {
            Text(message)
        }
    }
}
