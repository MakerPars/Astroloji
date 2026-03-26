package com.parsfilo.astrology.feature.weekly

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.parsfilo.astrology.core.data.preferences.UserPreferencesRepository
import com.parsfilo.astrology.core.data.repository.AnalyticsEvents
import com.parsfilo.astrology.core.data.repository.AnalyticsRepository
import com.parsfilo.astrology.core.data.repository.ContentRepository
import com.parsfilo.astrology.core.domain.model.WeeklyHoroscope
import com.parsfilo.astrology.core.ui.MviViewModel
import com.parsfilo.astrology.core.util.AppResult
import com.parsfilo.astrology.core.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyUiState(
    val isLoading: Boolean = true,
    val weekly: WeeklyHoroscope? = null,
    val error: String? = null,
)

@HiltViewModel
class WeeklyViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val contentRepository: ContentRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val analyticsRepository: AnalyticsRepository,
    ) : MviViewModel<WeeklyUiState, Unit, Unit>(WeeklyUiState()) {
        private val signFromArgs: String? = savedStateHandle.get<String>("sign")

        init {
            viewModelScope.launch {
                val sign = signFromArgs ?: preferencesRepository.current().selectedSign
                val prefs = preferencesRepository.current()
                analyticsRepository.track(AnalyticsEvents.WEEKLY_VIEWED, mapOf("sign" to sign))
                when (val result = contentRepository.getWeekly(sign, prefs.language, TimeUtils.weekIdentifier())) {
                    is AppResult.Success -> setState { copy(isLoading = false, weekly = result.data) }
                    is AppResult.Error -> setState { copy(isLoading = false, error = result.exception.message) }
                    AppResult.Loading -> Unit
                }
            }
        }

        override fun onEvent(event: Unit) = Unit
    }
