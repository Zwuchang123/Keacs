package com.keacs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.flow.distinctUntilChanged

data class WheelPickerColumn(
    val items: List<String>,
    val selectedIndex: Int,
    val onSelected: (Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelPickerBottomSheet(
    title: String,
    columns: List<WheelPickerColumn>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
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
                .padding(top = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                color = KeacsColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                columns.forEach { column ->
                    WheelColumn(
                        column = column,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确定")
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    column: WheelPickerColumn,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = column.selectedIndex.coerceIn(column.items.indices)
    val displayItems = listOf("") + column.items + listOf("")
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedIndex, column.items.size) {
        listState.scrollToItem(selectedIndex)
    }

    LaunchedEffect(listState, column.items.size) {
        snapshotFlow { listState.firstVisibleItemIndex.coerceIn(column.items.indices) }
            .distinctUntilChanged()
            .collect { centerIndex ->
                if (centerIndex != column.selectedIndex) {
                    column.onSelected(centerIndex)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .height(132.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(KeacsColors.SurfaceSubtle)
            .padding(vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(displayItems) { index, item ->
            val realIndex = index - 1
            val selected = realIndex == selectedIndex
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = realIndex in column.items.indices) { column.onSelected(realIndex) }
                    .background(
                        if (selected) KeacsColors.PrimaryLight else KeacsColors.SurfaceSubtle,
                        MaterialTheme.shapes.small,
                    )
                    .height(44.dp)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                color = if (selected) KeacsColors.Primary else KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}
