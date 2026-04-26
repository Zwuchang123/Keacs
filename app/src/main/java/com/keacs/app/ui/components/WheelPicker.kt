package com.keacs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> WheelPickerBottomSheet(
    title: String,
    items: List<T>,
    selectedIndex: Int,
    itemToString: (T) -> String,
    onItemSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KeacsColors.Surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KeacsSpacing.PageHorizontal)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "取消",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp),
                )
                Text(
                    text = title,
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "确定",
                    color = KeacsColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            onItemSelected(currentIndex)
                            onDismiss()
                        }
                        .padding(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(KeacsColors.SurfaceSubtle, MaterialTheme.shapes.medium),
            ) {
                val listState = rememberLazyListState()
                val halfVisibleItems = 2

                val centerIndex by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) currentIndex
                        else {
                            val centerOffset = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
                            visibleItems.minByOrNull { item ->
                                kotlin.math.abs((item.offset + item.size / 2) - centerOffset)
                            }?.index ?: currentIndex
                        }
                    }
                }

                LaunchedEffect(selectedIndex) {
                    if (currentIndex != selectedIndex) {
                        currentIndex = selectedIndex
                        listState.scrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
                    }
                }

                LaunchedEffect(listState) {
                    snapshotFlow { centerIndex }
                        .distinctUntilChanged()
                        .collect { index ->
                            currentIndex = index
                        }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(items) { index, item ->
                        val distanceFromCenter = kotlin.math.abs(index - centerIndex)
                        val alpha = when (distanceFromCenter) {
                            0 -> 1f
                            1 -> 0.6f
                            else -> 0.3f
                        }
                        val scale = when (distanceFromCenter) {
                            0 -> 1.1f
                            1 -> 1.0f
                            else -> 0.9f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .alpha(alpha)
                                .clickable {
                                    currentIndex = index
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = itemToString(item),
                                    color = if (index == currentIndex) KeacsColors.Primary else KeacsColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (index == currentIndex) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                )
                                if (index == currentIndex) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = KeacsColors.Primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearMonthWheelPicker(
    currentYear: Int,
    currentMonth: Int,
    onDateSelected: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentYearInt = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (currentYearInt - 10..currentYearInt + 2).toList().reversed()
    val months = (1..12).toList()

    var selectedYearIndex by remember { mutableIntStateOf(years.indexOf(currentYear).coerceAtLeast(0)) }
    var selectedMonthIndex by remember { mutableIntStateOf((currentMonth - 1).coerceIn(0, 11)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KeacsColors.Surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KeacsSpacing.PageHorizontal)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "取消",
                    color = KeacsColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp),
                )
                Text(
                    text = "选择年月",
                    color = KeacsColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "确定",
                    color = KeacsColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            onDateSelected(years[selectedYearIndex], selectedMonthIndex + 1)
                            onDismiss()
                        }
                        .padding(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(KeacsColors.SurfaceSubtle, MaterialTheme.shapes.medium),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                YearWheelPicker(
                    years = years,
                    selectedIndex = selectedYearIndex,
                    onIndexChanged = { selectedYearIndex = it },
                    modifier = Modifier.weight(1f),
                )
                MonthWheelPicker(
                    months = months,
                    selectedIndex = selectedMonthIndex,
                    onIndexChanged = { selectedMonthIndex = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun YearWheelPicker(
    years: List<Int>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                currentIndex = index
                onIndexChanged(index)
            }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(years) { index, year ->
                val distanceFromCenter = kotlin.math.abs(index - currentIndex)
                val alpha = when (distanceFromCenter) {
                    0 -> 1f
                    1 -> 0.5f
                    else -> 0.25f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .alpha(alpha)
                        .clickable {
                            currentIndex = index
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${year}年",
                        color = if (index == currentIndex) KeacsColors.Primary else KeacsColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (index == currentIndex) FontWeight.SemiBold else FontWeight.Normal
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthWheelPicker(
    months: List<Int>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                currentIndex = index
                onIndexChanged(index)
            }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(months) { index, month ->
                val distanceFromCenter = kotlin.math.abs(index - currentIndex)
                val alpha = when (distanceFromCenter) {
                    0 -> 1f
                    1 -> 0.5f
                    else -> 0.25f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .alpha(alpha)
                        .clickable {
                            currentIndex = index
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${month}月",
                        color = if (index == currentIndex) KeacsColors.Primary else KeacsColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (index == currentIndex) FontWeight.SemiBold else FontWeight.Normal
                        ),
                    )
                }
            }
        }
    }
}
