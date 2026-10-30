package com.osoterra.mobile.ui.demo

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.data.demo.DemoScenario
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.ui.theme.Emerald100
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.Emerald900
import com.osoterra.mobile.ui.theme.Line
import com.osoterra.mobile.ui.theme.SurfaceGreen
import com.osoterra.mobile.ui.theme.White

@StringRes
fun DemoScenario.titleRes(): Int = when (this) {
    DemoScenario.NO_PLOTS -> R.string.demo_scenario_1_title
    DemoScenario.ALL_NORMAL -> R.string.demo_scenario_2_title
    DemoScenario.CRITICAL_ALERT -> R.string.demo_scenario_3_title
    DemoScenario.ADVISOR_REQUEST -> R.string.demo_scenario_4_title
    DemoScenario.ADVISOR_CLIENTS -> R.string.demo_scenario_5_title
    DemoScenario.ADVISOR_ALERTS -> R.string.demo_scenario_6_title
}

@StringRes
fun DemoScenario.descriptionRes(): Int = when (this) {
    DemoScenario.NO_PLOTS -> R.string.demo_scenario_1_desc
    DemoScenario.ALL_NORMAL -> R.string.demo_scenario_2_desc
    DemoScenario.CRITICAL_ALERT -> R.string.demo_scenario_3_desc
    DemoScenario.ADVISOR_REQUEST -> R.string.demo_scenario_4_desc
    DemoScenario.ADVISOR_CLIENTS -> R.string.demo_scenario_5_desc
    DemoScenario.ADVISOR_ALERTS -> R.string.demo_scenario_6_desc
}

@StringRes
private fun DemoScenario.roleRes(): Int =
    if (role == UserRole.ADVISOR) R.string.demo_role_advisor else R.string.demo_role_producer

/**
 * Fila de los 6 escenarios numerados (48 dp cada uno, 8 dp entre sí). Si la pantalla es
 * angosta pasa a dos filas en lugar de achicar los botones. Pulsar el número seleccionado
 * vuelve a cargar el escenario desde cero.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoScenarioRow(
    selected: DemoScenario?,
    enabled: Boolean,
    onSelect: (DemoScenario) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoScenario.entries.forEach { scenario ->
            val isSelected = scenario == selected
            val description = stringResource(
                R.string.demo_scenario_cd,
                scenario.number,
                stringResource(scenario.titleRes()),
                stringResource(scenario.roleRes()),
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) Emerald700 else White,
                contentColor = if (isSelected) White else Emerald800,
                border = if (isSelected) null else BorderStroke(1.5.dp, Emerald700),
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = description }
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(scenario) },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = scenario.number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }
        }
    }
}

/** Contenido común del panel del login y de la hoja «Datos de prueba». */
@Composable
fun DemoScenarioChooser(
    selected: DemoScenario?,
    busy: Boolean,
    onSelect: (DemoScenario) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.demo_mode_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DemoScenarioRow(selected = selected, enabled = !busy, onSelect = onSelect)
        if (selected != null) {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    text = "${selected.number}. ${stringResource(selected.titleRes())}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(selected.descriptionRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.demo_mode_restart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        OutlinedButton(
            onClick = onExit,
            enabled = !busy,
            shape = RoundedCornerShape(50),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.demo_mode_exit), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Panel del login cuando el modo de prueba está activo. */
@Composable
fun DemoLoginPanel(
    selected: DemoScenario?,
    busy: Boolean,
    onSelect: (DemoScenario) -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGreen, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Science, contentDescription = null, tint = Emerald700)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.demo_mode_title),
                style = MaterialTheme.typography.titleMedium,
                color = Emerald900,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(8.dp))
        DemoScenarioChooser(selected = selected, busy = busy, onSelect = onSelect, onExit = onExit)
    }
}

/** Hoja con los 6 escenarios, accesible desde cualquier pantalla con sesión de prueba. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScenarioSheet(
    selected: DemoScenario?,
    busy: Boolean,
    onSelect: (DemoScenario) -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.demo_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            DemoScenarioChooser(selected = selected, busy = busy, onSelect = onSelect, onExit = onExit)
        }
    }
}
