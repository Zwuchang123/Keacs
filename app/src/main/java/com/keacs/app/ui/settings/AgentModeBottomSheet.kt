package com.keacs.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.domain.agent.AgentModelServiceMode
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentModeBottomSheet(
    selectedMode: AgentModelServiceMode,
    onSelected: (AgentModelServiceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(AgentModelServiceMode.OFFICIAL, AgentModelServiceMode.CUSTOM)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KeacsColors.Surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KeacsSpacing.PageHorizontal)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = "选择模型服务",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "取消",
                        tint = KeacsColors.TextSecondary,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(options) { option ->
                    AgentModeOptionRow(
                        mode = option,
                        selected = option == selectedMode,
                        onClick = {
                            onSelected(option)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentModeOptionRow(
    mode: AgentModelServiceMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mode.label(),
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (selected) {
            Text(
                text = "已选",
                color = KeacsColors.Primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

fun AgentModelServiceMode.label(): String = when (this) {
    AgentModelServiceMode.OFFICIAL -> "官方免费服务"
    AgentModelServiceMode.CUSTOM -> "自定义服务"
}
