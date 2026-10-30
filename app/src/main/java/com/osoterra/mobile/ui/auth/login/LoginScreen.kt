package com.osoterra.mobile.ui.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.ui.common.rememberReducedMotion
import com.osoterra.mobile.ui.demo.DemoLoginPanel
import kotlinx.coroutines.launch
import com.osoterra.mobile.ui.theme.Emerald100
import com.osoterra.mobile.ui.theme.Emerald600
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.Emerald800
import com.osoterra.mobile.ui.theme.Emerald900
import com.osoterra.mobile.ui.theme.Ink
import com.osoterra.mobile.ui.theme.SurfaceGreen
import com.osoterra.mobile.ui.theme.White

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    onDemoStarted: () -> Unit,
) {
    val container = rememberAppContainer()
    val demoMode by container.demoMode.collectAsState()
    val demoScenario by container.demoScenario.collectAsState()
    var demoBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // La clave cambia con el modo: el ViewModel toma los repositorios del modo activo.
    val viewModel: LoginViewModel = viewModel(
        key = if (demoMode) "login-demo" else "login",
        factory = LoginViewModel.provideFactory(container),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val entrance = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) onLoginSuccess()
    }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }
    LightStatusIcons()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Emerald800)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Hero(reducedMotion = reducedMotion, onLogoLongPress = container::enableDemoMode)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 120.dp.toPx()
                },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                Text(
                    text = stringResource(R.string.login_button),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.login_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                LoginForm(
                    state = state,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = viewModel::login,
                )

                TextButton(
                    onClick = onNavigateForgot,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.login_forgot), style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigateRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.login_register), style = MaterialTheme.typography.labelLarge)
                }

                if (container.usesFakeApi && !demoMode) {
                    Spacer(Modifier.height(20.dp))
                    DemoNote()
                }

                // Modo de prueba para las entrevistas de usabilidad: escenarios sin Backend.
                Spacer(Modifier.height(16.dp))
                if (demoMode) {
                    DemoLoginPanel(
                        selected = demoScenario,
                        busy = demoBusy,
                        onSelect = { scenario ->
                            demoBusy = true
                            scope.launch {
                                container.startDemoScenario(scenario)
                                demoBusy = false
                                onDemoStarted()
                            }
                        },
                        onExit = { scope.launch { container.disableDemoMode() } },
                    )
                } else {
                    TextButton(
                        onClick = container::enableDemoMode,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(R.string.demo_mode_link),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = White,
        unfocusedContainerColor = White,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    val fieldShape = RoundedCornerShape(16.dp)

    OutlinedTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.login_email)) },
        leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
        singleLine = true,
        isError = state.errorMessage != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        shape = fieldShape,
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(R.string.login_password)) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                    ),
                )
            }
        },
        singleLine = true,
        isError = state.errorMessage != null,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        shape = fieldShape,
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth(),
    )

    AnimatedVisibility(
        visible = state.errorMessage != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onSubmit,
        enabled = !state.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(stringResource(R.string.login_button), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Titular de la marca y una sonda de suelo que marca su lectura con un leve vaivén. */
@Composable
private fun Hero(reducedMotion: Boolean, onLogoLongPress: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "probe")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val drift = if (reducedMotion) 0.5f else bob

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(320.dp),
    ) {
        SoilLayers(modifier = Modifier.align(Alignment.BottomCenter))

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val longPressLabel = stringResource(R.string.demo_logo_long_press)
                Image(
                    painter = painterResource(R.drawable.osoterra_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        // Atajo discreto para quien conduce la entrevista: mantener pulsado activa el modo de prueba.
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onLogoLongPress,
                            onLongClickLabel = longPressLabel,
                            indication = null,
                            interactionSource = null,
                        ),
                )
                Spacer(Modifier.size(10.dp))
                Text("OsoSense", style = MaterialTheme.typography.titleLarge, color = White)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.login_headline),
                style = MaterialTheme.typography.headlineLarge,
                color = White,
            )
        }

        // Lectura flotante de la sonda.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 52.dp)
                .offset(y = (-6 * drift).dp),
            shape = RoundedCornerShape(50),
            color = White,
            shadowElevation = 4.dp,
        ) {
            Text(
                text = stringResource(R.string.login_probe_reading),
                style = MaterialTheme.typography.labelLarge,
                color = Ink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SoilLayers(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val layers = listOf(Emerald600, Emerald700, Emerald900)
        layers.forEachIndexed { index, color ->
            val top = size.height * (0.15f + index * 0.28f)
            drawRoundRect(
                color = color.copy(alpha = 0.55f + index * 0.15f),
                topLeft = Offset(-40f, top),
                size = Size(size.width + 80f, size.height),
                cornerRadius = CornerRadius(90f, 90f),
            )
        }
        // Sonda: varilla clavada en el suelo con su cabezal.
        val x = size.width * 0.78f
        drawRoundRect(
            color = Emerald100,
            topLeft = Offset(x - 3.dp.toPx(), -8.dp.toPx()),
            size = Size(6.dp.toPx(), size.height * 0.9f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )
        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, -8.dp.toPx()))
    }
}

/** Íconos claros en la barra de estado mientras se ve el panel esmeralda. */
@Composable
private fun LightStatusIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = true }
    }
}

@Composable
private fun DemoNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGreen, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Emerald700)
        Text(
            text = stringResource(R.string.login_demo_note),
            style = MaterialTheme.typography.bodyMedium,
            color = Emerald900,
        )
    }
}
