package com.juliablack.blocker5g

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import java.util.Locale


object Preference {

    private const val nameSharedPreferences = "blocker5G_shared_pref"
    private const val canceledVersion = "canceled_version_code_pref"
    private const val languageTag = "language_tag_pref"

    fun saveCanceledUpdate(context: Context, versionCode: Int?) {
        getSharedPreference(context).edit().apply {
            versionCode?.let {
                putInt(canceledVersion, it)
            }
            apply()
        }
    }

    fun getCanceledVersionCode(context: Context) =
        getSharedPreference(context).getInt(canceledVersion, 0)

    fun saveLanguageTag(context: Context, language: String?) {
        getSharedPreference(context).edit().apply {
            if (language == null) {
                remove(languageTag)
            } else {
                putString(languageTag, language)
            }
            apply()
        }
    }

    fun getLanguageTag(context: Context): String? =
        getSharedPreference(context).getString(languageTag, null)

    fun applyLanguage(context: Context): Context {
        val language = getLanguageTag(context) ?: return context
        val locale = Locale(language)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun getSharedPreference(context: Context) =
        context.getSharedPreferences(nameSharedPreferences, MODE_PRIVATE)
}
