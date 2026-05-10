package com.keacs.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSize

@Composable
fun NumberPad(
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = false,
    onKeyClick: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onDateClick: (() -> Unit)? = null,
    dateText: String? = null,
) {
    val rows = listOf(
        listOf("1", "2", "3", "DATE"),
        listOf("4", "5", "6", "+"),
        listOf("7", "8", "9", "-"),
        listOf(".", "0", "⌫", "保存"),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { label ->
                    if (label == "DATE" && onDateClick != null && dateText != null) {
                        DateKey(
                            text = dateText,
                            modifier = Modifier.weight(1f),
                            onClick = onDateClick,
                        )
                    } else if (label == "DATE") {
                        // Empty spacer if date picker is not provided
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        NumberKey(
                            label = label,
                            modifier = Modifier.weight(1f),
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
}

@Composable
private fun DateKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "keyScale"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .height(KeacsSize.MinTouch)
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (text == "今天" || text == "昨天" || text == "每周" || text == "每月" || text == "每年") {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = KeacsColors.TextPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (text == "周期") {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = KeacsColors.TextPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun NumberKey(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val isSaveKey = label == "保存"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "keyScale"
    )

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
            .scale(scale)
            .height(KeacsSize.MinTouch)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                }
            ),
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
