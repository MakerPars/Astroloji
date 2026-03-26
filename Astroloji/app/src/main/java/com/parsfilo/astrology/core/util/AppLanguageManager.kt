package com.parsfilo.astrology.core.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import com.parsfilo.astrology.core.data.preferences.userPreferencesDataStore
import kotlinx.coroutines.flow.first

object AppLanguageManager {
    private val languageKey = stringPreferencesKey("language")

    suspend fun applyStoredLanguage(context: Context) {
        val preferences = context.userPreferencesDataStore.data.first()
        applyLanguage(context, preferences[languageKey])
    }

    fun applyLanguage(language: String?) {
        applyCompatLocales(language)
    }

    fun applyLanguage(
        context: Context,
        language: String?,
    ) {
        val previousTags = currentLanguageTags(context)
        applyFrameworkLocales(context, language)
        val newLocales = applyCompatLocales(language)
        if (previousTags != currentLanguageTags(context) || AppCompatDelegate.getApplicationLocales() != newLocales) {
            context.findActivity()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.recreate()
                }
            }
        }
    }

    private fun applyCompatLocales(language: String?): LocaleListCompat {
        val locales =
            if (language.isNullOrBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language)
            }

        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
        return locales
    }

    private fun applyFrameworkLocales(
        context: Context,
        language: String?,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        val locales =
            if (language.isNullOrBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language)
            }
        if (localeManager.applicationLocales.toLanguageTags() != locales.toLanguageTags()) {
            localeManager.applicationLocales = locales
        }
    }

    private fun currentLanguageTags(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            return localeManager?.applicationLocales?.toLanguageTags().orEmpty()
        }
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }

    private tailrec fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
}
