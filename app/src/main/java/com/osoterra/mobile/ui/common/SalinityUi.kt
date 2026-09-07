package com.osoterra.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.domain.model.SalinityLevel
import com.osoterra.mobile.ui.theme.SalinityCritical
import com.osoterra.mobile.ui.theme.SalinityNoData
import com.osoterra.mobile.ui.theme.SalinityNormal
import com.osoterra.mobile.ui.theme.SalinityWarning
import com.osoterra.mobile.ui.theme.SalinityWatch

fun SalinityLevel.color(): Color = when (this) {
    SalinityLevel.NORMAL -> SalinityNormal
    SalinityLevel.WATCH -> SalinityWatch
    SalinityLevel.WARNING -> SalinityWarning
    SalinityLevel.CRITICAL -> SalinityCritical
    SalinityLevel.NO_DATA -> SalinityNoData
}

@Composable
fun SalinityLevel.label(): String = stringResource(
    when (this) {
        SalinityLevel.NORMAL -> R.string.salinity_normal
        SalinityLevel.WATCH -> R.string.salinity_watch
        SalinityLevel.WARNING -> R.string.salinity_warning
        SalinityLevel.CRITICAL -> R.string.salinity_critical
        SalinityLevel.NO_DATA -> R.string.salinity_no_data
    }
)

fun AlertSeverity.color(): Color = when (this) {
    AlertSeverity.LOW -> SalinityNormal
    AlertSeverity.MEDIUM -> SalinityWatch
    AlertSeverity.HIGH -> SalinityWarning
    AlertSeverity.CRITICAL -> SalinityCritical
}

@Composable
fun SalinityChip(level: SalinityLevel, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(level.color(), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = level.label(),
            color = Color.White,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
