package com.keacs.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = MaterialTheme.shapes.large,
                spotColor = KeacsColors.Primary.copy(alpha = 0.04f),
                ambientColor = KeacsColors.Primary.copy(alpha = 0.04f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = KeacsColors.Surface,
            contentColor = KeacsColors.TextPrimary,
        ),
        border = BorderStroke(0.5.dp, KeacsColors.Border.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        content(contentPadding)
    }
}
