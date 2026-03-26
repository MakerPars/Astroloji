package com.parsfilo.astrology.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
fun DailyScreen(
    sign: String,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyViewModel = hiltViewModel(),
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
        uiState.horoscope?.let { horoscope ->
            AstrologyCard {
                Text(stringResource(R.string.daily_title), style = MaterialTheme.typography.displayLarge)
                Text(horoscope.short)
                Text(
                    stringResource(
                        R.string.daily_scores_line,
                        horoscope.energy,
                        horoscope.loveScore,
                        horoscope.careerScore,
                    ),
                )
            }
            AstrologyCard {
                Text(horoscope.full ?: stringResource(R.string.daily_premium_locked))
                if (horoscope.full == null) {
                    Button(onClick = onOpenPremium) { Text(stringResource(R.string.common_open_premium)) }
                }
            }
            AstrologyCard {
                Text(horoscope.love ?: stringResource(R.string.daily_love_locked))
                Text(horoscope.career ?: stringResource(R.string.daily_career_locked))
                Text(horoscope.money ?: stringResource(R.string.daily_money_locked))
                Text(horoscope.health ?: stringResource(R.string.daily_health_locked))
                horoscope.dailyTip?.let { Text(it) }
            }
        }
        uiState.error?.let { ErrorState(message = it, onRetry = { viewModel.onEvent(DailyUiEvent.Refresh) }) }
    }
}
