package com.keacs.app.ui.management

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.usecase.CategoryManagementUseCase
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.ConfirmDialog
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CategoryListScreen(
    repository: LocalDataRepository,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onEditCategory: (Long?, String) -> Unit,
) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val direction = categoryDirectionForTab(selectedIndex)
    val useCase = remember(repository) { CategoryManagementUseCase(repository) }
    val scope = rememberCoroutineScope()
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    val rowHeightPx = with(LocalDensity.current) { 54.dp.toPx() }
    val visibleCategories = categories.filter {
        if (selectedIndex == 2) {
            PresetSeedData.isAccountCategoryDirection(it.direction)
        } else {
            it.direction == direction
        }
    }
    val canReorder = selectedIndex != 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-category-list")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        SegmentedTabs(
            items = listOf("支出分类", "收入分类", "账户分类"),
            selectedIndex = selectedIndex,
            onSelected = onSelectedIndexChange,
        )
        KeacsCard(contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.padding(it)) {
                itemsIndexed(visibleCategories, key = { _, item -> item.id }) { index, category ->
                    val dragModifier = if (canReorder) {
                        Modifier
                            .zIndex(if (draggingId == category.id) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (draggingId == category.id) dragOffset else 0f
                            }
                            .pointerInput(category.id, visibleCategories) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = category.id
                                        dragStartIndex = index
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragStartIndex = null
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        val fromIndex = dragStartIndex
                                        if (fromIndex != null) {
                                            val targetIndex = (fromIndex + (dragOffset / rowHeightPx).roundToInt())
                                                .coerceIn(visibleCategories.indices)
                                            if (targetIndex != fromIndex) {
                                                val reordered = visibleCategories.toMutableList()
                                                val moved = reordered.removeAt(fromIndex)
                                                reordered.add(targetIndex, moved)
                                                scope.launch {
                                                    useCase.reorder(direction, reordered.map { item -> item.id })
                                                }
                                            }
                                        }
                                        draggingId = null
                                        dragStartIndex = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                    },
                                )
                            }
                    } else {
                        Modifier
                    }
                    ManagementListItem(
                        title = category.name,
                        subtitle = categorySubtitle(category),
                        icon = iconFor(category.iconKey),
                        color = colorFor(category.colorKey),
                        enabled = category.isEnabled,
                        modifier = dragModifier,
                        onClick = { onEditCategory(category.id, category.direction) },
                    )
                    if (index != visibleCategories.lastIndex) {
                        ListDivider()
                    }
                }
            }
        }
        Button(
            onClick = { onEditCategory(null, direction) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(newCategoryButtonText(selectedIndex))
        }
    }
}

@Composable
fun CategoryEditScreen(
    repository: LocalDataRepository,
    categoryId: Long?,
    initialDirection: String = PresetSeedData.CATEGORY_EXPENSE,
    onDone: () -> Unit,
) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val editing = categories.firstOrNull { it.id == categoryId }
    val useCase = remember(repository) { CategoryManagementUseCase(repository) }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(categoryId) { mutableStateOf("") }
    var direction by rememberSaveable(categoryId, initialDirection) { mutableStateOf(initialDirection) }
    var iconKey by rememberSaveable(categoryId) { mutableStateOf("food") }
    var colorKey by rememberSaveable(categoryId) { mutableStateOf("orange") }
    var isEnabled by rememberSaveable(categoryId) { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editing?.id) {
        editing?.let {
            name = it.name
            direction = it.direction
            iconKey = it.iconKey
            colorKey = it.colorKey
            isEnabled = it.isEnabled
        }
    }

    LaunchedEffect(direction) {
        val options = categoryOptions(direction)
        if (options.none { it.key == iconKey }) {
            options.firstOrNull()?.let {
                iconKey = it.key
                colorKey = it.colorKey
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen-category-edit")
            .padding(horizontal = KeacsSpacing.PageHorizontal, vertical = KeacsSpacing.PageVertical),
        verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
        ) {
            ManagementTextField("分类名称", name, {
                name = it.take(MAX_CATEGORY_NAME_LENGTH)
                error = null
            }, error = error)
            DirectionSelector(direction) { direction = it }
            if (PresetSeedData.isAccountCategoryDirection(direction)) {
                NatureSelector(PresetSeedData.accountCategoryNatureFor(direction)) {
                    direction = PresetSeedData.accountCategoryDirectionFor(it)
                }
            }
            SwitchCard(isEnabled, onCheckedChange = { isEnabled = it })
            ErrorText(error)
        }
        IconSelector(
            direction = direction, 
            selectedKey = iconKey,
            modifier = Modifier.weight(1f)
        ) {
            iconKey = it.key
            colorKey = it.colorKey
        }
        ActionButtons(
            deleteText = "删除分类",
            saveText = "保存分类",
            onDeleteClick = { confirmDelete = true },
            onSaveClick = {
                scope.launch {
                    runCatching {
                        useCase.save(categoryId, name, direction, iconKey, colorKey, isEnabled)
                    }.onSuccess { onDone() }
                        .onFailure { error = it.message ?: "保存失败，请稍后重试" }
                }
            },
            saveEnabled = name.isNotBlank(),
        )
    }

    if (confirmDelete) {
        DeleteDialog(
            title = "删除这个分类？",
            text = "删除后无法恢复。",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    runCatching { categoryId?.let { useCase.delete(it) } }
                        .onSuccess { onDone() }
                        .onFailure { error = it.message ?: "删除失败，请稍后重试" }
                }
            },
        )
    }
}

@Composable
private fun DirectionSelector(
    direction: String,
    onSelected: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OptionChip("支出", direction == PresetSeedData.CATEGORY_EXPENSE, Modifier.weight(1f)) {
            onSelected(PresetSeedData.CATEGORY_EXPENSE)
        }
        OptionChip("收入", direction == PresetSeedData.CATEGORY_INCOME, Modifier.weight(1f)) {
            onSelected(PresetSeedData.CATEGORY_INCOME)
        }
        OptionChip("账户", PresetSeedData.isAccountCategoryDirection(direction), Modifier.weight(1f)) {
            onSelected(PresetSeedData.CATEGORY_ACCOUNT_ASSET)
        }
    }
}

@Composable
fun NatureSelector(nature: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OptionChip("资产账户", nature == PresetSeedData.ACCOUNT_ASSET, Modifier.weight(1f)) {
            onSelected(PresetSeedData.ACCOUNT_ASSET)
        }
        OptionChip("负债账户", nature == PresetSeedData.ACCOUNT_LIABILITY, Modifier.weight(1f)) {
            onSelected(PresetSeedData.ACCOUNT_LIABILITY)
        }
    }
}

@Composable
private fun IconSelector(
    direction: String, 
    selectedKey: String, 
    modifier: Modifier = Modifier,
    onSelected: (IconOption) -> Unit
) {
    KeacsCard(modifier = modifier) {
        Column(Modifier.padding(it)) {
            Text("选择图标", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categoryOptions(direction), key = { option -> option.key }) { option ->
                    val selected = selectedKey == option.key
                    Column(
                        modifier = Modifier
                            .height(52.dp)
                            .shadow(
                                elevation = if (selected) 10.dp else 0.dp,
                                shape = MaterialTheme.shapes.medium,
                                ambientColor = KeacsColors.Primary.copy(alpha = 0.22f),
                                spotColor = KeacsColors.Primary.copy(alpha = 0.22f),
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selected) KeacsColors.PrimaryLight else Color.Transparent)
                            .clickable { onSelected(option) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (selected) KeacsColors.PrimaryLight else KeacsColors.Surface)
                                .border(
                                    BorderStroke(if (selected) 1.5.dp else 0.dp, KeacsColors.Primary),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            CategoryIcon(
                                icon = option.icon,
                                backgroundColor = if (selected) KeacsColors.Primary else colorFor(option.colorKey),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = title,
        text = text,
        confirmText = "删除",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
    )
}

private const val MAX_CATEGORY_NAME_LENGTH = 4

private fun categoryDirectionForTab(index: Int): String = when (index) {
    1 -> PresetSeedData.CATEGORY_INCOME
    2 -> PresetSeedData.CATEGORY_ACCOUNT_ASSET
    else -> PresetSeedData.CATEGORY_EXPENSE
}

private fun newCategoryButtonText(index: Int): String = when (index) {
    1 -> "新增收入分类"
    2 -> "新增账户分类"
    else -> "新增支出分类"
}

private fun categorySubtitle(category: CategoryEntity): String {
    if (!category.isEnabled) return "历史记录仍会显示"
    if (!PresetSeedData.isAccountCategoryDirection(category.direction)) return ""
    return if (PresetSeedData.accountCategoryNatureFor(category.direction) == PresetSeedData.ACCOUNT_LIABILITY) {
        "负债账户可选"
    } else {
        "资产账户可选"
    }
}
