package com.parsfilo.astrology.feature.monthly

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.parsfilo.astrology.core.data.preferences.UserPreferencesRepository
import com.parsfilo.astrology.core.data.repository.AnalyticsEvents
import com.parsfilo.astrology.core.data.repository.AnalyticsRepository
import com.parsfilo.astrology.core.data.repository.ContentRepository
import com.parsfilo.astrology.core.domain.model.MonthlyHoroscope
import com.parsfilo.astrology.core.ui.MviViewModel
import com.parsfilo.astrology.core.util.AppResult
import com.parsfilo.astrology.core.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthlyUiState(
    val isLoading: Boolean = true,
    val monthly: MonthlyHoroscope? = null,
    val error: String? = null,
)

@HiltViewModel
class MonthlyViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val contentRepository: ContentRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val analyticsRepository: AnalyticsRepository,
    ) : MviViewModel<MonthlyUiState, Unit, Unit>(MonthlyUiState()) {
        private val signFromArgs: String? = savedStateHandle.get<String>("sign")

        init {
            viewModelScope.launch {
                val sign = signFromArgs ?: preferencesRepository.current().selectedSign
                val prefs = preferencesRepository.current()
                analyticsRepository.track(AnalyticsEvents.MONTHLY_VIEWED, mapOf("sign" to sign))
                when (val result = contentRepository.getMonthly(sign, prefs.language, TimeUtils.monthIdentifier())) {
                    is AppResult.Success -> setState { copy(isLoading = false, monthly = result.data) }
                    is AppResult.Error -> setState { copy(isLoading = false, error = result.exception.message) }
                    AppResult.Loading -> Unit
                }
            }
        }

        override fun onEvent(event: Unit) = Unit
    }
