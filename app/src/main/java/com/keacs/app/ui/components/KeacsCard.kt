package com.keacs.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSpacing

@Composable
fun KeacsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(KeacsSpacing.CardPadding),
    content: @Composable (PaddingValues) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = KeacsColors.Surface,
            contentColor = KeacsColors.TextPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        content(contentPadding)
    }
}
