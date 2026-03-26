package com.parsfilo.astrology.feature.compatibility

import androidx.lifecycle.viewModelScope
import com.parsfilo.astrology.core.data.preferences.UserPreferencesRepository
import com.parsfilo.astrology.core.data.repository.AnalyticsEvents
import com.parsfilo.astrology.core.data.repository.AnalyticsRepository
import com.parsfilo.astrology.core.data.repository.ContentRepository
import com.parsfilo.astrology.core.domain.model.CompatibilityReport
import com.parsfilo.astrology.core.ui.MviViewModel
import com.parsfilo.astrology.core.util.AppResult
import com.parsfilo.astrology.core.util.ZodiacSign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompatibilityUiState(
    val isLoading: Boolean = false,
    val mySign: String = ZodiacSign.ARIES.key,
    val selectedSign: String = ZodiacSign.LEO.key,
    val report: CompatibilityReport? = null,
    val error: String? = null,
)

sealed interface CompatibilityUiEvent {
    data class SelectSign(
        val sign: String,
    ) : CompatibilityUiEvent

    data object Load : CompatibilityUiEvent
}

@HiltViewModel
class CompatibilityViewModel
    @Inject
    constructor(
        private val preferencesRepository: UserPreferencesRepository,
        private val contentRepository: ContentRepository,
        private val analyticsRepository: AnalyticsRepository,
    ) : MviViewModel<CompatibilityUiState, CompatibilityUiEvent, Unit>(CompatibilityUiState()) {
        init {
            viewModelScope.launch {
                val prefs = preferencesRepository.current()
                setState { copy(mySign = prefs.selectedSign) }
                onEvent(CompatibilityUiEvent.Load)
            }
        }

        override fun onEvent(event: CompatibilityUiEvent) {
            when (event) {
                is CompatibilityUiEvent.SelectSign -> {
                    setState { copy(selectedSign = event.sign) }
                    onEvent(CompatibilityUiEvent.Load)
                }
                CompatibilityUiEvent.Load -> {
                    viewModelScope.launch {
                        val prefs = preferencesRepository.current()
                        setState { copy(isLoading = true, error = null) }
                        analyticsRepository.track(
                            AnalyticsEvents.COMPAT_CHECKED,
                            mapOf("sign1" to state.value.mySign, "sign2" to state.value.selectedSign),
                        )
                        when (
                            val result =
                                contentRepository.getCompatibility(
                                    state.value.mySign,
                                    state.value.selectedSign,
                                    prefs.language,
                                )
                        ) {
                            is AppResult.Success -> setState { copy(isLoading = false, report = result.data) }
                            is AppResult.Error -> setState { copy(isLoading = false, error = result.exception.message) }
                            AppResult.Loading -> Unit
                        }
                    }
                }
            }
        }
    }
