package com.parsfilo.astrology.feature.premium

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.parsfilo.astrology.core.data.repository.AnalyticsEvents
import com.parsfilo.astrology.core.data.repository.AnalyticsRepository
import com.parsfilo.astrology.core.data.repository.BillingManager
import com.parsfilo.astrology.core.data.repository.PremiumPlanUi
import com.parsfilo.astrology.core.data.repository.RemoteConfigRepository
import com.parsfilo.astrology.core.ui.MviViewModel
import com.parsfilo.astrology.core.util.AppResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isLoading: Boolean = true,
    val plans: List<PremiumPlanUi> = emptyList(),
    val selectedProductId: String = "premium_yearly",
    val trialDays: Int = 0,
    val error: String? = null,
    val purchaseSuccess: Boolean = false,
)

sealed interface PremiumUiEvent {
    data class SelectPlan(
        val productId: String,
    ) : PremiumUiEvent

    data class Purchase(
        val activity: Activity,
    ) : PremiumUiEvent

    data object Restore : PremiumUiEvent
}

@HiltViewModel
class PremiumViewModel
    @Inject
    constructor(
        private val billingManager: BillingManager,
        private val analyticsRepository: AnalyticsRepository,
        private val remoteConfigRepository: RemoteConfigRepository,
    ) : MviViewModel<PremiumUiState, PremiumUiEvent, Unit>(PremiumUiState()) {
        init {
            viewModelScope.launch {
                analyticsRepository.track(AnalyticsEvents.PREMIUM_SCREEN_VIEWED)
                val flags = remoteConfigRepository.fetchFlags()
                billingManager.loadPlans()
                setState { copy(isLoading = false, plans = billingManager.plans.value, trialDays = flags.premiumTrialDays) }
            }
            viewModelScope.launch {
                billingManager.purchaseState.collectLatest { purchaseState ->
                    when (purchaseState) {
                        is AppResult.Success -> setState { copy(purchaseSuccess = true, error = null) }
                        is AppResult.Error -> setState { copy(error = purchaseState.exception.message) }
                        AppResult.Loading, null -> Unit
                    }
                }
            }
        }

        override fun onEvent(event: PremiumUiEvent) {
            when (event) {
                is PremiumUiEvent.SelectPlan -> setState { copy(selectedProductId = event.productId) }
                is PremiumUiEvent.Purchase -> billingManager.launchPurchase(event.activity, state.value.selectedProductId)
                PremiumUiEvent.Restore -> viewModelScope.launch { billingManager.restorePurchases() }
            }
        }
    }
