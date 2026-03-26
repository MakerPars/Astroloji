package com.parsfilo.astrology.feature.daily

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.parsfilo.astrology.core.data.preferences.UserPreferencesRepository
import com.parsfilo.astrology.core.data.repository.AnalyticsEvents
import com.parsfilo.astrology.core.data.repository.AnalyticsRepository
import com.parsfilo.astrology.core.data.repository.ContentRepository
import com.parsfilo.astrology.core.domain.model.DailyHoroscope
import com.parsfilo.astrology.core.ui.MviViewModel
import com.parsfilo.astrology.core.util.AppResult
import com.parsfilo.astrology.core.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyUiState(
    val isLoading: Boolean = true,
    val horoscope: DailyHoroscope? = null,
    val error: String? = null,
    val isRefreshing: Boolean = false,
)

sealed interface DailyUiEvent {
    data object Refresh : DailyUiEvent
}

sealed interface DailyUiEffect

@HiltViewModel
class DailyViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val contentRepository: ContentRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val analyticsRepository: AnalyticsRepository,
    ) : MviViewModel<DailyUiState, DailyUiEvent, DailyUiEffect>(DailyUiState()) {
        private val signFromArgs: String? = savedStateHandle.get<String>("sign")

        init {
            viewModelScope.launch {
                load(signFromArgs ?: preferencesRepository.current().selectedSign, false)
            }
        }

        override fun onEvent(event: DailyUiEvent) {
            if (event == DailyUiEvent.Refresh) {
                viewModelScope.launch {
                    load(signFromArgs ?: preferencesRepository.current().selectedSign, true)
                }
            }
        }

        private suspend fun load(
            sign: String,
            refresh: Boolean,
        ) {
            setState { copy(isLoading = !refresh, isRefreshing = refresh, error = null) }
            val prefs = preferencesRepository.current()
            analyticsRepository.track(AnalyticsEvents.DAILY_VIEWED, mapOf("sign" to sign))
            when (
                val result =
                    contentRepository.getDaily(
                        sign = sign,
                        language = prefs.language,
                        date = TimeUtils.dateIdentifier(),
                        forceRefresh = refresh,
                    )
            ) {
                is AppResult.Success -> setState { copy(isLoading = false, isRefreshing = false, horoscope = result.data) }
                is AppResult.Error -> setState { copy(isLoading = false, isRefreshing = false, error = result.exception.message) }
                AppResult.Loading -> Unit
            }
        }
    }
