package com.osoterra.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.ui.theme.Emerald600

/**
 * Botón de ayuda junto a la unidad (área táctil de 48 dp): explica en pocas frases qué es la
 * ECe, qué significa dS/m y desde cuándo la sal afecta al cultivo. Una sola sigla, ECe, en toda la app.
 */
@Composable
fun EceHelpButton(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.ece_help_open),
            tint = Emerald600,
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.ece_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ece_help_what), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.ece_help_unit), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.ece_help_threshold), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.ece_help_close)) }
            },
        )
    }
}
