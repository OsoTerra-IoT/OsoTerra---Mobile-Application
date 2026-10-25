package com.osoterra.mobile.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.ui.theme.Emerald100
import com.osoterra.mobile.ui.theme.Emerald600
import com.osoterra.mobile.ui.theme.Emerald700

/**
 * Tarjeta de carga con la forma aproximada del contenido, para que nada salte al llegar.
 * Los bloques brillan (shimmer) mientras carga; TalkBack anuncia «Cargando» una vez.
 */
@Composable
fun SkeletonCard(height: Dp = 150.dp) {
    val loading = stringResource(R.string.common_loading)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { contentDescription = loading },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val block = RoundedCornerShape(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.fillMaxWidth(0.5f).height(20.dp).shimmer(block))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(width = 56.dp, height = 28.dp).shimmer(block))
            }
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).shimmer(block))
            Box(Modifier.fillMaxWidth().height(12.dp).shimmer(RoundedCornerShape(50)))
            Box(Modifier.size(width = 96.dp, height = 26.dp).shimmer(RoundedCornerShape(50)))
        }
    }
}

/** Estado vacío: dice qué pasa y, si hay algo que hacer, ofrece la acción. */
@Composable
fun EmptyCard(
    title: String,
    hint: String,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            if (icon != null) {
                // Ícono decorativo en círculo verde claro (guía: 48 dp); el texto da el mensaje.
                Surface(shape = CircleShape, color = Emerald100, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Emerald600, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(actionLabel) }
            }
        }
    }
}

/** Error de carga anunciado por el lector de pantalla, con reintento. */
@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
                Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.common_retry)) }
        }
    }
}

/** Encabezado de pantalla de nivel superior: rótulo en esmeralda y titular grande. */
@Composable
fun ScreenHeader(eyebrow: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(eyebrow, style = MaterialTheme.typography.titleMedium, color = Emerald700)
        val reducedMotion = rememberReducedMotion()
        // El titular cambia con un fundido y la altura se ajusta sin saltos.
        Crossfade(
            targetState = title,
            animationSpec = motionTween(OsoMotion.ENTER, reducedMotion),
            modifier = Modifier.animateContentSize(motionTween(OsoMotion.STATE, reducedMotion)),
            label = "headline",
        ) { text ->
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 8.dp)
                        .semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
