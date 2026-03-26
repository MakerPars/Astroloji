package com.parsfilo.astrology

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.parsfilo.astrology.core.ads.AdsInitializer
import com.parsfilo.astrology.core.ads.AppOpenAdManager
import com.parsfilo.astrology.core.ads.GoogleMobileAdsConsentManager
import com.parsfilo.astrology.core.ads.InterstitialAdManager
import com.parsfilo.astrology.core.ads.NativeAdvancedAdManager
import com.parsfilo.astrology.core.ads.RewardedAdManager
import com.parsfilo.astrology.core.ads.RewardedInterstitialAdManager
import com.parsfilo.astrology.core.data.preferences.UserPreferencesRepository
import com.parsfilo.astrology.core.util.AppLanguageManager
import com.parsfilo.astrology.navigation.AstrologyAppRoot
import com.parsfilo.astrology.ui.theme.AstrolojiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var consentManager: GoogleMobileAdsConsentManager

    @Inject
    lateinit var adsInitializer: AdsInitializer

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    @Inject
    lateinit var rewardedAdManager: RewardedAdManager

    @Inject
    lateinit var rewardedInterstitialAdManager: RewardedInterstitialAdManager

    @Inject
    lateinit var nativeAdvancedAdManager: NativeAdvancedAdManager

    private var skipNextAppOpen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        runBlocking {
            AppLanguageManager.applyStoredLanguage(this@MainActivity)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            consentManager.gatherConsent(this@MainActivity)
            preferencesRepository.updateConsentStatus(consentManager.consentStatus)
            adsInitializer.initializeIfNeeded()
            preloadAdStack()
        }
        setContent {
            val preferences =
                preferencesRepository.preferences
                    .collectAsStateWithLifecycle(
                        initialValue = preferencesRepository.emptyPreferences(),
                    ).value
            val darkTheme =
                when (preferences.theme) {
                    "light" -> false
                    "system" -> isSystemInDarkTheme()
                    else -> true
                }
            AstrolojiTheme(darkTheme = darkTheme) {
                AstrologyAppRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val preferences = preferencesRepository.current()
            if (skipNextAppOpen) {
                skipNextAppOpen = false
                return@launch
            }
            if (!BuildConfig.DEBUG && preferences.onboardingCompleted && !preferences.isPremium && consentManager.canRequestAds) {
                appOpenAdManager.showIfAvailable(this@MainActivity)
            }
        }
    }

    private fun preloadAdStack() {
        appOpenAdManager.preload()
        interstitialAdManager.preload()
        rewardedAdManager.preload()
        rewardedInterstitialAdManager.preload()
        nativeAdvancedAdManager.preload()
    }
}
