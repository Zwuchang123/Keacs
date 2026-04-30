package com.keacs.app.ui.management

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.data.local.entity.CategoryEntity
import com.keacs.app.data.repository.LocalDataRepository
import com.keacs.app.domain.usecase.CategoryManagementUseCase
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.SegmentedTabs
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing
import kotlinx.coroutines.launch

@Composable
fun CategoryListScreen(
    repository: LocalDataRepository,
    onEditCategory: (Long?) -> Unit,
) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val direction = when (selectedIndex) {
        1 -> PresetSeedData.CATEGORY_INCOME
        2 -> PresetSeedData.CATEGORY_ACCOUNT
        else -> PresetSeedData.CATEGORY_EXPENSE
    }
    val visibleCategories = categories.filter { it.direction == direction }

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
            onSelected = { selectedIndex = it },
        )
        KeacsCard(contentPadding = PaddingValues(0.dp), modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.padding(it)) {
                itemsIndexed(visibleCategories, key = { _, item -> item.id }) { index, category ->
                    ManagementListItem(
                        title = category.name,
                        subtitle = if (category.isEnabled) "新建记录可选" else "历史记录仍会显示",
                        icon = iconFor(category.iconKey),
                        color = colorFor(category.colorKey),
                        enabled = category.isEnabled,
                        onClick = { onEditCategory(category.id) },
                    )
                    if (index != visibleCategories.lastIndex) {
                        ListDivider()
                    }
                }
            }
        }
        Button(
            onClick = { onEditCategory(null) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (direction == PresetSeedData.CATEGORY_ACCOUNT) "＋ 新增账户分类" else "＋ 新增分类")
        }
    }
}

@Composable
fun CategoryEditScreen(
    repository: LocalDataRepository,
    categoryId: Long?,
    onDone: () -> Unit,
) {
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val editing = categories.firstOrNull { it.id == categoryId }
    val useCase = remember(repository) { CategoryManagementUseCase(repository) }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(categoryId) { mutableStateOf("") }
    var direction by rememberSaveable(categoryId) { mutableStateOf(PresetSeedData.CATEGORY_EXPENSE) }
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
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KeacsSpacing.Section),
        ) {
            ManagementTextField("分类名称", name, {
                name = it.take(MAX_CATEGORY_NAME_LENGTH)
                error = null
            }, error = error)
            DirectionSelector(direction) { direction = it }
            IconSelector(direction, iconKey) {
                iconKey = it.key
                colorKey = it.colorKey
            }
            SwitchCard(isEnabled, onCheckedChange = { isEnabled = it })
            ErrorText(error)
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
            text = "没有历史记录时可以删除；已经用过的分类不能删除，只能停用。",
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
    KeacsCard {
        Column(Modifier.padding(it)) {
            Text("分类方向", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OptionChip("支出", direction == PresetSeedData.CATEGORY_EXPENSE, Modifier.weight(1f)) {
                    onSelected(PresetSeedData.CATEGORY_EXPENSE)
                }
                OptionChip("收入", direction == PresetSeedData.CATEGORY_INCOME, Modifier.weight(1f)) {
                    onSelected(PresetSeedData.CATEGORY_INCOME)
                }
                OptionChip("账户", direction == PresetSeedData.CATEGORY_ACCOUNT, Modifier.weight(1f)) {
                    onSelected(PresetSeedData.CATEGORY_ACCOUNT)
                }
            }
        }
    }
}

@Composable
private fun IconSelector(direction: String, selectedKey: String, onSelected: (IconOption) -> Unit) {
    KeacsCard {
        Column(Modifier.padding(it)) {
            Text("选择图标", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(252.dp)
                    .padding(top = 10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categoryOptions(direction), key = { option -> option.key + option.label }) { option ->
                    val selected = selectedKey == option.key
                    Column(
                        modifier = Modifier
                            .height(68.dp)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = KeacsColors.Error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private const val MAX_CATEGORY_NAME_LENGTH = 4
