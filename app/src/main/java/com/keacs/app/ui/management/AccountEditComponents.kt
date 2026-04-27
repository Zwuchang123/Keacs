package com.keacs.app.ui.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keacs.app.data.local.database.PresetSeedData
import com.keacs.app.ui.components.AmountText
import com.keacs.app.ui.components.KeacsCard
import com.keacs.app.ui.components.NumberPad
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
            options.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { option ->
                        OptionChip(
                            text = option.label,
                            selected = selectedType == option.label,
                            modifier = Modifier.weight(1f),
                        ) { onSelected(option) }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
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
    KeacsCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
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

private fun balanceDisplay(balance: String): String =
    balance.ifBlank { "0.00" }
