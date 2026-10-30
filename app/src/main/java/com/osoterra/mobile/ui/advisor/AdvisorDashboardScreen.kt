package com.osoterra.mobile.ui.advisor

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.SupervisedPlot
import com.osoterra.mobile.ui.common.SalinityChip
import com.osoterra.mobile.ui.common.SerenaTopBar
import com.osoterra.mobile.ui.common.StaggeredEntrance
import com.osoterra.mobile.ui.common.fmt
import com.osoterra.mobile.ui.theme.Emerald700
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SageInk
import com.osoterra.mobile.ui.theme.White
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisorDashboardScreen(
    onBack: () -> Unit,
    onCompare: (List<String>) -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: AdvisorDashboardViewModel =
        viewModel(factory = AdvisorDashboardViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            SerenaTopBar(
                title = stringResource(R.string.advisor_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.toggleSort() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.advisor_sort),
                            tint = if (state.sortByCriticality) Emerald700 else SageInk,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.canCompare) {
                ExtendedFloatingActionButton(
                    onClick = { onCompare(state.selectedIds.toList()) },
                    shape = RoundedCornerShape(50),
                    containerColor = Emerald700,
                    contentColor = White,
                    text = { Text(stringResource(R.string.advisor_compare_n, state.selectedIds.size)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                )
            }
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
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        FilterDropdown(
                            label = stringResource(R.string.advisor_filter_producer),
                            selected = state.producerFilter,
                            options = state.producers,
                            onSelected = viewModel::setProducerFilter,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterDropdown(
                            label = stringResource(R.string.advisor_filter_crop),
                            selected = state.cropFilter,
                            options = state.crops,
                            onSelected = viewModel::setCropFilter,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.visiblePlots, key = { _, it -> it.id }) { index, plot ->
                            StaggeredEntrance(index, Modifier.animateItem()) {
                            SupervisedPlotCard(
                                plot = plot,
                                selected = plot.id in state.selectedIds,
                                onToggle = { viewModel.toggleSelection(plot.id) },
                            )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupervisedPlotCard(plot: SupervisedPlot, selected: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = White,
        border = BorderStroke(1.dp, if (selected) Emerald700 else OliveLine),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(plot.plotName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${plot.producerName} · ${plot.cropName ?: stringResource(R.string.plot_crop_none)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SageInk,
                )
                SalinityChip(level = plot.salinityLevel, compact = true, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                plot.lastEcDsPerM?.let { "${it.fmt(2)}\u00A0dS/m" } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    selected: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.advisor_filter_all)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected ?: allLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = { onSelected(null); expanded = false },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}
