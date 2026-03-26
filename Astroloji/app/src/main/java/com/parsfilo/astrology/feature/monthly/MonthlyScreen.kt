package com.parsfilo.astrology.feature.monthly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parsfilo.astrology.R
import com.parsfilo.astrology.core.ui.components.AstrologyCard
import com.parsfilo.astrology.core.ui.components.ErrorState
import com.parsfilo.astrology.core.ui.components.LoadingState

@Composable
fun MonthlyScreen(
    sign: String,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonthlyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    if (uiState.isLoading) {
        LoadingState()
        return
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        uiState.monthly?.let { monthly ->
            AstrologyCard {
                Text(stringResource(R.string.monthly_label_month, monthly.month))
                Text(monthly.summary ?: stringResource(R.string.monthly_premium_locked))
                if (monthly.summary == null) {
                    Button(onClick = onOpenPremium) { Text(stringResource(R.string.common_open_premium)) }
                }
            }
            AstrologyCard {
                Text(stringResource(R.string.monthly_calendar_title))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..30).forEach { day ->
                        Text(day.toString(), modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
        uiState.error?.let { ErrorState(message = it, onRetry = {}) }
    }
}
