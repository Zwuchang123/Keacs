package com.keacs.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.keacs.app.ui.theme.KeacsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeacsScaffold(
    title: String,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = KeacsColors.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.testTag("screen-title"),
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = KeacsColors.Background,
                    titleContentColor = KeacsColors.TextPrimary,
                ),
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}
