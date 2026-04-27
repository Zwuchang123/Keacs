package com.keacs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.navigation.KeacsDestination
import com.keacs.app.ui.navigation.bottomDestinations
import com.keacs.app.ui.theme.KeacsColors
import com.keacs.app.ui.theme.KeacsSize
import com.keacs.app.ui.theme.icon

@Composable
fun KeacsBottomBar(
    currentDestination: KeacsDestination,
    onDestinationSelected: (KeacsDestination) -> Unit,
) {
    Surface(
        color = KeacsColors.Surface,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(KeacsSize.BottomBarHeight),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomDestinations.forEach { destination ->
                if (destination == KeacsDestination.Add) {
                    AddDestinationItem(
                        selected = destination == currentDestination,
                        destination = destination,
                        onClick = { onDestinationSelected(destination) },
                    )
                } else {
                    BottomDestinationItem(
                        selected = destination == currentDestination,
                        destination = destination,
                        onClick = { onDestinationSelected(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomDestinationItem(
    selected: Boolean,
    destination: KeacsDestination,
    onClick: () -> Unit,
) {
    val label = stringResource(destination.titleRes)
    val description = stringResource(destination.contentDescriptionRes)
    val tint = if (selected) KeacsColors.Primary else KeacsColors.TextTertiary

    Column(
        modifier = Modifier
            .size(width = 58.dp, height = 62.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon(),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(23.dp),
        )
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun AddDestinationItem(
    selected: Boolean,
    destination: KeacsDestination,
    onClick: () -> Unit,
) {
    val description = stringResource(destination.contentDescriptionRes)

    Column(
        modifier = Modifier
            .size(width = 64.dp, height = 62.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(KeacsSize.AddButton)
                .clip(CircleShape)
                .background(KeacsColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = destination.icon(),
                contentDescription = null,
                tint = KeacsColors.Surface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
