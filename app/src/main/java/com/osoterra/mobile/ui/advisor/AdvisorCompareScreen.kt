package com.osoterra.mobile.ui.advisor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer

private val seriesColors = listOf(
    Color(0xFF2E6E4E),
    Color(0xFF1565C0),
    Color(0xFFEF6C00),
    Color(0xFF6A1B9A),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisorCompareScreen(
    plotIds: List<String>,
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: AdvisorCompareViewModel =
        viewModel(factory = AdvisorCompareViewModel.provideFactory(container, plotIds))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.advisor_compare_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.advisor_compare_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    MultiSeriesChart(state.series)
                    Spacer(Modifier.height(20.dp))
                    state.series.forEachIndexed { index, s ->
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .background(
                                        seriesColors[index % seriesColors.size],
                                        RoundedCornerShape(4.dp),
                                    ),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(s.plotName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSeriesChart(series: List<PlotSeries>) {
    val allValues = series.flatMap { s -> s.readings.map { it.ecDsPerM } }
    val maxV = allValues.maxOrNull() ?: 1.0
    val minV = allValues.minOrNull() ?: 0.0
    val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0

    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            series.forEachIndexed { index, s ->
                val values = s.readings.map { it.ecDsPerM }
                if (values.size < 2) return@forEachIndexed
                val stepX = size.width / (values.size - 1)
                val color = seriesColors[index % seriesColors.size]
                val points = values.mapIndexed { i, v ->
                    val x = stepX * i
                    val y = size.height - ((v - minV) / range).toFloat() * size.height
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(color = color, start = points[i], end = points[i + 1], strokeWidth = 5f)
                }
            }
        }
    }
}
