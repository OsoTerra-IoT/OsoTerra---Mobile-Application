package com.osoterra.mobile.ui.alerts

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.domain.model.Alert
import com.osoterra.mobile.domain.model.AlertSeverity
import com.osoterra.mobile.ui.common.dsPerM
import com.osoterra.mobile.ui.common.label
import com.osoterra.mobile.ui.theme.Emerald700

/** Qué hacer ante cada nivel, en palabras simples (US33). Igual que en la Web App. */
@StringRes
fun AlertSeverity.recommendation(): Int = when (this) {
    AlertSeverity.WATCH -> R.string.alerts_recommendation_watch
    AlertSeverity.HIGH -> R.string.alerts_recommendation_high
    AlertSeverity.CRITICAL -> R.string.alerts_recommendation_critical
}

/**
 * «Qué hacer» y el botón para compartir la alerta (US34). Usa el menú de compartir de Android,
 * donde el productor elige WhatsApp y el contacto: la app no guarda números de teléfono.
 */
@Composable
fun AlertGuidance(alert: Alert, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val action = stringResource(alert.severity.recommendation())
    val reading = alert.observedDsPerM
    val threshold = alert.thresholdDsPerM
    val readingText = if (reading != null && threshold != null) {
        stringResource(R.string.alerts_share_reading, reading.dsPerM(), threshold.dsPerM())
    } else {
        ""
    }
    val shareText = stringResource(
        R.string.alerts_share_text,
        alert.severity.label(),
        alert.plotName,
        alert.createdAt,
        readingText,
        action,
    )
    val chooserTitle = stringResource(R.string.alerts_share)
    Column(modifier) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.alerts_what_to_do))
                }
                append(" ")
                append(action)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, chooserTitle))
            },
            modifier = Modifier
                .padding(top = 4.dp)
                .heightIn(min = 48.dp),
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(chooserTitle, color = Emerald700, style = MaterialTheme.typography.labelLarge)
        }
    }
}
