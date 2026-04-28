package com.keacs.app.ui.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.ui.components.AmountText
import com.keacs.app.ui.components.CategoryIcon
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.NumberPad
import com.keacs.app.ui.record.MAX_AMOUNT_INPUT_LENGTH
import com.keacs.app.ui.theme.KeacsColors

@Composable
fun AccountTypeSelector(
    options: List<IconOption>,
    selectedType: String,
    onSelected: (IconOption) -> Unit,
) {
    KeacsCard {
        Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("账户类型", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(options, key = { option -> option.key + option.label }) { option ->
                    val selected = selectedType == option.label
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
                        Text(
                            text = option.label,
                            color = if (selected) KeacsColors.Primary else KeacsColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NatureSelector(nature: String, onSelected: (String) -> Unit) {
    KeacsCard {
        Column(Modifier.padding(it)) {
            Text("账户性质", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OptionChip("资产", nature == PresetSeedData.ACCOUNT_ASSET, Modifier.weight(1f)) {
                    onSelected(PresetSeedData.ACCOUNT_ASSET)
                }
                OptionChip("负债", nature == PresetSeedData.ACCOUNT_LIABILITY, Modifier.weight(1f)) {
                    onSelected(PresetSeedData.ACCOUNT_LIABILITY)
                }
            }
        }
    }
}

@Composable
fun AccountBalanceKeyboardPanel(
    balance: String,
    error: String?,
    saveEnabled: Boolean,
    onKeyClick: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    KeacsCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("当前余额", color = KeacsColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            AmountText(amount = balanceDisplay(balance))
            Text(
                text = error ?: " ",
                color = KeacsColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            NumberPad(
                saveEnabled = saveEnabled,
                onKeyClick = onKeyClick,
                onSaveClick = onSaveClick,
            )
        }
    }
}

fun centsToInput(value: Long): String =
    java.text.DecimalFormat("0.00").format(value / 100.0)

fun parseCent(text: String): Long? =
    runCatching {
        val value = text.trim().toBigDecimalOrNull() ?: return null
        if (value.scale() > 2) return null
        value.movePointRight(2).toLong()
    }.getOrNull()

fun balanceError(balance: String): String? =
    if (parseCent(balance) == null) "余额格式不正确" else null

fun nextBalanceAmount(current: String, key: String): String {
    if (key == "-") return if (current.startsWith("-")) current.removePrefix("-") else "-$current"
    if (key == "+") return current.removePrefix("-")
    if (key == "⌫") return current.dropLast(1).ifBlank { "0" }
    if (key == "." && current.contains(".")) return current
    val negative = current.startsWith("-")
    val raw = current.removePrefix("-")
    val base = if (raw == "0.00" || raw == "0") "" else raw
    val next = if (key == "." && base.isBlank()) "0." else base + key
    if (next.substringAfter('.', "").length > 2) return current
    val normalized = if (next.startsWith("0.")) next else next.trimStart('0').ifBlank { "0" }
    return if (negative) "-$normalized" else normalized
}

fun balanceInputWouldOverflow(next: String): Boolean {
    val maxLength = if (next.startsWith("-")) MAX_AMOUNT_INPUT_LENGTH + 1 else MAX_AMOUNT_INPUT_LENGTH
    return next.length > maxLength
}

private fun balanceDisplay(balance: String): String =
    balance.ifBlank { "0.00" }
