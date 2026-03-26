package com.parsfilo.astrology.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.parsfilo.astrology.core.data.preferences.UserPreferencesRepository
import com.parsfilo.astrology.core.data.remote.AstrologyApi
import com.parsfilo.astrology.core.data.remote.UpdateUserRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class TokenRefreshWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val api: AstrologyApi,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            val jwt = userPreferencesRepository.getJwt().orEmpty()
            if (jwt.isBlank()) return Result.success()
            val token =
                runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
                    ?: return Result.retry()
            val prefs = userPreferencesRepository.current()
            val response =
                api.updateUser(
                    UpdateUserRequest(
                        fcmToken = token,
                        notificationEnabled = prefs.notificationEnabled,
                        notificationHour = prefs.notificationHour,
                    ),
                )
            return if (response.isSuccessful) Result.success() else Result.retry()
        }
    }
