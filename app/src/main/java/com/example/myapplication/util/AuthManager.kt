package com.example.myapplication.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple authentication manager for development/testing when Firebase is not configured
 */
object AuthManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_EMAIL = "user_email"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setLoggedIn(context: Context, email: String) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_EMAIL, null)
    }

    fun logout(context: Context) {
        getPrefs(context).edit().apply {
            clear()
            apply()
        }
    }
}