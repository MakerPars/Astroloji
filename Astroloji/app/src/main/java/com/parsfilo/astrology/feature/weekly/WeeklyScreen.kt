package com.parsfilo.astrology.feature.weekly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun WeeklyScreen(
    sign: String,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
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
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            listOf(
                stringResource(R.string.weekly_tab_overview),
                stringResource(R.string.weekly_tab_love),
                stringResource(R.string.weekly_tab_career),
                stringResource(R.string.weekly_tab_money),
            ).forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
            }
        }
        uiState.weekly?.let { weekly ->
            AstrologyCard {
                Text(stringResource(R.string.weekly_label_week, weekly.week))
                Text(stringResource(R.string.weekly_best_day, weekly.bestDay ?: "-"))
                val content =
                    when (selectedTab) {
                        0 -> weekly.summary
                        1 -> weekly.love
                        2 -> weekly.career
                        else -> weekly.money
                    }
                Text(content ?: stringResource(R.string.weekly_premium_locked))
                if (selectedTab > 0 && content == null) {
                    Button(onClick = onOpenPremium) { Text(stringResource(R.string.common_open_premium)) }
                }
                weekly.warning?.let { Text(stringResource(R.string.weekly_warning, it)) }
            }
        }
        uiState.error?.let { ErrorState(message = it, onRetry = {}) }
    }
}
