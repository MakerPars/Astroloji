package com.parsfilo.astrology.feature.personality

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun PersonalityScreen(
    sign: String,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalityViewModel = hiltViewModel(),
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
        uiState.report?.let { report ->
            AstrologyCard {
                Text(report.summary)
                Text("${report.element} • ${report.planet} • ${report.color}")
            }
            AstrologyCard {
                Text(stringResource(R.string.personality_strengths_title))
                report.strengths.forEach { Text("• $it") }
                Text(stringResource(R.string.personality_ideal_partners, report.idealPartners.joinToString()))
            }
            AstrologyCard {
                if (report.deepAnalysis == null) {
                    Text(stringResource(R.string.personality_premium_locked))
                    Button(onClick = onOpenPremium) { Text(stringResource(R.string.common_open_premium)) }
                } else {
                    Text(report.deepAnalysis)
                    report.weaknesses.forEach { Text("• $it") }
                    report.careerFit.forEach { Text("• $it") }
                }
            }
        }
        uiState.error?.let { ErrorState(message = it, onRetry = {}) }
    }
}
