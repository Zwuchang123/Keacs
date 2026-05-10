package com.keacs.app.ui.scheduled

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.data.repository.ScheduledFrequency
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurrencePickerBottomSheet(
    frequency: String,
    recurrenceValues: String?,
    nextRunAt: Long,
    onSelected: (String, String, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFrequency = remember(frequency) { ScheduledRecordRepository.normalizeFrequency(frequency) }
    val initial = remember(nextRunAt) {
        Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = nextRunAt }
    }
    var selectedFrequency by remember(frequency) { mutableStateOf(initialFrequency) }
    var selectedWeekdays by remember(recurrenceValues, nextRunAt) {
        mutableStateOf(
            parseWeekdays(
                values = recurrenceValues.takeIf { initialFrequency == ScheduledFrequency.WEEKLY },
                fallback = initial.get(Calendar.DAY_OF_WEEK),
            ).toSet(),
        )
    }
    var selectedMonthDays by remember(recurrenceValues, nextRunAt) {
        mutableStateOf(
            parseMonthDays(
                values = recurrenceValues.takeIf { initialFrequency == ScheduledFrequency.MONTHLY },
                fallback = initial.get(Calendar.DAY_OF_MONTH),
            ).toSet(),
        )
    }
    val initialYearlyValues = remember(recurrenceValues, nextRunAt) {
        parseYearlyValues(
            values = recurrenceValues.takeIf { initialFrequency == ScheduledFrequency.YEARLY },
            fallbackMonth = initial.get(Calendar.MONTH) + 1,
            fallbackDay = initial.get(Calendar.DAY_OF_MONTH),
        )
    }
    var selectedYearMonths by remember(initialYearlyValues) {
        mutableStateOf(initialYearlyValues.map { it.month }.toSet())
    }
    var selectedYearDays by remember(initialYearlyValues) {
        mutableStateOf(initialYearlyValues.map { it.day }.toSet())
    }

    fun selectedValues(): String = when (selectedFrequency) {
        ScheduledFrequency.WEEKLY -> selectedWeekdays
            .sortedWith(compareBy { weekdayValues.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })
            .joinToString(",")
        ScheduledFrequency.YEARLY -> selectedYearMonths.sorted().flatMap { month ->
            selectedYearDays.sorted().map { day -> "$month-$day" }
        }.joinToString(",")
        else -> selectedMonthDays.sorted().joinToString(",")
    }

    val canConfirm = selectedValues().isNotBlank()

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
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = "生成时间",
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
            SegmentedTabs(
                items = frequencyLabels,
                selectedIndex = frequencyValues.indexOf(selectedFrequency).coerceAtLeast(0),
                onSelected = { selectedFrequency = frequencyValues[it] },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedFrequency) {
                    ScheduledFrequency.WEEKLY -> MultiChoiceGroup(
                        title = "选择星期",
                        description = "选中的星期会在每天 09:00 自动生成",
                        options = weekdayValues.mapIndexed { index, value -> value to weekdayLabels[index] },
                        selectedValues = selectedWeekdays,
                        onToggle = { value -> selectedWeekdays = selectedWeekdays.toggle(value) },
                    )
                    ScheduledFrequency.YEARLY -> {
                        MultiChoiceGroup(
                            title = "选择月份",
                            description = "先选月份，再选日期",
                            options = (1..12).map { it to "${it}月" },
                            selectedValues = selectedYearMonths,
                            onToggle = { value -> selectedYearMonths = selectedYearMonths.toggle(value) },
                        )
                        MultiChoiceGroup(
                            title = "选择日期",
                            description = "选中的日期会在 09:00 自动生成",
                            options = (1..31).map { it to "${it}日" },
                            selectedValues = selectedYearDays,
                            onToggle = { value -> selectedYearDays = selectedYearDays.toggle(value) },
                        )
                    }
                    else -> MultiChoiceGroup(
                        title = "选择日期",
                        description = "选中的日期会在 09:00 自动生成",
                        options = (1..31).map { it to "${it}日" },
                        selectedValues = selectedMonthDays,
                        onToggle = { value -> selectedMonthDays = selectedMonthDays.toggle(value) },
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
                    onClick = {
                        val values = selectedValues()
                        val nextTime = ScheduledRecordRepository.nextOccurrence(
                            frequency = selectedFrequency,
                            month = null,
                            day = null,
                            weekday = null,
                            recurrenceValues = values,
                            hour = ScheduledRecordRepository.DEFAULT_RECURRENCE_HOUR,
                            afterMillis = System.currentTimeMillis(),
                        )
                        onSelected(selectedFrequency, values, nextTime)
                    },
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("确定")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceGroup(
    title: String,
    description: String,
    options: List<Pair<Int, String>>,
    selectedValues: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(title, color = KeacsColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "已选 ${selectedValues.size}",
                color = KeacsColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(description, color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            options.forEach { (value, label) ->
                ChoiceChip(
                    label = label,
                    selected = value in selectedValues,
                    onClick = { onToggle(value) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (selected) KeacsColors.Primary else KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (selected) KeacsColors.Surface else KeacsColors.TextPrimary,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
}

private fun Set<Int>.toggle(value: Int): Set<Int> =
    if (value in this) this - value else this + value
