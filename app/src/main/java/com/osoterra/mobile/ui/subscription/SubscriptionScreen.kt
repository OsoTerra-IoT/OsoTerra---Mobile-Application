package com.osoterra.mobile.ui.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osoterra.mobile.R
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.BillingCycle
import com.osoterra.mobile.domain.model.Subscription
import com.osoterra.mobile.domain.model.SubscriptionOverview
import com.osoterra.mobile.domain.model.SubscriptionPlan
import com.osoterra.mobile.ui.common.ErrorCard
import com.osoterra.mobile.ui.common.SerenaTopBar
import com.osoterra.mobile.ui.common.SkeletonCard
import com.osoterra.mobile.ui.common.rememberCanWrite
import com.osoterra.mobile.ui.theme.OliveLine
import com.osoterra.mobile.ui.theme.SurfaceGreen

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: SubscriptionViewModel = viewModel(factory = SubscriptionViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Solo el productor contrata o cancela; el asesor únicamente consulta.
    val canWrite = rememberCanWrite()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            SerenaTopBar(title = stringResource(R.string.subscription_title), onBack = onBack)
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val overview = state.overview
            when {
                state.isLoading -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonCard()
                    SkeletonCard()
                }
                overview == null -> ErrorCard(
                    message = state.loadError ?: stringResource(R.string.subscription_load_error),
                    onRetry = viewModel::load,
                    modifier = Modifier.padding(20.dp),
                )
                else -> SubscriptionContent(
                    overview = overview,
                    state = state,
                    canWrite = canWrite,
                    onChange = viewModel::askChange,
                    onCancel = viewModel::askCancel,
                    onRetryAction = viewModel::retryAction,
                    onDismissNotice = viewModel::dismissNotice,
                    onDismissActionError = viewModel::dismissActionError,
                )
            }
        }
    }

    state.confirming?.let { action ->
        ConfirmDialog(
            action = action,
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismissConfirmation,
        )
    }
}

@Composable
private fun SubscriptionContent(
    overview: SubscriptionOverview,
    state: SubscriptionUiState,
    canWrite: Boolean,
    onChange: (SubscriptionPlan) -> Unit,
    onCancel: (Subscription) -> Unit,
    onRetryAction: () -> Unit,
    onDismissNotice: () -> Unit,
    onDismissActionError: () -> Unit,
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.notice?.let { notice ->
            item { NoticeCard(notice, onDismissNotice) }
        }
        state.actionError?.let { error ->
            item {
                Column {
                    ErrorCard(message = error, onRetry = onRetryAction)
                    TextButton(onClick = onDismissActionError) { Text(stringResource(R.string.common_cancel)) }
                }
            }
        }
        item { CurrentPlanCard(overview.current, canWrite, state.isWorking, onCancel) }
        overview.pending?.let { pending ->
            item { PendingCard(pending, canWrite, state.isWorking, onCancel) }
        }
        item {
            Text(
                stringResource(R.string.subscription_plans_heading),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(overview.plans, key = { it.id }) { plan ->
            PlanCard(
                plan = plan,
                isCurrent = overview.current?.planId == plan.id,
                hasCurrent = overview.current != null,
                canWrite = canWrite,
                isWorking = state.isWorking,
                onChange = { onChange(plan) },
            )
        }
        item {
            Text(
                stringResource(R.string.subscription_payment_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CurrentPlanCard(
    current: Subscription?,
    canWrite: Boolean,
    isWorking: Boolean,
    onCancel: (Subscription) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGreen),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, OliveLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.subscription_current_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            if (current == null) {
                Text(
                    stringResource(R.string.subscription_none_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.subscription_none_hint), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            Text(
                current.planName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (current.isFree) {
                    stringResource(R.string.subscription_free)
                } else {
                    current.renewsAt?.let { stringResource(R.string.subscription_renews, it) } ?: ""
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.subscription_plots_used, current.plotsUsed, current.plotsAllowed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            val fraction = if (current.plotsAllowed > 0) current.plotsUsed.toFloat() / current.plotsAllowed else 0f
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
            )
            if (current.plotsUsed >= current.plotsAllowed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.subscription_limit_reached),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (canWrite) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onCancel(current) },
                    enabled = !isWorking,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.subscription_cancel)) }
            }
        }
    }
}

@Composable
private fun PendingCard(
    pending: Subscription,
    canWrite: Boolean,
    isWorking: Boolean,
    onCancel: (Subscription) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OliveLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.subscription_pending_title, pending.planName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.subscription_pending_hint), style = MaterialTheme.typography.bodyMedium)
            if (canWrite) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onCancel(pending) },
                    enabled = !isWorking,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.subscription_cancel_request)) }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrent: Boolean,
    hasCurrent: Boolean,
    canWrite: Boolean,
    isWorking: Boolean,
    onChange: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else OliveLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(planPrice(plan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.subscription_plan_max_plots, plan.maxPlots),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isCurrent) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.subscription_plan_current),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            } else if (canWrite) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onChange,
                    enabled = !isWorking,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            stringResource(
                                if (hasCurrent) R.string.subscription_action_change else R.string.subscription_action_subscribe,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun planPrice(plan: SubscriptionPlan): String {
    if (plan.isFree) return stringResource(R.string.subscription_price_free)
    val money = formatMoney(plan.priceAmount, plan.priceCurrency)
    return when (plan.billingCycle) {
        BillingCycle.MONTHLY -> stringResource(R.string.subscription_price_monthly, money)
        BillingCycle.ANNUAL -> stringResource(R.string.subscription_price_annual, money)
        BillingCycle.NONE -> money
    }
}

@Composable
private fun NoticeCard(notice: SubscriptionNotice, onDismiss: () -> Unit) {
    val text = when (notice.kind) {
        SubscriptionNotice.Kind.ACTIVATED -> stringResource(R.string.subscription_notice_activated, notice.planName)
        SubscriptionNotice.Kind.PENDING_PAYMENT -> stringResource(R.string.subscription_notice_pending, notice.planName)
        SubscriptionNotice.Kind.CANCELLED -> stringResource(R.string.subscription_notice_cancelled, notice.planName)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.subscription_notice_close))
            }
        }
    }
}

@Composable
private fun ConfirmDialog(action: SubscriptionAction, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title: String
    val message: String
    when (action) {
        is SubscriptionAction.Change -> {
            title = stringResource(
                if (action.replacesCurrent) R.string.subscription_confirm_change_title else R.string.subscription_confirm_subscribe_title,
                action.plan.name,
            )
            message = when {
                !action.plan.isFree -> stringResource(R.string.subscription_confirm_paid_message)
                action.replacesCurrent -> stringResource(R.string.subscription_confirm_free_replace_message)
                else -> stringResource(R.string.subscription_confirm_free_message)
            }
        }
        is SubscriptionAction.Cancel -> {
            title = stringResource(R.string.subscription_confirm_cancel_title, action.subscription.planName)
            message = stringResource(R.string.subscription_confirm_cancel_message)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.common_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
