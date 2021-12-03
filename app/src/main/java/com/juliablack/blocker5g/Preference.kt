package com.juliablack.blocker5g

import android.content.Context
import android.content.Context.MODE_PRIVATE


object Preference {

    private const val nameSharedPreferences = "blocker5G_shared_pref"
    private const val canceledVersion = "canceled_version_code_pref"

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

    private fun getSharedPreference(context: Context) =
        context.getSharedPreferences(nameSharedPreferences, MODE_PRIVATE)
}