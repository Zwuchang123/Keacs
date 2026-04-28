package com.keacs.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.AccountEntity
import com.keacs.app.ui.components.WheelPickerBottomSheet
import com.keacs.app.ui.components.WheelPickerColumn
import com.keacs.app.ui.management.colorFor
import com.keacs.app.ui.management.iconFor
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import java.util.Calendar
import java.util.Locale

enum class DatePickerMode { DAY, MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectorBottomSheet(
    accounts: List<AccountEntity>,
    selectedId: Long?,
    title: String,
    includeNone: Boolean,
    onSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = title,
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
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (includeNone) {
                    item {
                        AccountPickerRow(
                            name = "不选账户",
                            iconKey = "more",
                            colorKey = "gray",
                            selected = selectedId == null,
                            onClick = {
                                onSelected(null)
                                onDismiss()
                            },
                        )
                    }
                }
                items(accounts, key = { it.id }) { account ->
                    AccountPickerRow(
                        name = account.name,
                        iconKey = account.iconKey,
                        colorKey = account.colorKey,
                        selected = selectedId == account.id,
                        onClick = {
                            onSelected(account.id)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountPickerRow(
    name: String,
    iconKey: String,
    colorKey: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colorFor(colorKey).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(iconKey),
                contentDescription = null,
                tint = colorFor(colorKey),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            color = if (selected) KeacsColors.Primary else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "已选", tint = KeacsColors.Primary)
        }
    }
}

@Composable
fun DateWheelPickerBottomSheet(
    title: String,
    selectedDate: Long,
    mode: DatePickerMode,
    onSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val calendar = remember(selectedDate) {
        Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = selectedDate }
    }
    val thisYear = Calendar.getInstance(Locale.getDefault()).get(Calendar.YEAR)
    val years = remember { (thisYear - 10..thisYear + 2).map { "${it}年" } }
    val months = remember { (1..12).map { "${it}月" } }

    var yearIndex by remember(selectedDate) { mutableIntStateOf((calendar.get(Calendar.YEAR) - (thisYear - 10)).coerceIn(years.indices)) }
    var monthIndex by remember(selectedDate) { mutableIntStateOf(calendar.get(Calendar.MONTH).coerceIn(months.indices)) }
    val days = remember(yearIndex, monthIndex) {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.set(thisYear - 10 + yearIndex, monthIndex, 1)
        (1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)).map { "${it}日" }
    }
    var dayIndex by remember(selectedDate, days.size) {
        mutableIntStateOf((calendar.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(days.indices))
    }

    WheelPickerBottomSheet(
        title = title,
        columns = buildList {
            add(WheelPickerColumn(years, yearIndex) { yearIndex = it })
            if (mode != DatePickerMode.YEAR) add(WheelPickerColumn(months, monthIndex) { monthIndex = it })
            if (mode == DatePickerMode.DAY) add(WheelPickerColumn(days, dayIndex) { dayIndex = it })
        },
        onDismiss = onDismiss,
        onConfirm = {
            val result = Calendar.getInstance(Locale.getDefault())
            result.set(thisYear - 10 + yearIndex, monthIndex, dayIndex + 1, 0, 0, 0)
            result.set(Calendar.MILLISECOND, 0)
            onSelected(result.timeInMillis)
        },
    )
}
