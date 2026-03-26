package com.parsfilo.astrology.core.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.parsfilo.astrology.core.domain.model.RemoteFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

object RemoteConfigKeys {
    const val SHOW_PREMIUM_BANNER = "show_premium_banner"
    const val INTERSTITIAL_FREQUENCY = "interstitial_frequency"
    const val PREMIUM_TRIAL_DAYS = "premium_trial_days"
    const val FORCE_UPDATE_VERSION = "force_update_version"
}

@Singleton
class RemoteConfigRepository
    @Inject
    constructor(
        private val remoteConfig: FirebaseRemoteConfig,
    ) {
        suspend fun fetchFlags(): RemoteFlags =
            withContext(Dispatchers.IO) {
                remoteConfig
                    .setConfigSettingsAsync(
                        FirebaseRemoteConfigSettings
                            .Builder()
                            .setMinimumFetchIntervalInSeconds(3600)
                            .build(),
                    ).await()
                remoteConfig
                    .setDefaultsAsync(
                        mapOf(
                            RemoteConfigKeys.SHOW_PREMIUM_BANNER to true,
                            RemoteConfigKeys.INTERSTITIAL_FREQUENCY to 5L,
                            RemoteConfigKeys.PREMIUM_TRIAL_DAYS to 0L,
                            RemoteConfigKeys.FORCE_UPDATE_VERSION to 0L,
                        ),
                    ).await()
                runCatching { remoteConfig.fetchAndActivate().await() }
                    .onFailure { Timber.w(it, "Remote Config fetch failed, continuing with cached/default flags.") }
                RemoteFlags(
                    showPremiumBanner = remoteConfig.getBoolean(RemoteConfigKeys.SHOW_PREMIUM_BANNER),
                    interstitialFrequency = remoteConfig.getLong(RemoteConfigKeys.INTERSTITIAL_FREQUENCY).toInt(),
                    premiumTrialDays = remoteConfig.getLong(RemoteConfigKeys.PREMIUM_TRIAL_DAYS).toInt(),
                    forceUpdateVersion = remoteConfig.getLong(RemoteConfigKeys.FORCE_UPDATE_VERSION),
                )
            }
    }
