package com.keacs.app.ui.scheduled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.entity.ScheduledRecordEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.data.repository.ScheduledRecordRepository
import com.keacs.app.domain.model.RecordType
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.MenuDivider
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun ScheduledRecordListScreen(
    repository: LocalDataRepository,
    scheduledRepository: ScheduledRecordRepository,
    onEditSchedule: (Long?) -> Unit,
) {
    val schedules by scheduledRepository.observeSchedules().collectAsState(initial = emptyList())
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccounts().collectAsState(initial = emptyList())
    val accountNames = accounts.associate { it.id to it.name }
    val sortedSchedules = schedules.sortedWith(scheduledRecordComparator)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-scheduled-list")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        KeacsCard(contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.padding(it)) {
                if (sortedSchedules.isEmpty()) {
                    item {
                        Text(
                            text = "还没有定时记账",
                            color = KeacsColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                itemsIndexed(sortedSchedules, key = { _, item -> item.id }) { index, schedule ->
                    ScheduledRow(
                        schedule = schedule,
                        category = categories.firstOrNull { it.id == schedule.categoryId },
                        accountNames = accountNames,
                        onClick = { onEditSchedule(schedule.id) },
                    )
                    if (index != sortedSchedules.lastIndex) MenuDivider()
                }
            }
        }
        Button(onClick = { onEditSchedule(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("新增定时记账")
        }
    }
}

private val scheduledRecordComparator = compareBy<ScheduledRecordEntity> { scheduledTypeOrder(it.type) }
    .thenByDescending { it.isEnabled }
    .thenBy { it.nextRunAt }
    .thenByDescending { it.id }

private fun scheduledTypeOrder(type: String): Int = when (type) {
    RecordType.EXPENSE -> 0
    RecordType.INCOME -> 1
    RecordType.TRANSFER -> 2
    else -> 3
}
