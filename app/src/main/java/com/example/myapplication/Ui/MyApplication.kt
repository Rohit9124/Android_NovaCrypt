package com.example.myapplication.Ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import java.util.Locale

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedLanguage()
        applySavedTheme()
    }

    /**
     * Applies the saved app theme (Light, Dark, or System default)
     */
    private fun applySavedTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("theme", null)

        if (theme == null) {
            prefs.edit().putString("theme", "light").apply()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    /**
     * Applies the saved app language.
     * Uses AppCompatDelegate.setApplicationLocales on Android 13+,
     * falls back to legacy config for older devices.
     */
    private fun applySavedLanguage() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val languageCode = prefs.getString("language", "en") ?: "en"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Modern way: Android 13+ per-app locales
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        } else {
            // Legacy fallback for older Android
            setLegacyLocale(languageCode)
        }
    }

    /**
     * Legacy locale application for Android 12 and below
     */
    @Suppress("DEPRECATION")
    private fun setLegacyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val res = resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        res.updateConfiguration(config, res.displayMetrics)
    }

    /**
     * Ensures Activities also get the correct locale context on older Android
     */
    override fun attachBaseContext(base: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(base)
        val languageCode = prefs.getString("language", "en") ?: "en"

        val wrappedContext = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            wrapLegacyContext(base, languageCode)
        } else {
            base
        }
        super.attachBaseContext(wrappedContext)
    }

    private fun wrapLegacyContext(base: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
