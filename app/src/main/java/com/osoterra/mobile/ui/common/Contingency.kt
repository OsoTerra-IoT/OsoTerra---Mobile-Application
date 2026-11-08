package com.osoterra.mobile.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.ContingencyState
import com.osoterra.mobile.domain.model.CorrectiveActionRecord
import com.osoterra.mobile.domain.model.CorrectiveActionType
import com.osoterra.mobile.domain.model.SalinityContingency
import com.osoterra.mobile.ui.theme.Emerald50
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SageInk
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityCriticalContainer
import com.osoterra.mobile.ui.theme.Track

@Composable
fun CorrectiveActionType.label(): String = stringResource(
    when (this) {
        CorrectiveActionType.LEACHING -> R.string.action_leaching
        CorrectiveActionType.IRRIGATION_ADJUSTMENT -> R.string.action_irrigation_adjustment
        CorrectiveActionType.DRAINAGE_IMPROVEMENT -> R.string.action_drainage_improvement
        CorrectiveActionType.SOIL_AMENDMENT -> R.string.action_soil_amendment
        CorrectiveActionType.CROP_ROTATION -> R.string.action_crop_rotation
    },
)

/** Texto de una acción: «Lavado automático con bomba 1/3, 60 s» si es del dispositivo; si no, su tipo. */
@Composable
fun CorrectiveActionRecord.description(): String {
    val number = flushNumber
    val max = flushMax
    val seconds = durationSeconds
    return if (isAutomatic && number != null && max != null && seconds != null) {
        stringResource(R.string.contingency_flush_action, number, max, seconds)
    } else {
        type.label()
    }
}

/** Etiqueta discreta que distingue lo que hizo el dispositivo de lo que hizo una persona. */
@Composable
fun AutomaticTag(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Emerald50, RoundedCornerShape(50))
            .border(BorderStroke(1.dp, OliveLine), RoundedCornerShape(50))
            .padding(start = 8.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
        Text(
            stringResource(R.string.contingency_tag),
            style = MaterialTheme.typography.labelSmall,
            color = Emerald800,
        )
    }
}

/**
 * Estado de la contingencia del dispositivo: activa o con los lavados agotados, los lavados
 * usados (puntos y X/3) y las acciones registradas, con la etiqueta «Lavado automático».
 */
@Composable
fun ContingencyStatus(contingency: SalinityContingency, modifier: Modifier = Modifier) {
    val exhausted = contingency.state == ContingencyState.EXHAUSTED
    val stateLabel = stringResource(
        if (exhausted) R.string.contingency_state_exhausted else R.string.contingency_state_active,
    )
    val summary = stringResource(R.string.contingency_cd, stateLabel, contingency.flushesUsed, contingency.maxFlushes)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Emerald50, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, OliveLine), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier.clearAndSetSemantics { contentDescription = summary },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.contingency_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Emerald800,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlushDots(used = contingency.flushesUsed, max = contingency.maxFlushes)
                Text(
                    stringResource(R.string.contingency_flushes_used, contingency.flushesUsed, contingency.maxFlushes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SageInk,
                )
            }
            Text(
                stateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (exhausted) SalinityCritical else Emerald800,
            )
        }
        if (exhausted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SalinityCriticalContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = SalinityCritical, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.contingency_exhausted_note, contingency.maxFlushes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SalinityCritical,
                )
            }
        } else {
            Text(
                stringResource(R.string.contingency_active_hint),
                style = MaterialTheme.typography.bodySmall,
                color = SageInk,
            )
        }
        if (contingency.actions.isNotEmpty()) {
            Text(
                stringResource(R.string.contingency_actions_heading),
                style = MaterialTheme.typography.labelLarge,
                color = SageInk,
                modifier = Modifier.padding(top = 2.dp),
            )
            contingency.actions.forEach { ActionRow(it) }
        }
    }
}

@Composable
private fun ActionRow(action: CorrectiveActionRecord) {
    Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(action.description(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (action.isAutomatic) AutomaticTag()
            Text(action.performedAt, style = MaterialTheme.typography.bodySmall, color = SageInk)
        }
    }
}

/** Un punto por lavado: lleno si ya se usó. El color no va solo: el texto X/3 va al lado. */
@Composable
private fun FlushDots(used: Int, max: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(max) { index ->
            Row(
                Modifier
                    .size(12.dp)
                    .background(if (index < used) Emerald700 else Track, CircleShape),
            ) {}
        }
    }
}
