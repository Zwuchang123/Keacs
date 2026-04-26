package com.keacs.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.keacs.app.ui.theme.KeacsColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeacsScaffold(
    title: String,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    actionText: String? = null,
    actionEnabled: Boolean = true,
    onBackClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    actions: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = KeacsColors.Background,
        snackbarHost = snackbarHost,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回",
                                tint = KeacsColors.TextPrimary,
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.testTag("screen-title"),
                    )
                },
                actions = {
                    if (actionText != null) {
                        TextButton(
                            onClick = onActionClick,
                            enabled = actionEnabled,
                        ) {
                            Text(text = actionText)
                        }
                    } else {
                        actions()
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = KeacsColors.Background,
                    titleContentColor = KeacsColors.TextPrimary,
                    navigationIconContentColor = KeacsColors.TextPrimary,
                    actionIconContentColor = KeacsColors.TextPrimary,
                ),
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}
