package com.example.myapplication.Ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.ChangePasswordActivity
import com.example.myapplication.EncryptionStatsActivity
import com.example.myapplication.R
import com.example.myapplication.Ui.AboutUsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Initialize summaries
        updateThemeSummary()
        updateLanguageSummary()

        // Change Password
        findPreference<Preference>("pref_change_password")?.setOnPreferenceClickListener {
            startActivityWithAnimation(ChangePasswordActivity::class.java)
            true
        }

        // Encryption Statistics
        findPreference<Preference>("pref_stats")?.setOnPreferenceClickListener {
            startActivityWithAnimation(EncryptionStatsActivity::class.java)
            true
        }

        // About
        findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
            startActivityWithAnimation(AboutUsActivity::class.java)
            true
        }

        // Theme selection dialog
        findPreference<Preference>("theme")?.setOnPreferenceClickListener {
            val currentTheme = preferenceScreen.sharedPreferences?.getString("theme", "light") ?: "light"
            showThemeDialog(currentTheme)
            true
        }

        // Language selection dialog
        findPreference<Preference>("language")?.setOnPreferenceClickListener {
            val currentLanguage = preferenceScreen.sharedPreferences?.getString("language", "en") ?: "en"
            showLanguageDialog(currentLanguage)
            true
        }
    }

    private fun startActivityWithAnimation(target: Class<*>) {
        val intent = Intent(requireContext(), target)
        startActivity(intent)
        requireActivity().overridePendingTransition(
            R.anim.premium_slide_in,
            R.anim.premium_slide_out
        )
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "pref_use_biometric" -> {
                // Optional: Handle biometric toggle changes instantly if needed
            }
        }
    }

    private fun applyLanguage(languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        } else {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val res = resources
            val config = res.configuration
            config.setLocale(locale)
            res.updateConfiguration(config, res.displayMetrics)
        }
    }

    // ----------------------------
    // Update summaries
    // ----------------------------
    private fun updateThemeSummary() {
        val theme = preferenceScreen.sharedPreferences?.getString("theme", "light") ?: "light"
        val display = when (theme) {
            "light" -> "Light"
            "dark" -> "Dark"
            "system" -> "System Default"
            else -> "Light"
        }
        preferenceScreen.findPreference<Preference>("theme")?.summary = display
    }


    private fun updateLanguageSummary() {
        val languageCode = preferenceScreen.sharedPreferences?.getString("language", "en") ?: "en"
        val languages = mapOf("en" to "English", "hi" to "Hindi", "fr" to "French")
        val display = languages[languageCode] ?: "English"
        preferenceScreen.findPreference<Preference>("language")?.summary = display
    }

    // ----------------------------
    // Theme Dialog
    // ----------------------------
    private fun showThemeDialog(currentTheme: String) {
        val themes = arrayOf("Light", "Dark", "System Default")
        val codes = arrayOf("light", "dark", "system")

        var selectedIndex = codes.indexOf(currentTheme)
        if (selectedIndex == -1) selectedIndex = 0

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Theme")
            .setSingleChoiceItems(themes, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Apply") { _, _ ->
                val selectedTheme = codes[selectedIndex]
                val prefs = preferenceScreen.sharedPreferences
                prefs?.edit()?.putString("theme", selectedTheme)?.apply()

                when (selectedTheme) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

                updateThemeSummary()
                activity?.recreate()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK)
            )
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.DKGRAY)
            )
        }

        dialog.show()
    }

    // ----------------------------
    // Language Dialog
    // ----------------------------
    private fun showLanguageDialog(currentLanguage: String) {
        val languages = arrayOf("English", "Hindi", "French")
        val codes = arrayOf("en", "hi", "fr")
        var selectedIndex = codes.indexOf(currentLanguage)
        if (selectedIndex == -1) selectedIndex = 0

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Language")
            .setSingleChoiceItems(languages, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Apply") { _, _ ->
                val selectedCode = codes[selectedIndex]
                val prefs = preferenceScreen.sharedPreferences
                prefs?.edit()?.putString("language", selectedCode)?.apply()

                applyLanguage(selectedCode)
                updateLanguageSummary()
                activity?.recreate()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK)
            )
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.DKGRAY)
            )
        }

        dialog.show()
    }
}
