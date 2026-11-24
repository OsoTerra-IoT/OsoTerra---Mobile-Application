package com.osoterra.mobile.ui.plot

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.osoterra.mobile.R
import java.util.Locale

/** Seis decimales (unos 10 cm) bastan para ubicar la parcela; siempre con punto decimal. */
fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

/**
 * Botón «Usar mi ubicación» del registro de parcela (hallazgos 5 y 10 de la validación): pide
 * el permiso de ubicación y llena la latitud y la longitud con la posición del teléfono.
 */
@Composable
fun UseMyLocationButton(onLocation: (latitude: String, longitude: String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    fun locate() {
        locating = true
        message = R.string.plot_locating
        currentLocation(context) { location ->
            locating = false
            if (location == null) {
                message = R.string.plot_location_unavailable
            } else {
                message = R.string.plot_location_found
                onLocation(formatCoordinate(location.latitude), formatCoordinate(location.longitude))
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate() else message = R.string.plot_location_denied
    }

    Column(modifier) {
        OutlinedButton(
            onClick = {
                if (hasLocationPermission(context)) {
                    locate()
                } else {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            },
            enabled = !locating,
            shape = RoundedCornerShape(50),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.plot_use_my_location))
        }
        message?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = if (it == R.string.plot_location_found || it == R.string.plot_locating) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Una sola lectura de la posición. En Android 11 o superior se pide una posición actual; antes,
 * se usa la última conocida del GPS o de la red. Devuelve null si el teléfono no tiene ninguna.
 */
@SuppressLint("MissingPermission")
private fun currentLocation(context: Context, onResult: (Location?) -> Unit) {
    if (!hasLocationPermission(context)) return onResult(null)
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (providers.isEmpty()) return onResult(null)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        manager.getCurrentLocation(providers.first(), null, ContextCompat.getMainExecutor(context)) { location ->
            onResult(location ?: providers.firstNotNullOfOrNull { manager.getLastKnownLocation(it) })
        }
    } else {
        onResult(providers.firstNotNullOfOrNull { manager.getLastKnownLocation(it) })
    }
}
