package com.keacs.app.ui.scheduled

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import com.keacs.app.ui.components.WheelPickerRow
import com.keacs.app.ui.components.WheelPickerColumn

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
        mutableStateOf<Set<Int>>(
            recurrenceValues
                .takeIf { initialFrequency == ScheduledFrequency.WEEKLY && !it.isNullOrBlank() }
                ?.let { parseWeekdays(values = it, fallback = initial.get(Calendar.DAY_OF_WEEK)).toSet() }
                ?: emptySet(),
        )
    }
    var selectedMonthDays by remember(recurrenceValues, nextRunAt) {
        mutableStateOf<Set<Int>>(
            recurrenceValues
                .takeIf { initialFrequency == ScheduledFrequency.MONTHLY && !it.isNullOrBlank() }
                ?.let { parseMonthDays(values = it, fallback = initial.get(Calendar.DAY_OF_MONTH)).toSet() }
                ?: emptySet(),
        )
    }
    val initialYearlyValues = remember(recurrenceValues, nextRunAt) {
        recurrenceValues
            .takeIf { initialFrequency == ScheduledFrequency.YEARLY && !it.isNullOrBlank() }
            ?.let {
                parseYearlyValues(
                    values = it,
                    fallbackMonth = initial.get(Calendar.MONTH) + 1,
                    fallbackDay = initial.get(Calendar.DAY_OF_MONTH),
                )
            }
            ?: emptyList()
    }
    var selectedYearlyValues by remember(initialYearlyValues) {
        mutableStateOf(initialYearlyValues.toSet())
    }

    var pickerMonthIndex by remember { mutableIntStateOf(0) }
    var pickerDayIndex by remember { mutableIntStateOf(0) }

    fun selectedValues(): String = when (selectedFrequency) {
        ScheduledFrequency.WEEKLY -> selectedWeekdays
            .sortedWith(compareBy { weekdayValues.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })
            .joinToString(",")
        ScheduledFrequency.YEARLY -> selectedYearlyValues
            .sortedWith(compareBy<YearlyValue> { it.month }.thenBy { it.day })
            .joinToString(",") { "${it.month}-${it.day}" }
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
                    text = "记账时间",
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
                        options = weekdayValues.mapIndexed { index, value -> value to weekdayLabels[index] },
                        selectedValues = selectedWeekdays,
                        onToggle = { value -> selectedWeekdays = selectedWeekdays.toggle(value) },
                    )
                    ScheduledFrequency.YEARLY -> {
                        if (selectedYearlyValues.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            ) {
                                selectedYearlyValues.sortedWith(compareBy<YearlyValue> { it.month }.thenBy { it.day }).forEach { yearly ->
                                    val text = "${yearly.month}月${yearly.day}日"
                                    Box(
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .background(KeacsColors.PrimaryLight)
                                            .clickable { selectedYearlyValues = selectedYearlyValues - yearly }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Text(
                                            text = text,
                                            color = KeacsColors.Primary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            WheelPickerRow(
                                columns = listOf(
                                    WheelPickerColumn(
                                        items = (1..12).map { "${it}月" },
                                        selectedIndex = pickerMonthIndex,
                                        onSelected = { pickerMonthIndex = it }
                                    ),
                                    WheelPickerColumn(
                                        items = (1..31).map { "${it}日" },
                                        selectedIndex = pickerDayIndex,
                                        onSelected = { pickerDayIndex = it }
                                    )
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val month = pickerMonthIndex + 1
                                    val day = pickerDayIndex + 1
                                    selectedYearlyValues = selectedYearlyValues + YearlyValue(month, day)
                                },
                            ) {
                                Text("添加")
                            }
                        }
                    }
                    else -> MultiChoiceGroup(
                        title = "选择日期",
                        options = (1..31).map { it to "${it}日" },
                        selectedValues = selectedMonthDays,
                        onToggle = { value -> selectedMonthDays = selectedMonthDays.toggle(value) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
        }
        
        if (options.size > 12) {
            // For 31 days, use a 7-column grid layout using rows
            val chunkedOptions = options.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunkedOptions.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { (value, label) ->
                            Box(modifier = Modifier.weight(1f)) {
                                GridChoiceChip(
                                    label = label,
                                    selected = value in selectedValues,
                                    onClick = { onToggle(value) }
                                )
                            }
                        }
                        // Fill remaining space if row is not full
                        repeat(7 - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (options.size == 12) {
            // For 12 months, use a 4-column grid layout
            val chunkedOptions = options.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunkedOptions.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { (value, label) ->
                            Box(modifier = Modifier.weight(1f)) {
                                GridChoiceChip(
                                    label = label,
                                    selected = value in selectedValues,
                                    onClick = { onToggle(value) }
                                )
                            }
                        }
                        repeat(4 - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // For 7 weekdays or fewer, use FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { (value, label) ->
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        GridChoiceChip(
                            label = label,
                            selected = value in selectedValues,
                            onClick = { onToggle(value) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val displayLabel = if (label.startsWith("周")) label else label.replace("日", "").replace("月", "")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) KeacsColors.Primary else KeacsColors.SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = displayLabel,
            color = if (selected) KeacsColors.Surface else KeacsColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun Set<Int>.toggle(value: Int): Set<Int> =
    if (value in this) this - value else this + value
