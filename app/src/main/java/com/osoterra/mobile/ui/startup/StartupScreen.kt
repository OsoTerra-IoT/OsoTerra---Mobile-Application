package com.osoterra.mobile.ui.startup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.theme.Emerald600
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StartupScreen(
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val container = rememberAppContainer()
    val reducedMotion = rememberReducedMotion()
    val entrance = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        launch { entrance.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing)) }
        val user = container.bootstrapSession()
        // Deja ver la marca un instante sin retrasar a quien ya tiene sesión.
        if (!reducedMotion) delay(700)
        if (user != null) onSignedIn() else onSignedOut()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = entrance.value
                    val scale = 0.92f + 0.08f * entrance.value
                    scaleX = scale
                    scaleY = scale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.osoterra_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    append("Oso")
                    withStyle(SpanStyle(color = Emerald700)) { append("Sense") }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.startup_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LoadingStrip(
            reducedMotion = reducedMotion,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp),
        )
    }
}

/** Indicador de carga con la forma de la franja de salinidad. */
@Composable
private fun LoadingStrip(reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val description = stringResource(R.string.startup_loading)
    Canvas(
        modifier = modifier
            .width(120.dp)
            .height(6.dp)
            .semantics { contentDescription = description },
    ) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = Track, cornerRadius = radius)
        val fraction = if (reducedMotion) 0.5f else sweep
        drawRoundRect(
            color = Emerald600,
            size = Size(size.width * fraction, size.height),
            cornerRadius = radius,
        )
    }
}
