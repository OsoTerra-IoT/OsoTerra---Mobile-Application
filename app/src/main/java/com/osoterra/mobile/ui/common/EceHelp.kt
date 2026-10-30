package com.osoterra.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.ui.theme.Emerald600

/**
 * Botón de ayuda (área táctil de 48 dp): abre una hoja inferior que explica en pocas frases qué es
 * la ECe, qué significa dS/m y desde cuándo la sal afecta al cultivo. [extra] agrega la lectura
 * del gráfico o de la franja de la pantalla, para no ocupar la pantalla con párrafos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EceHelpButton(modifier: Modifier = Modifier, extra: String? = null) {
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.ece_help_open),
            tint = Emerald600,
        )
    }
    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.ece_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                HelpRow(Icons.Outlined.WaterDrop, stringResource(R.string.ece_help_what))
                HelpRow(Icons.Outlined.Straighten, stringResource(R.string.ece_help_unit))
                HelpRow(Icons.Outlined.Speed, stringResource(R.string.ece_help_threshold))
                extra?.let { HelpRow(Icons.Outlined.ShowChart, it) }
                TextButton(
                    onClick = { open = false },
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.ece_help_close)) }
            }
        }
    }
}

@Composable
private fun HelpRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = Emerald600, modifier = Modifier.size(24.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
