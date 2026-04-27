package com.keacs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun NumberPad(
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = false,
    onKeyClick: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    val rows = listOf(
        listOf("1", "2", "3", "⌫"),
        listOf("4", "5", "6", "+"),
        listOf("7", "8", "9", "-"),
        listOf(".", "0", "保存"),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { label ->
                    NumberKey(
                        label = label,
                        modifier = Modifier.weight(if (label == "保存") 2f else 1f),
                        enabled = label != "保存" || saveEnabled,
                        onClick = {
                            if (label == "保存") onSaveClick() else onKeyClick(label)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val isSaveKey = label == "保存"
    val background = when {
        isSaveKey && enabled -> KeacsColors.Primary
        enabled -> KeacsColors.Surface
        else -> KeacsColors.SurfaceSubtle
    }
    val textColor = when {
        isSaveKey && enabled -> KeacsColors.Surface
        enabled -> KeacsColors.TextPrimary
        else -> KeacsColors.TextTertiary
    }
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
